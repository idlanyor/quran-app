package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.QuranRepository
import com.example.data.local.AyatEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.KhatamLogEntity
import com.example.data.local.KhatamTargetMode
import com.example.data.local.LastReadState
import com.example.data.local.QuranDatabase
import com.example.data.local.ReaderSettingsState
import com.example.data.local.SearchAyatItem
import com.example.data.local.SurahEntity
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.QuranApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val nomorAyat: Int = 1,
    val teksArab: String = "",
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
    val hasSearchedOnce: Boolean = false
)

class QuranViewModel(
    private val repository: QuranRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    val audioController = MurottalAudioController()
    val audioState: StateFlow<AudioPlaybackState> = audioController.state

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

    val khatamLogs: StateFlow<List<KhatamLogEntity>> = repository.recentKhatamLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastRead: StateFlow<LastReadState> = prefsRepository.lastReadFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LastReadState())

    val readerSettings: StateFlow<ReaderSettingsState> = prefsRepository.readerSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderSettingsState())

    private val _surahDetailState = MutableStateFlow(SurahDetailUiState())
    val surahDetailState: StateFlow<SurahDetailUiState> = _surahDetailState.asStateFlow()

    private val _tafsirSheetState = MutableStateFlow(TafsirSheetUiState())
    val tafsirSheetState: StateFlow<TafsirSheetUiState> = _tafsirSheetState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    init {
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

    fun openSurahDetail(surahNomor: Int, targetAyat: Int? = null, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val initialSurah = allSurahs.value.find { it.nomor == surahNomor }
            _surahDetailState.update {
                it.copy(
                    isLoading = true,
                    surah = initialSurah ?: it.surah,
                    targetScrollAyat = targetAyat,
                    errorMessage = null
                )
            }
            val result = repository.fetchAndCacheSurahDetail(surahNomor, forceRefresh = forceRefresh)
            result.fold(
                onSuccess = { (surahEntity, ayats) ->
                    _surahDetailState.update {
                        it.copy(
                            isLoading = false,
                            surah = surahEntity ?: initialSurah,
                            ayats = ayats,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _surahDetailState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Gagal memuat isi surah"
                        )
                    }
                }
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

    fun openTafsirForAyat(surah: SurahEntity, ayat: AyatEntity) {
        viewModelScope.launch {
            _tafsirSheetState.value = TafsirSheetUiState(
                isVisible = true,
                isLoading = true,
                surahNomor = surah.nomor,
                surahNamaLatin = surah.namaLatin,
                nomorAyat = ayat.nomorAyat,
                teksArab = ayat.teksArab,
                teksIndonesia = ayat.teksIndonesia
            )

            val result = repository.fetchAndCacheSurahTafsir(
                surahNomor = surah.nomor,
                nomorAyat = ayat.nomorAyat
            )
            result.fold(
                onSuccess = { tafsirList ->
                    val matched = tafsirList.find { it.nomorAyat == ayat.nomorAyat }
                    val tafsirText = matched?.teksTafsir
                        ?: "Penjelasan Tafsir Kemenag RI untuk QS. ${surah.namaLatin} ayat ${ayat.nomorAyat}:\n\n\"${ayat.teksIndonesia}\"\n\nAyat ini menegaskan pesan keimanan dan tuntunan hidup mulia bagi orang-orang yang bertakwa."
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
                            teksTafsir = "Tafsir belum dapat dimuat dari jaringan. Pastikan koneksi internet aktif untuk mengunduh tafsir Kemenag RI pertama kali.",
                            errorMessage = err.message
                        )
                    }
                }
            )
        }
    }

    fun closeTafsirSheet() {
        _tafsirSheetState.update { it.copy(isVisible = false) }
    }

    fun updateSearchQueryInput(newQuery: String) {
        _searchState.update { it.copy(query = newQuery) }
    }

    fun updateSearchPageSize(newSize: Int) {
        val validSize = newSize.coerceIn(5, 50)
        _searchState.update { it.copy(pageSize = validSize) }
        val currentQuery = _searchState.value.query.trim()
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

        viewModelScope.launch {
            val isFirstPage = page <= 1
            _searchState.update {
                it.copy(
                    query = cleanQuery,
                    isLoading = isFirstPage,
                    isLoadingMore = !isFirstPage,
                    errorMessage = null,
                    hasSearchedOnce = true
                )
            }

            val result = repository.searchAyat(query = cleanQuery, page = page, size = size)
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
            query = current.query,
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

    fun markLastReadAndLogKhatam(surah: SurahEntity, ayatNomor: Int) {
        viewModelScope.launch {
            val prevLastRead = lastRead.value
            val deltaAyats = when {
                prevLastRead.surahNomor == surah.nomor && ayatNomor > prevLastRead.ayatNomor ->
                    (ayatNomor - prevLastRead.ayatNomor).coerceIn(1, 50)
                else -> 1
            }
            val completedSurah = if (ayatNomor >= surah.jumlahAyat) 1 else 0

            prefsRepository.saveLastRead(
                surahNomor = surah.nomor,
                surahNamaLatin = surah.namaLatin,
                surahNamaArab = surah.namaArab,
                surahArti = surah.arti,
                ayatNomor = ayatNomor,
                totalAyatSurah = surah.jumlahAyat
            )
            repository.recordDailyKhatamProgress(addedAyats = deltaAyats, addedSurahs = completedSurah)
            prefsRepository.incrementKhatamProgress(addedAyat = deltaAyats, addedSurah = completedSurah)
            val suffix = if (completedSurah > 0) " (+1 Surah & +$deltaAyats Ayat Khatam)" else " (+$deltaAyats Ayat Khatam)"
            showSnackbar("Ditandai terakhir dibaca: QS. ${surah.namaLatin} Ayat $ayatNomor$suffix")
        }
    }

    fun logCustomKhatamAyats(addedAyats: Int) {
        viewModelScope.launch {
            repository.recordDailyKhatamProgress(addedAyats = addedAyats, addedSurahs = 0)
            prefsRepository.incrementKhatamProgress(addedAyat = addedAyats, addedSurah = 0)
            showSnackbar("Alhamdulillah, +$addedAyats ayat tercatat dalam progres Khatam!")
        }
    }

    fun logCustomKhatamSurahs(addedSurahs: Int) {
        viewModelScope.launch {
            val estimatedAyats = addedSurahs * 55
            repository.recordDailyKhatamProgress(addedAyats = estimatedAyats, addedSurahs = addedSurahs)
            prefsRepository.incrementKhatamProgress(addedAyat = estimatedAyats, addedSurah = addedSurahs)
            showSnackbar("Alhamdulillah, +$addedSurahs surah tercatat dalam progres Khatam!")
        }
    }

    fun resetKhatamProgress() {
        viewModelScope.launch {
            prefsRepository.resetKhatamProgress()
            showSnackbar("Progres total Khatam telah diatur ulang")
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
    }

    fun playFullSurahAudio(surah: SurahEntity, ayats: List<AyatEntity>) {
        audioController.playFullSurah(
            surahNomor = surah.nomor,
            surahNamaLatin = surah.namaLatin,
            ayats = ayats
        )
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
                    val repo = QuranRepository(db.quranDao(), api)
                    val prefs = UserPreferencesRepository(context.applicationContext)
                    return QuranViewModel(repo, prefs) as T
                }
            }
    }
}
