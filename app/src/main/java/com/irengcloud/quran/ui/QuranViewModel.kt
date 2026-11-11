package com.irengcloud.quran.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.irengcloud.quran.data.QuranRepository
import com.irengcloud.quran.data.MurottalStorage
import com.irengcloud.quran.data.SavedMurottal
import com.irengcloud.quran.data.alafasyVerseUrl
import com.irengcloud.quran.data.local.AyatEntity
import com.irengcloud.quran.data.local.BookmarkEntity
import com.irengcloud.quran.data.local.KhatamLogEntity
import com.irengcloud.quran.data.local.KhatamTargetMode
import com.irengcloud.quran.data.local.LastReadState
import com.irengcloud.quran.data.local.QuranDatabase
import com.irengcloud.quran.data.local.QuranLatinSource
import com.irengcloud.quran.data.local.ReadingProgressRepository
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.data.local.SearchAyatItem
import com.irengcloud.quran.data.local.SurahEntity
import com.irengcloud.quran.data.local.ThemeMode
import com.irengcloud.quran.data.local.UserPreferencesRepository
import com.irengcloud.quran.data.remote.QuranApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class SurahFilterCategory(val label: String) {
    ALL("Semua"),
    MAKKIYAH("Makkiyah"),
    MADANIYAH("Madaniyah"),
    OFFLINE("Tersimpan Offline")
}

data class SurahDetailUiState(
    val isLoading: Boolean = false,
    val surah: SurahEntity? = null,
    val ayats: List<AyatEntity> = emptyList(),
    val targetScrollAyat: Int? = null,
    val errorMessage: String? = null,
    val isOfflineSaving: Boolean = false
)

data class TafsirSheetUiState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val surahNomor: Int = 1,
    val surahNamaLatin: String = "",
    val jumlahAyat: Int = 1,
    val nomorAyat: Int = 1,
    val teksArab: String = "",
    val teksLatin: String = "",
    val teksIndonesia: String = "",
    val teksTafsir: String = "",
    val errorMessage: String? = null
)

data class SearchUiState(
    val query: String = "rahmat",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<SearchAyatItem> = emptyList(),
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val totalPages: Int = 1,
    val totalItems: Int = 0,
    val hasMore: Boolean = false,
    val errorMessage: String? = null,
    val hasSearchedOnce: Boolean = false,
    val submittedQuery: String = ""
)

data class MurottalDownloadsState(
    val saved: Map<Int, SavedMurottal> = emptyMap(),
    val downloadingSurah: Int? = null,
    val savedVerses: Int = 0,
    val totalVerses: Int = 0,
    val removingSurah: Int? = null
)

