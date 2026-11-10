package com.irengcloud.quran

import android.app.Activity
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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
import com.irengcloud.quran.ui.screens.MoreScreen
import com.irengcloud.quran.ui.screens.MurottalScreen
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
    MORE(
        label = "Lainnya",
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Filled.Apps,
        testTag = "nav_tab_more"
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
                ThemeMode.CLASSIC, ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            NurQuranTheme(
                darkTheme = useDarkTheme,
                ivoryTheme = readerSettings.themeMode == ThemeMode.LIGHT
            ) {
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
    var bookmarkSection by rememberSaveable { mutableStateOf(1) }
    var isReadingSurahDetail by rememberSaveable { mutableStateOf(false) }
    var murottalStartVerse by rememberSaveable { mutableStateOf<Int?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val openSurahAction: (Int, Int?) -> Unit = { surahNomor, targetAyat ->
        murottalStartVerse = null
        tabStateHolder.removeState("surah_reader")
        viewModel.openSurahDetail(surahNomor, targetAyat)
        isReadingSurahDetail = true
    }

    LaunchedEffect(reminderOpenRequest) {
        if (reminderOpenRequest > 0) {
            val saved = viewModel.awaitLastRead()
            murottalStartVerse = null
            tabStateHolder.removeState("surah_reader")
            viewModel.openSurahDetail(saved.surahNomor, saved.ayatNomor)
            isReadingSurahDetail = true
            onReminderHandled()
        }
    }

    BackHandler(enabled = !isReadingSurahDetail && currentTab in listOf(AppNavTab.SEARCH, AppNavTab.PRAYER, AppNavTab.SETTINGS)) {
        currentTab = AppNavTab.MORE
    }
    BackHandler(enabled = !isReadingSurahDetail && currentTab == AppNavTab.MORE) {
        currentTab = AppNavTab.SURAH
    }

    BackHandler(enabled = isReadingSurahDetail) {
        if (murottalStartVerse != null) murottalStartVerse = null
        else isReadingSurahDetail = false
    }

    val fullScreenPage = isReadingSurahDetail || currentTab == AppNavTab.SETTINGS
    val pageBackground = when {
        isReadingSurahDetail -> Color(0xFF252439)
        currentTab == AppNavTab.SEARCH -> Color(0xFF202034)
        currentTab == AppNavTab.SETTINGS -> Color(0xFF242338)
        else -> MaterialTheme.colorScheme.background
    }
    val navigationBackground = if (fullScreenPage) pageBackground else MaterialTheme.colorScheme.surface
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = pageBackground.luminance() > 0.5f
            controller.isAppearanceLightNavigationBars = navigationBackground.luminance() > 0.5f
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = pageBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isExpandedScreen && !fullScreenPage) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        val labels = listOf("Beranda", "Baca", "Penanda", "Lainnya")
                        labels.forEachIndexed { index, label ->
                            val selected = when (index) {
                                0 -> currentTab == AppNavTab.SURAH
                                2 -> currentTab == AppNavTab.BOOKMARK_KHATAM
                                3 -> currentTab != AppNavTab.SURAH && currentTab != AppNavTab.BOOKMARK_KHATAM
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    when (index) {
                                        0 -> currentTab = AppNavTab.SURAH
                                        1 -> openSurahAction(lastRead.surahNomor, lastRead.ayatNomor)
                                        2 -> { bookmarkSection = 1; currentTab = AppNavTab.BOOKMARK_KHATAM }
                                        3 -> currentTab = AppNavTab.MORE
                                    }
                                },
                                icon = {
                                    Icon(when (index) {
                                        0 -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                        1 -> Icons.AutoMirrored.Outlined.MenuBook
                                        2 -> if (selected) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
                                        else -> Icons.Filled.Apps
                                    }, label)
                                },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.testTag("nav_mockup_$index")
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
                                    murottalStartVerse = null
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
                    key(isReadingSurahDetail) {
                        val readingDetail = isReadingSurahDetail
                        val startMurottal = murottalStartVerse
                        val openedSurah = surahDetailState.surah
                        if (readingDetail && startMurottal != null && openedSurah != null) {
                            MurottalScreen(
                                detail = surahDetailState,
                                surah = openedSurah,
                                initialVerse = startMurottal,
                                settings = readerSettings,
                                viewModel = viewModel,
                                onBack = { murottalStartVerse = null }
                            )
                        } else if (readingDetail) {
                            tabStateHolder.SaveableStateProvider("surah_reader") {
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
                                    onOpenMurottal = { murottalStartVerse = it },
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
                                    onUpdateArabicFontSize = viewModel::updateArabicFontSize,
                                    onUpdateShowLatin = viewModel::updateShowLatin,
                                    onUpdateShowTranslation = viewModel::updateShowTranslation,
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
                            }
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
                                            onNavigateToPrayer = { currentTab = AppNavTab.PRAYER },
                                            onNavigateToSettings = { currentTab = AppNavTab.SETTINGS },
                                            onNavigateToBookmarks = {
                                                bookmarkSection = 1
                                                currentTab = AppNavTab.BOOKMARK_KHATAM
                                            },
                                            onNavigateToKhatamTab = {
                                                bookmarkSection = 0
                                                currentTab = AppNavTab.BOOKMARK_KHATAM
                                            }
                                        )
                                    }

                                    AppNavTab.SEARCH -> {
                                        SearchAyatScreen(
                                            searchState = searchState,
                                            readerSettings = readerSettings,
                                            onQueryChange = viewModel::updateSearchQueryInput,
                                            onClearSearch = viewModel::clearSearch,
                                            onExecuteSearch = { q -> viewModel.executeSearch(query = q, page = 1) },
                                            onPageSizeChange = viewModel::updateSearchPageSize,
                                            onLoadNextPage = viewModel::loadNextSearchPage,
                                            onOpenSurahAtAyat = { sNomor, aNomor ->
                                                openSurahAction(sNomor, aNomor)
                                            },
                                            onPlaySearchItemAudio = viewModel::playSearchItemAudio,
                                            onBack = { currentTab = AppNavTab.MORE }
                                        )
                                    }

                                    AppNavTab.BOOKMARK_KHATAM -> {
                                        BookmarkKhatamScreen(
                                            selectedSection = bookmarkSection,
                                            onSectionChange = { bookmarkSection = it },
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

                                    AppNavTab.MORE -> {
                                        MoreScreen(
                                            onOpenSearch = { currentTab = AppNavTab.SEARCH },
                                            onOpenPrayer = { currentTab = AppNavTab.PRAYER },
                                            onOpenSettings = { currentTab = AppNavTab.SETTINGS }
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
                                            dailyFeatures = dailyFeatures,
                                            onBack = { currentTab = AppNavTab.MORE }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating Mini Audio Player Bar
                    if (!isReadingSurahDetail) MiniAudioPlayerBar(
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
