package com.irengcloud.quran

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irengcloud.quran.data.local.ThemeMode
import com.irengcloud.quran.ui.QuranViewModel
import com.irengcloud.quran.ui.DailyFeaturesViewModel
import com.irengcloud.quran.reminders.DailyReminder
import com.irengcloud.quran.ui.screens.PrayerScreen
import com.irengcloud.quran.ui.components.MiniAudioPlayerBar
import com.irengcloud.quran.ui.components.TafsirBottomSheet
import com.irengcloud.quran.ui.screens.BookmarkKhatamScreen
import com.irengcloud.quran.ui.screens.HomeScreen
import com.irengcloud.quran.ui.screens.SearchAyatScreen
import com.irengcloud.quran.ui.screens.SettingsScreen
import com.irengcloud.quran.ui.screens.SurahDetailScreen
import com.irengcloud.quran.ui.theme.NurQuranTheme

enum class AppNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    SURAH(
        label = "Surah",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
        testTag = "nav_tab_surah"
    ),
    SEARCH(
        label = "Cari Ayat",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        testTag = "nav_tab_search"
    ),
    BOOKMARK_KHATAM(
        label = "Khatam",
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
        testTag = "nav_tab_bookmark"
    ),
    PRAYER(
        label = "Sholat",
        selectedIcon = Icons.Filled.AccessTime,
        unselectedIcon = Icons.Outlined.AccessTime,
        testTag = "nav_tab_prayer"
    ),
    SETTINGS(
        label = "Pengaturan",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
}

class MainActivity : ComponentActivity() {
    private var reminderOpenRequest by mutableStateOf(0L)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == DailyReminder.ACTION_OPEN_READER) reminderOpenRequest++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent.action == DailyReminder.ACTION_OPEN_READER) reminderOpenRequest++
        setContent {
            val context = LocalContext.current
            val viewModel: QuranViewModel = viewModel(
                factory = QuranViewModel.provideFactory(context)
            )
            val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()

            val useDarkTheme = when (readerSettings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            NurQuranTheme(darkTheme = useDarkTheme) {
                NurQuranApp(viewModel = viewModel, reminderOpenRequest = reminderOpenRequest,
                    onReminderHandled = { reminderOpenRequest = 0; intent.action = Intent.ACTION_MAIN })
            }
        }
    }
}