class QuranViewModel(
    private val repository: QuranRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val murottalStorage: MurottalStorage
) : ViewModel() {

    private val readingProgress = ReadingProgressRepository(prefsRepository.store)

    val audioController = MurottalAudioController(murottalStorage::localAudioPath)
    val audioState: StateFlow<AudioPlaybackState> = audioController.state
    private val _murottalDownloads = MutableStateFlow(MurottalDownloadsState())
    val murottalDownloads: StateFlow<MurottalDownloadsState> = _murottalDownloads.asStateFlow()
    private var murottalFileJob: Job? = null

    private val _surahSearchQuery = MutableStateFlow("")
    val surahSearchQuery: StateFlow<String> = _surahSearchQuery.asStateFlow()

    private val _selectedSurahFilter = MutableStateFlow(SurahFilterCategory.ALL)
    val selectedSurahFilter: StateFlow<SurahFilterCategory> = _selectedSurahFilter.asStateFlow()

    private val _isSyncingSurahList = MutableStateFlow(false)
    val isSyncingSurahList: StateFlow<Boolean> = _isSyncingSurahList.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val allSurahs: StateFlow<List<SurahEntity>> = repository.allSurahsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSurahs: StateFlow<List<SurahEntity>> = combine(
        allSurahs,
        _surahSearchQuery,
        _selectedSurahFilter
    ) { surahs, query, filter ->
        val q = query.trim().lowercase()
        surahs.filter { surah ->
            val matchesFilter = when (filter) {
                SurahFilterCategory.ALL -> true
                SurahFilterCategory.MAKKIYAH -> surah.tempatTurun.equals("Makkiyah", ignoreCase = true)
                SurahFilterCategory.MADANIYAH -> surah.tempatTurun.equals("Madaniyah", ignoreCase = true)
                SurahFilterCategory.OFFLINE -> surah.isCachedOffline
            }
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                surah.namaLatin.lowercase().contains(q) ||
                    surah.arti.lowercase().contains(q) ||
                    surah.nomor.toString() == q ||
                    surah.namaArab.contains(q)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val khatamLogs: StateFlow<List<KhatamLogEntity>> = readingProgress.logsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastRead: StateFlow<LastReadState> = prefsRepository.lastReadFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LastReadState())

    val readerSettings: StateFlow<ReaderSettingsState> = prefsRepository.readerSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderSettingsState())

    private val _surahDetailState = MutableStateFlow(SurahDetailUiState())
    val surahDetailState: StateFlow<SurahDetailUiState> = _surahDetailState.asStateFlow()

    private val _tafsirSheetState = MutableStateFlow(TafsirSheetUiState())
    val tafsirSheetState: StateFlow<TafsirSheetUiState> = _tafsirSheetState.asStateFlow()
    private var tafsirJob: Job? = null

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = murottalStorage.savedSurahs()
            _murottalDownloads.update { it.copy(saved = saved) }
        }
        viewModelScope.launch {
            readingProgress.importLegacyHistory(repository.recentKhatamLogsFlow.first())
        }
        viewModelScope.launch {
            readerSettings.collect { settings ->
                audioController.setAutoNext(settings.autoNextAudio)
            }
        }
        refreshSurahList()
    }

    fun refreshSurahList() {
        viewModelScope.launch {
            _isSyncingSurahList.value = true
            repository.ensureInitialDataAndSync()
            _isSyncingSurahList.value = false
        }
    }

    fun updateSurahSearchQuery(query: String) {
        _surahSearchQuery.value = query
    }

    fun selectSurahFilter(filter: SurahFilterCategory) {
        _selectedSurahFilter.value = filter
    }

    private var openSurahJob: Job? = null

    fun openSurahDetail(surahNomor: Int, targetAyat: Int? = null, forceRefresh: Boolean = false) {
        openSurahJob?.cancel()
        val initialSurah = allSurahs.value.find { it.nomor == surahNomor }
        _surahDetailState.value = SurahDetailUiState(
            isLoading = true, surah = initialSurah, targetScrollAyat = targetAyat
        )
        openSurahJob = viewModelScope.launch {
            // Ignore an older request if another surah has been opened.
            val cachedResult = repository.fetchAndCacheSurahDetail(surahNomor, forceRefresh = forceRefresh)
            ensureActive()
            val (surahEntity, ayats) = cachedResult.getOrNull() ?: (initialSurah to emptyList())

            _surahDetailState.value = SurahDetailUiState(
                isLoading = false,
                surah = surahEntity ?: initialSurah,
                ayats = ayats,
                targetScrollAyat = targetAyat,
                errorMessage = if (cachedResult.isFailure && ayats.isEmpty()) cachedResult.exceptionOrNull()?.message else null
            )
        }
    }

    fun clearTargetScrollAyat() {
        _surahDetailState.update { it.copy(targetScrollAyat = null) }
    }

    fun saveSurahOfflineWithTafsir(surahNomor: Int) {
        viewModelScope.launch {
            _surahDetailState.update { it.copy(isOfflineSaving = true) }
            val detailRes = repository.fetchAndCacheSurahDetail(surahNomor, forceRefresh = true)
            repository.fetchAndCacheSurahTafsir(surahNomor, forceRefresh = true)
            _surahDetailState.update { state ->
                val (s, a) = detailRes.getOrNull() ?: (state.surah to state.ayats)
                state.copy(
                    isOfflineSaving = false,
                    surah = s?.copy(isCachedOffline = true) ?: state.surah?.copy(isCachedOffline = true),
                    ayats = a.ifEmpty { state.ayats }
                )
            }
            showSnackbar("Surah & Tafsir berhasil disimpan untuk dibaca offline")
        }
    }

    fun openTafsirForAyat(surah: SurahEntity, ayat: AyatEntity, forceRefresh: Boolean = false) {
        tafsirJob?.cancel()
        _tafsirSheetState.value = TafsirSheetUiState(
            isVisible = true,
            isLoading = true,
            surahNomor = surah.nomor,
            surahNamaLatin = surah.namaLatin,
            jumlahAyat = surah.jumlahAyat.coerceAtLeast(1),
            nomorAyat = ayat.nomorAyat,
            teksArab = ayat.teksArab,
            teksLatin = ayat.teksLatin,
            teksIndonesia = ayat.teksIndonesia
        )

        tafsirJob = viewModelScope.launch {
            val result = repository.fetchAndCacheSurahTafsir(
                surahNomor = surah.nomor,
                nomorAyat = ayat.nomorAyat,
                forceRefresh = forceRefresh
            )
            ensureActive()
            result.fold(
                onSuccess = { tafsirList ->
                    val matched = tafsirList.find { it.nomorAyat == ayat.nomorAyat }
                    val tafsirText = matched?.teksTafsir?.takeIf { it.isNotBlank() }
                        ?: "Tafsir untuk ayat ini belum tersedia."
                    _tafsirSheetState.update {
                        it.copy(
                            isLoading = false,
                            teksTafsir = tafsirText,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _tafsirSheetState.update {
                        it.copy(
                            isLoading = false,
                            teksTafsir = "Tafsir belum dapat dimuat dari jaringan. Pastikan koneksi internet aktif untuk mengunduh tafsir pertama kali.",
                            errorMessage = err.message
                        )
                    }
                }
            )
        }
    }

    fun navigateTafsirAyat(targetAyatNomor: Int, forceRefresh: Boolean = false) {
        val current = _tafsirSheetState.value
        if (!current.isVisible) return
        val maxAyat = current.jumlahAyat.coerceAtLeast(1)
        val clamped = targetAyatNomor.coerceIn(1, maxAyat)
        if (clamped == current.nomorAyat && !forceRefresh && !current.isLoading) return

        tafsirJob?.cancel()
        tafsirJob = viewModelScope.launch {
            val surah = _surahDetailState.value.surah?.takeIf { it.nomor == current.surahNomor }
                ?: allSurahs.value.find { it.nomor == current.surahNomor }
            val loadedAyats = _surahDetailState.value.ayats.takeIf {
                _surahDetailState.value.surah?.nomor == current.surahNomor && it.isNotEmpty()
            } ?: repository.fetchAndCacheSurahDetail(current.surahNomor).getOrNull()?.second.orEmpty()

            val targetAyat = loadedAyats.find { it.nomorAyat == clamped }
            if (surah != null && targetAyat != null) {
                openTafsirForAyat(surah, targetAyat, forceRefresh = forceRefresh)
            }
        }
    }

    fun toggleTafsirBookmark() {
        val current = _tafsirSheetState.value
        if (!current.isVisible) return
        val surah = _surahDetailState.value.surah?.takeIf { it.nomor == current.surahNomor }
            ?: allSurahs.value.find { it.nomor == current.surahNomor }
            ?: SurahEntity(
                nomor = current.surahNomor,
                namaArab = "",
                namaLatin = current.surahNamaLatin,
                arti = "",
                jumlahAyat = current.jumlahAyat,
                tempatTurun = "Makkiyah"
            )
        val ayat = _surahDetailState.value.ayats.find {
            it.surahNomor == current.surahNomor && it.nomorAyat == current.nomorAyat
        } ?: AyatEntity(
            surahNomor = current.surahNomor,
            nomorAyat = current.nomorAyat,
            teksArab = current.teksArab,
            teksLatin = current.teksLatin,
            teksIndonesia = current.teksIndonesia
        )
        toggleBookmark(surah, ayat)
    }

    fun playTafsirAyatAudio() {
        val current = _tafsirSheetState.value
        if (!current.isVisible) return
        val surah = _surahDetailState.value.surah?.takeIf { it.nomor == current.surahNomor }
            ?: allSurahs.value.find { it.nomor == current.surahNomor }
        val ayats = _surahDetailState.value.ayats.takeIf {
            _surahDetailState.value.surah?.nomor == current.surahNomor && it.isNotEmpty()
        }
        if (surah != null && ayats != null) {
            playSingleVerseAudio(surah, ayats, current.nomorAyat)
        } else {
            playSearchItemAudio(
                SearchAyatItem(
                    surahNomor = current.surahNomor,
                    surahNamaLatin = current.surahNamaLatin,
                    surahNamaArab = surah?.namaArab.orEmpty(),
                    nomorAyat = current.nomorAyat,
                    teksArab = current.teksArab,
                    teksLatin = current.teksLatin,
                    teksIndonesia = current.teksIndonesia,
                    audioUrl = alafasyVerseUrl(current.surahNomor, current.nomorAyat)
                )
            )
        }
    }

    fun closeTafsirSheet() {
        tafsirJob?.cancel()
        _tafsirSheetState.update { it.copy(isVisible = false) }
    }

    fun updateSearchQueryInput(newQuery: String) {
        _searchState.update { it.copy(query = newQuery) }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchState.update {
            SearchUiState(query = "", pageSize = it.pageSize, hasSearchedOnce = true)
        }
    }

    fun updateSearchPageSize(newSize: Int) {
        val validSize = newSize.coerceIn(5, 50)
        _searchState.update { it.copy(pageSize = validSize) }
        val currentQuery = _searchState.value.submittedQuery
        if (currentQuery.isNotEmpty()) {
            executeSearch(query = currentQuery, page = 1, size = validSize)
        }
    }

    fun executeSearch(
        query: String = _searchState.value.query,
        page: Int = 1,
        size: Int = _searchState.value.pageSize
    ) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            showSnackbar("Masukkan kata kunci pencarian terlebih dahulu")
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val isFirstPage = page <= 1
            _searchState.update {
                if (isFirstPage) {
                    SearchUiState(
                        query = cleanQuery,
                        submittedQuery = cleanQuery,
                        pageSize = size,
                        isLoading = true,
                        hasSearchedOnce = true
                    )
                } else {
                    it.copy(isLoadingMore = true, errorMessage = null)
                }
            }

            val result = repository.searchAyat(query = cleanQuery, page = page, size = size)
            ensureActive()
            result.fold(
                onSuccess = { paginated ->
                    _searchState.update { prev ->
                        val combinedItems = if (isFirstPage) {
                            paginated.items
                        } else {
                            (prev.items + paginated.items).distinctBy { "${it.surahNomor}:${it.nomorAyat}" }
                        }
                        prev.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            items = combinedItems,
                            currentPage = paginated.currentPage,
                            totalPages = paginated.totalPages,
                            totalItems = maxOf(paginated.totalItems, combinedItems.size),
                            hasMore = paginated.hasMore,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _searchState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = err.message ?: "Gagal mencari ayat"
                        )
                    }
                }
            )
        }
    }

    fun loadNextSearchPage() {
        val current = _searchState.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        executeSearch(
            query = current.submittedQuery,
            page = current.currentPage + 1,
            size = current.pageSize
        )
    }

    fun toggleBookmark(surah: SurahEntity, ayat: AyatEntity) {
        viewModelScope.launch {
            val isBookmarked = bookmarks.value.any {
                it.surahNomor == surah.nomor && it.nomorAyat == ayat.nomorAyat
            }
            repository.toggleBookmark(surah, ayat, isBookmarked)
            val msg = if (isBookmarked) {
                "Dihapus dari Bookmark (QS. ${surah.namaLatin}: ${ayat.nomorAyat})"
            } else {
                "Disimpan ke Bookmark (QS. ${surah.namaLatin}: ${ayat.nomorAyat})"
            }
            showSnackbar(msg)
        }
    }

    fun removeBookmark(surahNomor: Int, nomorAyat: Int) {
        viewModelScope.launch {
            repository.removeBookmark(surahNomor, nomorAyat)
            showSnackbar("Bookmark berhasil dihapus")
        }
    }

    fun saveReadingPosition(surah: SurahEntity, ayatNomor: Int) {
        viewModelScope.launch {
            readingProgress.savePosition(surah, ayatNomor)
        }
    }

    suspend fun awaitLastRead(): LastReadState = prefsRepository.lastReadFlow.first()

    fun markLastReadAndLogKhatam(surah: SurahEntity, ayatNomor: Int) {
        viewModelScope.launch {
            val delta = readingProgress.markReadThrough(surah, ayatNomor)
            showSnackbar(if (delta.ayats == 0) "Ayat ini sudah tercatat dalam khatam"
                else "QS. ${surah.namaLatin}:$ayatNomor selesai dibaca (+${delta.ayats} ayat, +${delta.surahs} surah)")
        }
    }

    fun logCustomKhatamAyats(addedAyats: Int) {
        viewModelScope.launch {
            val delta = readingProgress.recordNext(ayatCount = addedAyats.coerceIn(1, 600))
            showSnackbar(if (delta.ayats == 0) "Khatam sudah selesai. Mulai putaran baru melalui Reset."
                else "+${delta.ayats} ayat dan +${delta.surahs} surah tercatat berurutan")
        }
    }

    fun logCustomKhatamSurahs(addedSurahs: Int) {
        viewModelScope.launch {
            val delta = readingProgress.recordNext(surahCount = addedSurahs.coerceIn(1, 30))
            showSnackbar(if (delta.ayats == 0) "Khatam sudah selesai. Mulai putaran baru melalui Reset."
                else "+${delta.surahs} surah (${delta.ayats} ayat) tercatat berurutan")
        }
    }

    fun resetKhatamProgress() {
        viewModelScope.launch {
            readingProgress.reset()
            showSnackbar("Putaran khatam baru dimulai. Posisi terakhir dibaca tetap tersimpan.")
        }
    }

    fun updateArabicFontSize(sizeSp: Int) {
        viewModelScope.launch { prefsRepository.updateArabicFontSize(sizeSp) }
    }

    fun updateTranslationFontSize(sizeSp: Int) {
        viewModelScope.launch { prefsRepository.updateTranslationFontSize(sizeSp) }
    }

    fun updateShowLatin(show: Boolean) {
        viewModelScope.launch { prefsRepository.updateShowLatin(show) }
    }

    fun updateShowTranslation(show: Boolean) {
        viewModelScope.launch { prefsRepository.updateShowTranslation(show) }
    }

    fun updateAutoNextAudio(autoNext: Boolean) {
        viewModelScope.launch { prefsRepository.updateAutoNextAudio(autoNext) }
    }

    fun updateAutoScrollSpeed(speedLevel: Int) {
        viewModelScope.launch { prefsRepository.updateAutoScrollSpeed(speedLevel) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepository.updateThemeMode(mode) }
    }

    fun updateKhatamTargetMode(mode: KhatamTargetMode) {
        viewModelScope.launch { prefsRepository.updateKhatamTargetMode(mode) }
    }

    fun updateDailyKhatamTarget(targetAyat: Int) {
        viewModelScope.launch { prefsRepository.updateDailyKhatamTarget(targetAyat) }
    }

    fun updateDailyKhatamTargetSurah(targetSurah: Int) {
        viewModelScope.launch { prefsRepository.updateDailyKhatamTargetSurah(targetSurah) }
    }

    fun playSingleVerseAudio(surah: SurahEntity, ayats: List<AyatEntity>, nomorAyat: Int) {
        audioController.playVerseFromSurah(
            surahNomor = surah.nomor,
            surahNamaLatin = surah.namaLatin,
            ayats = ayats,
            startAyatNomor = nomorAyat
        )
        // Background on-demand refresh from irengcloud if needed
        viewModelScope.launch {
            repository.syncSurahAudioOnDemand(surah.nomor)
        }
    }

    fun playFullSurahAudio(surah: SurahEntity, ayats: List<AyatEntity>) {
        audioController.playFullSurah(
            surahNomor = surah.nomor,
            surahNamaLatin = surah.namaLatin,
            ayats = ayats
        )
        // Background on-demand refresh from irengcloud if needed
        viewModelScope.launch {
            repository.syncSurahAudioOnDemand(surah.nomor)
        }
    }

    fun playSearchItemAudio(item: SearchAyatItem) {
        val tempAyat = AyatEntity(
            surahNomor = item.surahNomor,
            nomorAyat = item.nomorAyat,
            teksArab = item.teksArab,
            teksLatin = item.teksLatin,
            teksIndonesia = item.teksIndonesia,
            audioUrl = item.audioUrl
        )
        audioController.playVerseFromSurah(
            surahNomor = item.surahNomor,
            surahNamaLatin = item.surahNamaLatin,
            ayats = listOf(tempAyat),
            startAyatNomor = item.nomorAyat
        )
    }

    fun playMurottal(surah: SurahEntity, ayats: List<AyatEntity>, startVerse: Int) {
        val queue = ayats.map { ayat ->
            ayat.copy(audioUrl = alafasyVerseUrl(surah.nomor, ayat.nomorAyat))
        }
        audioController.playVerseFromSurah(surah.nomor, surah.namaLatin, queue, startVerse)
    }

    fun downloadMurottal(surah: SurahEntity) {
        if (murottalFileJob?.isCompleted == false || surah.jumlahAyat <= 0) return
        murottalFileJob = viewModelScope.launch {
            _murottalDownloads.update {
                it.copy(downloadingSurah = surah.nomor, savedVerses = 0, totalVerses = surah.jumlahAyat)
            }
            try {
                murottalStorage.saveSurah(surah.nomor, surah.jumlahAyat) { count ->
                    _murottalDownloads.update { it.copy(savedVerses = count) }
                }
                showSnackbar("Murottal ${surah.namaLatin} tersimpan")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showSnackbar("Unduhan belum selesai. Coba unduh lagi.")
            } finally {
                withContext(NonCancellable) {
                    val saved = murottalStorage.savedSurahs()
                    _murottalDownloads.update {
                        it.copy(saved = saved, downloadingSurah = null, savedVerses = 0, totalVerses = 0)
                    }
                }
            }
        }
    }

    fun cancelMurottalDownload() {
        if (_murottalDownloads.value.downloadingSurah != null) murottalFileJob?.cancel()
    }

    fun removeMurottal(surah: Int) {
        if (murottalFileJob?.isCompleted == false) return
        murottalFileJob = viewModelScope.launch {
            _murottalDownloads.update { it.copy(removingSurah = surah) }
            try {
                val removed = murottalStorage.removeSurah(surah)
                if (!removed) showSnackbar("Sebagian file audio tidak dapat dihapus")
                val playing = audioState.value
                if (playing.surahNomor == surah && playing.usesLocalAudio) audioController.stop()
            } finally {
                val saved = murottalStorage.savedSurahs()
                _murottalDownloads.update { it.copy(saved = saved, removingSurah = null) }
            }
        }
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioController.stop()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = QuranDatabase.getInstance(context)
                    val api = QuranApiService.create()
                    val latin = QuranLatinSource(context.assets)
                    val repo = QuranRepository(db.quranDao(), api, latin::forSurah)
                    val prefs = UserPreferencesRepository(context.applicationContext)
                    val murottal = MurottalStorage(File(context.filesDir, "murottal"))
                    return QuranViewModel(repo, prefs, murottal) as T
                }
            }
    }
}
