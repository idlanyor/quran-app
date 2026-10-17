package com.example.data

import com.example.data.local.AyatEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.KhatamLogEntity
import com.example.data.local.PaginatedSearchResult
import com.example.data.local.QuranDao
import com.example.data.local.QuranSeedData
import com.example.data.local.SearchAyatItem
import com.example.data.local.SurahEntity
import com.example.data.local.TafsirEntity
import com.example.data.remote.QuranApiService
import com.example.data.remote.QuranJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuranRepository(
    private val dao: QuranDao,
    private val api: QuranApiService
) {

    val allSurahsFlow: Flow<List<SurahEntity>> = dao.getAllSurahsFlow()
    val allBookmarksFlow: Flow<List<BookmarkEntity>> = dao.getAllBookmarksFlow()
    val recentKhatamLogsFlow: Flow<List<KhatamLogEntity>> = dao.getRecentKhatamLogsFlow()

    fun getAyatsBySurahFlow(surahNomor: Int): Flow<List<AyatEntity>> =
        dao.getAyatsBySurahFlow(surahNomor)

    suspend fun ensureInitialDataAndSync(): Result<List<SurahEntity>> = withContext(Dispatchers.IO) {
        val currentLocal = dao.getAllSurahsOnce()
        if (currentLocal.isEmpty()) {
            dao.insertSurahs(QuranSeedData.getAll114Surahs())
            dao.insertAyats(QuranSeedData.getInitialCachedAyats())
            dao.insertTafsirs(QuranSeedData.getInitialCachedTafsirs())
        }
        syncAllSuratFromApi()
    }

    /**
     * GET /api/v1/quran/surat
     */
    suspend fun syncAllSuratFromApi(): Result<List<SurahEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllSurat()
            if (response.isSuccessful) {
                val raw = response.body()?.string().orEmpty()
                val parsed = QuranJsonParser.parseSurahList(raw)
                if (parsed.isNotEmpty()) {
                    val existingMap = dao.getAllSurahsOnce().associateBy { it.nomor }
                    val merged = parsed.map { remote ->
                        val local = existingMap[remote.nomor]
                        remote.copy(
                            isCachedOffline = local?.isCachedOffline ?: false
                        )
                    }
                    dao.insertSurahs(merged)
                    return@withContext Result.success(merged)
                }
            }
            val fallback = dao.getAllSurahsOnce()
            if (fallback.isNotEmpty()) {
                Result.success(fallback)
            } else {
                Result.failure(Exception("Gagal memuat daftar surah (${response.code()})"))
            }
        } catch (e: Exception) {
            val fallback = dao.getAllSurahsOnce()
            if (fallback.isNotEmpty()) {
                Result.success(fallback)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * GET /api/v1/quran/surat/{nomor}
     */
    suspend fun fetchAndCacheSurahDetail(
        nomor: Int,
        forceRefresh: Boolean = false
    ): Result<Pair<SurahEntity?, List<AyatEntity>>> = withContext(Dispatchers.IO) {
        val cachedSurah = dao.getSurahByNomor(nomor)
        val cachedAyats = dao.getAyatsBySurahOnce(nomor)

        if (!forceRefresh && cachedAyats.isNotEmpty() && cachedSurah != null && cachedAyats.size >= cachedSurah.jumlahAyat) {
            return@withContext Result.success(cachedSurah to cachedAyats)
        }

        try {
            val response = api.getSuratDetail(nomor)
            if (response.isSuccessful) {
                val raw = response.body()?.string().orEmpty()
                val (parsedSurah, parsedAyats) = QuranJsonParser.parseSurahDetail(raw, nomor)
                if (parsedAyats.isNotEmpty()) {
                    dao.insertAyats(parsedAyats)
                    val updatedSurah = (parsedSurah ?: cachedSurah)?.copy(
                        isCachedOffline = true,
                        jumlahAyat = if ((parsedSurah?.jumlahAyat ?: 0) > 0) parsedSurah!!.jumlahAyat else parsedAyats.size
                    )
                    if (updatedSurah != null) {
                        dao.insertSurah(updatedSurah)
                    } else {
                        dao.updateSurahOfflineStatus(nomor, true)
                    }
                    return@withContext Result.success(updatedSurah to parsedAyats)
                }
            }
            if (cachedAyats.isNotEmpty()) {
                Result.success(cachedSurah to cachedAyats)
            } else {
                Result.failure(Exception("Gagal memuat ayat surah ke-$nomor (${response.code()})"))
            }
        } catch (e: Exception) {
            if (cachedAyats.isNotEmpty()) {
                Result.success(cachedSurah to cachedAyats)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * GET /api/v1/quran/tafsir/{nomor}
     */
    suspend fun fetchAndCacheSurahTafsir(
        surahNomor: Int,
        nomorAyat: Int? = null,
        forceRefresh: Boolean = false
    ): Result<List<TafsirEntity>> = withContext(Dispatchers.IO) {
        val cachedTafsirs = dao.getTafsirsBySurahOnce(surahNomor)
        if (!forceRefresh && cachedTafsirs.isNotEmpty()) {
            if (nomorAyat == null || cachedTafsirs.any { it.nomorAyat == nomorAyat }) {
                return@withContext Result.success(cachedTafsirs)
            }
        }

        try {
            val response = api.getSuratTafsir(surahNomor)
            if (response.isSuccessful) {
                val raw = response.body()?.string().orEmpty()
                val parsed = QuranJsonParser.parseTafsirList(raw, surahNomor)
                if (parsed.isNotEmpty()) {
                    dao.insertTafsirs(parsed)
                    return@withContext Result.success(parsed)
                }
            }
            if (cachedTafsirs.isNotEmpty()) {
                Result.success(cachedTafsirs)
            } else {
                Result.failure(Exception("Gagal memuat tafsir surah ke-$surahNomor (${response.code()})"))
            }
        } catch (e: Exception) {
            if (cachedTafsirs.isNotEmpty()) {
                Result.success(cachedTafsirs)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * GET /api/v1/quran/search?q={query}&page={page}&size={size}
     */
    suspend fun searchAyat(
        query: String,
        page: Int = 1,
        size: Int = 10
    ): Result<PaginatedSearchResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return@withContext Result.success(PaginatedSearchResult(emptyList(), 1, 1, 0, false))
        }

        val localSurahs = dao.getAllSurahsOnce()
        val surahMap = if (localSurahs.isNotEmpty()) {
            localSurahs.associateBy { it.nomor }
        } else {
            QuranSeedData.getAll114Surahs().associateBy { it.nomor }
        }

        try {
            val response = api.searchAyat(
                query = cleanQuery,
                page = page.coerceAtLeast(1),
                size = size.coerceIn(1, 50)
            )
            if (response.isSuccessful) {
                val raw = response.body()?.string().orEmpty()
                val parsed = QuranJsonParser.parseSearchResults(raw, page, size)
                val enrichedItems = parsed.items.map { item ->
                    val s = surahMap[item.surahNomor] ?: QuranSeedData.getAll114Surahs().find { it.nomor == item.surahNomor }
                    item.copy(
                        surahNamaLatin = if (item.surahNamaLatin.isBlank() || item.surahNamaLatin.startsWith("Surah ke-")) {
                            s?.namaLatin ?: "Surah ${item.surahNomor}"
                        } else item.surahNamaLatin,
                        surahNamaArab = item.surahNamaArab.ifBlank { s?.namaArab.orEmpty() }
                    )
                }
                if (enrichedItems.isNotEmpty() || page > 1) {
                    return@withContext Result.success(parsed.copy(items = enrichedItems))
                }
            }

            // Fallback to local cached verses if offline or empty API response on page 1
            val offset = (page - 1) * size
            val localMatches = dao.searchCachedAyats(cleanQuery, size, offset)
            val mappedLocal = localMatches.map { ayat ->
                val s = surahMap[ayat.surahNomor]
                SearchAyatItem(
                    surahNomor = ayat.surahNomor,
                    surahNamaLatin = s?.namaLatin ?: "Surah ${ayat.surahNomor}",
                    surahNamaArab = s?.namaArab ?: "",
                    nomorAyat = ayat.nomorAyat,
                    teksArab = ayat.teksArab,
                    teksLatin = ayat.teksLatin,
                    teksIndonesia = ayat.teksIndonesia,
                    audioUrl = ayat.audioUrl
                )
            }
            Result.success(
                PaginatedSearchResult(
                    items = mappedLocal,
                    currentPage = page,
                    totalPages = if (mappedLocal.size >= size) page + 1 else page,
                    totalItems = mappedLocal.size,
                    hasMore = mappedLocal.size >= size
                )
            )
        } catch (e: Exception) {
            val offset = (page - 1) * size
            val localMatches = dao.searchCachedAyats(cleanQuery, size, offset)
            if (localMatches.isNotEmpty()) {
                val mappedLocal = localMatches.map { ayat ->
                    val s = surahMap[ayat.surahNomor]
                    SearchAyatItem(
                        surahNomor = ayat.surahNomor,
                        surahNamaLatin = s?.namaLatin ?: "Surah ${ayat.surahNomor}",
                        surahNamaArab = s?.namaArab ?: "",
                        nomorAyat = ayat.nomorAyat,
                        teksArab = ayat.teksArab,
                        teksLatin = ayat.teksLatin,
                        teksIndonesia = ayat.teksIndonesia,
                        audioUrl = ayat.audioUrl
                    )
                }
                Result.success(
                    PaginatedSearchResult(
                        items = mappedLocal,
                        currentPage = page,
                        totalPages = page,
                        totalItems = mappedLocal.size,
                        hasMore = false
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun toggleBookmark(
        surah: SurahEntity,
        ayat: AyatEntity,
        isCurrentlyBookmarked: Boolean,
        catatan: String = ""
    ) = withContext(Dispatchers.IO) {
        if (isCurrentlyBookmarked) {
            dao.deleteBookmark(surah.nomor, ayat.nomorAyat)
        } else {
            dao.insertBookmark(
                BookmarkEntity(
                    surahNomor = surah.nomor,
                    surahNamaLatin = surah.namaLatin,
                    surahNamaArab = surah.namaArab,
                    nomorAyat = ayat.nomorAyat,
                    teksArab = ayat.teksArab,
                    teksIndonesia = ayat.teksIndonesia,
                    catatan = catatan
                )
            )
        }
    }

    suspend fun removeBookmark(surahNomor: Int, nomorAyat: Int) = withContext(Dispatchers.IO) {
        dao.deleteBookmark(surahNomor, nomorAyat)
    }

    suspend fun recordDailyKhatamProgress(addedAyats: Int, addedSurahs: Int = 0) = withContext(Dispatchers.IO) {
        if (addedAyats <= 0 && addedSurahs <= 0) return@withContext
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val existing = dao.getKhatamLogForDate(todayKey)
        val newTotalAyat = (existing?.ayatCountRead ?: 0) + addedAyats.coerceAtLeast(0)
        val newTotalSurah = (existing?.surahCountRead ?: 0) + addedSurahs.coerceAtLeast(0)
        dao.insertKhatamLog(
            KhatamLogEntity(
                dateKey = todayKey,
                ayatCountRead = newTotalAyat,
                surahCountRead = newTotalSurah,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }
}