@Composable
fun NurQuranApp(viewModel: QuranViewModel, reminderOpenRequest: Long = 0, onReminderHandled: () -> Unit = {}) {
    val dailyFeatures: DailyFeaturesViewModel = viewModel()
    val allSurahs by viewModel.allSurahs.collectAsStateWithLifecycle()
    val filteredSurahs by viewModel.filteredSurahs.collectAsStateWithLifecycle()
    val surahSearchQuery by viewModel.surahSearchQuery.collectAsStateWithLifecycle()
    val selectedSurahFilter by viewModel.selectedSurahFilter.collectAsStateWithLifecycle()
    val isSyncingSurahs by viewModel.isSyncingSurahList.collectAsStateWithLifecycle()

    val lastRead by viewModel.lastRead.collectAsStateWithLifecycle()
    val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val khatamLogs by viewModel.khatamLogs.collectAsStateWithLifecycle()

    val surahDetailState by viewModel.surahDetailState.collectAsStateWithLifecycle()
    val tafsirSheetState by viewModel.tafsirSheetState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val tabStateHolder = rememberSaveableStateHolder()
    var currentTab by rememberSaveable { mutableStateOf(AppNavTab.SURAH) }
    var isReadingSurahDetail by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val openSurahAction: (Int, Int?) -> Unit = { surahNomor, targetAyat ->
        viewModel.openSurahDetail(surahNomor, targetAyat)
        isReadingSurahDetail = true
    }

    LaunchedEffect(reminderOpenRequest) {
        if (reminderOpenRequest > 0) {
            val saved = viewModel.awaitLastRead()
            viewModel.openSurahDetail(saved.surahNomor, saved.ayatNomor)
            isReadingSurahDetail = true
            onReminderHandled()
        }
    }

    BackHandler(enabled = isReadingSurahDetail) {
        isReadingSurahDetail = false
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isExpandedScreen && !isReadingSurahDetail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        AppNavTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppNavTab.entries.forEach { tab ->
                            val selected = !isReadingSurahDetail && currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    isReadingSurahDetail = false
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("${tab.testTag}_rail")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    AnimatedContent(
                        targetState = isReadingSurahDetail,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 75)).togetherWith(
                                fadeOut(animationSpec = tween(durationMillis = 50))
                            )
                        },
                        label = "SurahDetailMotionTransition"
                    ) { readingDetail ->
                        if (readingDetail) {
                            SurahDetailScreen(
                                detailState = surahDetailState,
                                bookmarks = bookmarks,
                                lastRead = lastRead,
                                readerSettings = readerSettings,
                                audioState = audioState,
                                onBack = { isReadingSurahDetail = false },
                                onOpenOtherSurah = { nextNomor ->
                                    viewModel.openSurahDetail(nextNomor, targetAyat = null)
                                },
                                onRetry = {
                                    val currentNo = surahDetailState.surah?.nomor ?: 1
                                    viewModel.openSurahDetail(currentNo, forceRefresh = true)
                                },
                                onSaveOffline = { nomor ->
                                    viewModel.saveSurahOfflineWithTafsir(nomor)
                                },
                                onPlayFullSurah = { surah, ayats ->
                                    viewModel.playFullSurahAudio(surah, ayats)
                                },
                                onPlaySingleAyat = { surah, ayats, nomorAyat ->
                                    viewModel.playSingleVerseAudio(surah, ayats, nomorAyat)
                                },
                                onTogglePlayPauseAudio = {
                                    viewModel.audioController.togglePlayPause()
                                },
                                onOpenTafsir = { surah, ayat ->
                                    viewModel.openTafsirForAyat(surah, ayat)
                                },
                                onToggleBookmark = { surah, ayat ->
                                    viewModel.toggleBookmark(surah, ayat)
                                },
                                onReadingPositionChange = viewModel::saveReadingPosition,
                                onMarkLastRead = { surah, ayatNomor ->
                                    viewModel.markLastReadAndLogKhatam(surah, ayatNomor)
                                },
                                onUpdateAutoScrollSpeed = { speed ->
                                    viewModel.updateAutoScrollSpeed(speed)
                                },
                                onClearTargetScroll = {
                                    viewModel.clearTargetScrollAyat()
                                },
                                onCopyMessage = { msg ->
                                    viewModel.showSnackbar(msg)
                                }
                            )
                        } else {
                            tabStateHolder.SaveableStateProvider(currentTab) {
                                val tab = currentTab
                                when (tab) {
                                    AppNavTab.SURAH -> {
                                        HomeScreen(
                                            surahs = filteredSurahs,
                                            searchQuery = surahSearchQuery,
                                            selectedFilter = selectedSurahFilter,
                                            lastRead = lastRead,
                                            readerSettings = readerSettings,
                                            khatamLogs = khatamLogs,
                                            isSyncing = isSyncingSurahs,
                                            onSearchQueryChange = viewModel::updateSurahSearchQuery,
                                            onFilterChange = viewModel::selectSurahFilter,
                                            onRefreshSurahs = viewModel::refreshSurahList,
                                            onOpenSurah = openSurahAction,
                                            onNavigateToKhatamTab = {
                                                currentTab = AppNavTab.BOOKMARK_KHATAM
                                            }
                                        )
                                    }

                                    AppNavTab.SEARCH -> {
                                        SearchAyatScreen(
                                            searchState = searchState,
                                            readerSettings = readerSettings,
                                            onQueryChange = viewModel::updateSearchQueryInput,
                                            onExecuteSearch = { q -> viewModel.executeSearch(query = q, page = 1) },
                                            onPageSizeChange = viewModel::updateSearchPageSize,
                                            onLoadNextPage = viewModel::loadNextSearchPage,
                                            onOpenSurahAtAyat = { sNomor, aNomor ->
                                                openSurahAction(sNomor, aNomor)
                                            },
                                            onPlaySearchItemAudio = viewModel::playSearchItemAudio
                                        )
                                    }

                                    AppNavTab.BOOKMARK_KHATAM -> {
                                        BookmarkKhatamScreen(
                                            bookmarks = bookmarks,
                                            khatamLogs = khatamLogs,
                                            lastRead = lastRead,
                                            readerSettings = readerSettings,
                                            onOpenSurahAtAyat = { sNomor, aNomor ->
                                                openSurahAction(sNomor, aNomor)
                                            },
                                            onRemoveBookmark = viewModel::removeBookmark,
                                            onTargetModeChange = viewModel::updateKhatamTargetMode,
                                            onUpdateDailyTargetAyat = viewModel::updateDailyKhatamTarget,
                                            onUpdateDailyTargetSurah = viewModel::updateDailyKhatamTargetSurah,
                                            onLogCustomAyats = viewModel::logCustomKhatamAyats,
                                            onLogCustomSurahs = viewModel::logCustomKhatamSurahs,
                                            onResetKhatam = viewModel::resetKhatamProgress
                                        )
                                    }

                                    AppNavTab.PRAYER -> {
                                        PrayerScreen(readerSettings, dailyFeatures)
                                    }

                                    AppNavTab.SETTINGS -> {
                                        val cachedCount = allSurahs.count { it.isCachedOffline }
                                        SettingsScreen(
                                            settings = readerSettings,
                                            cachedSurahCount = cachedCount,
                                            onArabicFontSizeChange = viewModel::updateArabicFontSize,
                                            onTranslationFontSizeChange = viewModel::updateTranslationFontSize,
                                            onShowLatinChange = viewModel::updateShowLatin,
                                            onShowTranslationChange = viewModel::updateShowTranslation,
                                            onAutoNextAudioChange = viewModel::updateAutoNextAudio,
                                            onThemeModeChange = viewModel::updateThemeMode,
                                            dailyFeatures = dailyFeatures
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating Mini Audio Player Bar
                    MiniAudioPlayerBar(
                        audioState = audioState,
                        onTogglePlayPause = { viewModel.audioController.togglePlayPause() },
                        onNext = { viewModel.audioController.playNextVerse() },
                        onPrevious = { viewModel.audioController.playPreviousVerse() },
                        onClose = { viewModel.audioController.stop() },
                        onJumpToSurah = { sNomor, aNomor ->
                            openSurahAction(sNomor, aNomor)
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            // Kemenag RI Tafsir Modal Bottom Sheet
            TafsirBottomSheet(
                state = tafsirSheetState,
                arabicFontSizeSp = readerSettings.arabicFontSizeSp,
                onDismiss = viewModel::closeTafsirSheet,
                onCopyTafsir = viewModel::showSnackbar
            )
        }
    }
}
