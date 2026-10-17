package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.example.data.local.ThemeMode
import com.example.ui.QuranViewModel
import com.example.ui.components.MiniAudioPlayerBar
import com.example.ui.components.TafsirBottomSheet
import com.example.ui.screens.BookmarkKhatamScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchAyatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SurahDetailScreen
import com.example.ui.theme.NurQuranTheme

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
    SETTINGS(
        label = "Pengaturan",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                NurQuranApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NurQuranApp(viewModel: QuranViewModel) {
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
                            if (targetState) {
                                // Entering Surah Detail: slide in from right, slide out tab content to left
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 300))).togetherWith(
                                    slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() },
                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                                )
                            } else {
                                // Returning from Surah Detail: slide in tab content from left, slide out Surah detail to right
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() },
                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 300))).togetherWith(
                                    slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> fullWidth },
                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                                )
                            }
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
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    val isForward = targetState.ordinal > initialState.ordinal
                                    val direction = if (isForward) 1 else -1
                                    (slideInHorizontally(
                                        initialOffsetX = { fullWidth -> (direction * fullWidth * 0.25f).toInt() },
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(durationMillis = 250))).togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> (-direction * fullWidth * 0.25f).toInt() },
                                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                        ) + fadeOut(animationSpec = tween(durationMillis = 180))
                                    )
                                },
                                label = "BottomTabMotionTransition"
                            ) { tab ->
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
                                            onThemeModeChange = viewModel::updateThemeMode
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
