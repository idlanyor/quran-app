package com.irengcloud.quran.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.irengcloud.quran.R
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irengcloud.quran.data.local.AyatEntity
import com.irengcloud.quran.data.local.BookmarkEntity
import com.irengcloud.quran.data.local.LastReadState
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.data.local.SurahEntity
import com.irengcloud.quran.ui.AudioPlaybackState
import com.irengcloud.quran.ui.SurahDetailUiState
import com.irengcloud.quran.ui.components.VerseNumberBadge
import com.irengcloud.quran.ui.components.rememberArtwork
import com.irengcloud.quran.ui.components.TransparentArtworkPaper
import com.irengcloud.quran.ui.theme.AmiriQuranFontFamily
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(
    detailState: SurahDetailUiState,
    bookmarks: List<BookmarkEntity>,
    lastRead: LastReadState,
    readerSettings: ReaderSettingsState,
    audioState: AudioPlaybackState,
    onBack: () -> Unit,
    onOpenOtherSurah: (Int) -> Unit,
    onRetry: () -> Unit,
    onSaveOffline: (Int) -> Unit,
    onPlayFullSurah: (SurahEntity, List<AyatEntity>) -> Unit,
    onPlaySingleAyat: (SurahEntity, List<AyatEntity>, Int) -> Unit,
    onTogglePlayPauseAudio: () -> Unit,
    onOpenMurottal: (Int) -> Unit,
    onOpenTafsir: (SurahEntity, AyatEntity) -> Unit,
    onToggleBookmark: (SurahEntity, AyatEntity) -> Unit,
    onMarkLastRead: (SurahEntity, Int) -> Unit,
    onReadingPositionChange: (SurahEntity, Int) -> Unit,
    onUpdateAutoScrollSpeed: (Int) -> Unit,
    onClearTargetScroll: () -> Unit,
    onCopyMessage: (String) -> Unit,
    onUpdateArabicFontSize: (Int) -> Unit = {},
    onUpdateShowLatin: (Boolean) -> Unit = {},
    onUpdateShowTranslation: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val listState = key(detailState.surah?.nomor) {
        rememberLazyListState(initialFirstVisibleItemIndex = detailState.targetScrollAyat ?: 0)
    }
    val coroutineScope = rememberCoroutineScope()
    val surah = detailState.surah
    val ayats = detailState.ayats

    val bookmarkedKeys = remember(bookmarks) {
        bookmarks.mapTo(HashSet()) { it.surahNomor * 1000 + it.nomorAyat }
    }

    var pendingReadAyat by remember(surah?.nomor) { mutableStateOf<Int?>(null) }

    var isAutoScrolling by rememberSaveable { mutableStateOf(false) }
    var showJumpAyatBar by rememberSaveable { mutableStateOf(false) }
    val speedLevel = readerSettings.autoScrollSpeedLevel.coerceIn(1, 10)

    // Determine currently visible verse number from LazyListState
    val currentVisibleAyat by remember(ayats) {
        derivedStateOf {
            if (ayats.isEmpty()) 1
            else {
                val firstIdx = listState.firstVisibleItemIndex
                // Index 0 is SurahHeaderBanner, so verse index is (firstIdx - 1).coerceAtLeast(0)
                val ayatIdx = (firstIdx - 1).coerceIn(0, (ayats.size - 1).coerceAtLeast(0))
                ayats.getOrNull(ayatIdx)?.nomorAyat ?: 1
            }
        }
    }

    // Stop auto-scrolling when switching surah
    LaunchedEffect(surah?.nomor) {
        isAutoScrolling = false
    }

    // Continuous smooth auto-scroll loop (~60fps)
    LaunchedEffect(isAutoScrolling, speedLevel, ayats.size) {
        if (isAutoScrolling && ayats.isNotEmpty()) {
            // Frame-synced: distance is derived from real elapsed frame time, so movement stays
            // smooth regardless of vsync jitter or display refresh rate.
            var lastFrameNanos = withFrameNanos { it }
            var carry = 0f
            while (isActive && isAutoScrolling) {
                if (!listState.canScrollForward) {
                    isAutoScrolling = false
                    break
                }
                val now = withFrameNanos { it }
                val dtSeconds = ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastFrameNanos = now
                // Speed level 1..10 maps to ~44px/s .. ~325px/s (same as before).
                val pixelsPerSecond = (0.45f + (speedLevel * 0.48f)) * 62.5f
                // Keep the sub-pixel remainder so slow speeds don't stall on rounding.
                val wanted = pixelsPerSecond * dtSeconds + carry
                val consumed = listState.scrollBy(wanted)
                carry = wanted - consumed
                if (consumed == 0f && !listState.canScrollForward) {
                    isAutoScrolling = false
                    break
                }
            }
        }
    }

    // Scroll to target verse if opened from Last Read, Bookmark, or Search
    LaunchedEffect(surah?.nomor, detailState.targetScrollAyat, ayats) {
        val target = detailState.targetScrollAyat
        if (target != null && ayats.isNotEmpty()) {
            val indexInAyats = ayats.indexOfFirst { it.nomorAyat == target }
            if (indexInAyats >= 0) {
                // +1 offset for the Surah Header card item
                listState.requestScrollToItem(indexInAyats + 1)
                // Let the lazy list apply the requested position during layout
                // before enabling automatic position persistence.
                withFrameNanos { }
            }
            onClearTargetScroll()
        }
    }

    // Persist a settled reading position without treating scrolling as completed tilawah.
    LaunchedEffect(surah?.nomor, ayats, detailState.targetScrollAyat) {
        if (surah != null && ayats.isNotEmpty() && detailState.targetScrollAyat == null) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.isScrollInProgress }
                .distinctUntilChanged().collectLatest { (index, scrolling) ->
                    if (!scrolling) {
                        delay(400)
                        val verse = ayats[(index - 1).coerceIn(0, ayats.lastIndex)]
                        onReadingPositionChange(surah, verse.nomorAyat)
                    }
                }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, surah?.nomor, ayats, detailState.targetScrollAyat) {
        fun saveCurrentPosition() {
            if (surah != null && ayats.isNotEmpty() && detailState.targetScrollAyat == null) {
                val index = (listState.firstVisibleItemIndex - 1).coerceIn(0, ayats.lastIndex)
                onReadingPositionChange(surah, ayats[index].nomorAyat)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) saveCurrentPosition()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            saveCurrentPosition()
        }
    }
    pendingReadAyat?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingReadAyat = null },
            title = { Text("Konfirmasi bacaan selesai") },
            text = { Text("Tandai QS. ${surah?.namaLatin} dari ayat 1 sampai $target sudah dibaca? Ayat yang sudah tercatat tidak dihitung ulang.") },
            confirmButton = {
                TextButton(onClick = {
                    surah?.let { onMarkLastRead(it, target) }
                    pendingReadAyat = null
                }) { Text("Sudah dibaca") }
            },
            dismissButton = { TextButton(onClick = { pendingReadAyat = null }) { Text("Batal") } }
        )
    }

    var showReaderMenu by remember { mutableStateOf(false) }
    var showReaderTools by rememberSaveable { mutableStateOf(false) }
    var showTypography by rememberSaveable { mutableStateOf(false) }
    val readerInk = Color(0xFF252439)
    val readerPaper = MaterialTheme.colorScheme.background
    val readerCopper = Color(0xFFD4A178)
    if (showTypography) {
        AlertDialog(onDismissRequest = { showTypography = false },
            title = { Text("Tampilan bacaan") },
            text = {
                Column {
                    Text("Ukuran teks Arab · ${readerSettings.arabicFontSizeSp}")
                    Slider(value = readerSettings.arabicFontSizeSp.toFloat(),
                        onValueChange = { onUpdateArabicFontSize(it.roundToInt()) }, valueRange = 22f..44f)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Tampilkan latin")
                        Switch(readerSettings.showLatin, onUpdateShowLatin)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Tampilkan terjemahan")
                        Switch(readerSettings.showTranslation, onUpdateShowTranslation)
                    }
                }
            }, confirmButton = { TextButton(onClick = { showTypography = false }) { Text("Selesai") } })
    }

    Scaffold(modifier = modifier.fillMaxSize(), containerColor = readerInk,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        if (surah != null) {
            Surface(modifier = Modifier.fillMaxWidth().height(72.dp), color = readerInk, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
                Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        ayats.getOrNull((currentVisibleAyat - 1).coerceAtLeast(0))?.let { onOpenTafsir(surah, it) }
                    }, enabled = ayats.isNotEmpty(), modifier = Modifier.testTag("reader_tafsir")) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = Color(0xFFF1DFCE), modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tafsir", color = Color(0xFFF1DFCE))
                    }
                    IconButton(onClick = {
                        if (audioState.isActive && audioState.surahNomor == surah.nomor) onTogglePlayPauseAudio()
                        else onPlayFullSurah(surah, ayats)
                    }, enabled = ayats.isNotEmpty(), modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color(0xFFE8BB94), Color(0xFFAD7048))))
                        .testTag("play_full_surah_button")) {
                        Icon(if (audioState.isPlaying && audioState.surahNomor == surah.nomor) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Putar atau jeda murottal", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    TextButton(
                        onClick = { onOpenMurottal(currentVisibleAyat) },
                        modifier = Modifier.testTag("reader_murottal")
                    ) {
                        Icon(Icons.Outlined.Headphones, null, tint = Color(0xFFF1DFCE), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Murottal", color = Color(0xFFF1DFCE))
                    }
                    TextButton(onClick = { showTypography = true }, modifier = Modifier.testTag("reader_typography")) {
                        Text("Aa", color = Color(0xFFF1DFCE), fontSize = 21.sp, fontFamily = FontFamily.Serif)
                    }
                }
            }
        }
    }) { readerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(readerPadding).background(readerInk)) {
        Box(Modifier.fillMaxWidth().height(64.dp)) {
            ReaderArtworkSlice(0f, 0.293f, Modifier.fillMaxSize())
            Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isAutoScrolling = false; onBack() },
                    modifier = Modifier.testTag("surah_detail_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color(0xFFFFF4E6))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    surah?.let { chapter -> ayats.getOrNull((currentVisibleAyat - 1).coerceAtLeast(0))?.let { onToggleBookmark(chapter, it) } }
                }) {
                    Icon(Icons.Outlined.BookmarkBorder, "Tandai ayat yang terlihat", tint = Color(0xFFF1DFCE))
                }
                Box {
                    IconButton(onClick = { showReaderMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu pembaca", tint = Color(0xFFF1DFCE))
                    }
                    DropdownMenu(showReaderMenu, onDismissRequest = { showReaderMenu = false }) {
                        DropdownMenuItem(text = { Text("Lompat ayat & gulir otomatis") }, onClick = {
                            showReaderTools = !showReaderTools; showReaderMenu = false
                        })
                        DropdownMenuItem(text = { Text(if (surah?.isCachedOffline == true) "Perbarui unduhan offline" else "Simpan offline") },
                            enabled = surah != null && !detailState.isOfflineSaving, onClick = {
                                surah?.let { onSaveOffline(it.nomor) }; showReaderMenu = false
                            })
                        DropdownMenuItem(text = { Text("Surah sebelumnya") }, enabled = surah != null && surah.nomor > 1,
                            onClick = { surah?.let { onOpenOtherSurah(it.nomor - 1) }; showReaderMenu = false })
                        DropdownMenuItem(text = { Text("Surah berikutnya") }, enabled = surah != null && surah.nomor < 114,
                            onClick = { surah?.let { onOpenOtherSurah(it.nomor + 1) }; showReaderMenu = false })
                    }
                }
            }
        }

        // Sticky Top Navigation & Auto-Scroll Bar (Pinned above LazyColumn so it stays fixed at top)
        if (showReaderTools && surah != null && ayats.isNotEmpty()) {
            StickyAyatNavigationAndAutoScrollBar(
                surah = surah,
                ayats = ayats,
                currentAyat = { currentVisibleAyat },
                isAutoScrolling = isAutoScrolling,
                speedLevel = speedLevel,
                showJumpAyatBar = showJumpAyatBar,
                onToggleAutoScroll = { isAutoScrolling = !isAutoScrolling },
                onSpeedChange = onUpdateAutoScrollSpeed,
                onToggleJumpAyatBar = { showJumpAyatBar = !showJumpAyatBar },
                onJumpToAyat = { targetAyat ->
                    val idx = ayats.indexOfFirst { it.nomorAyat == targetAyat }
                    if (idx >= 0) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(idx + 1)
                        }
                    }
                }
            )
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                detailState.isLoading && ayats.isEmpty() && surah == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Memuat ayat & audio surah dari server…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                detailState.errorMessage != null && ayats.isEmpty() -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Gagal Memuat Surah",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = detailState.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.testTag("retry_surah_detail_button")
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }

                surah != null -> {
                    val isSurahPlaying = audioState.isActive &&
                        audioState.surahNomor == surah.nomor &&
                        audioState.isPlaying

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                            .background(readerPaper)
                            .testTag("ayat_lazy_column"),
                        contentPadding = PaddingValues(
                            start = 0.dp,
                            end = 0.dp,
                            top = 0.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Surah Banner Card + Bismillah
                        item(key = "reader_opening", contentType = "opening") {
                            ReaderOpening(surah = surah)
                        }

                        items(
                            items = ayats,
                            key = { "${it.surahNomor}_${it.nomorAyat}" },
                            contentType = { "ayat" }
                        ) { ayat ->
                            val isBookmarked = (surah.nomor * 1000 + ayat.nomorAyat) in bookmarkedKeys
                            val isLastReadVerse = lastRead.updatedAt > 0 && lastRead.surahNomor == surah.nomor &&
                                lastRead.ayatNomor == ayat.nomorAyat
                            val isAudioActiveOnVerse = audioState.isActive &&
                                audioState.surahNomor == surah.nomor &&
                                audioState.currentAyatNomor == ayat.nomorAyat

                            AyatItemCard(
                                surah = surah,
                                ayat = ayat,
                                readerSettings = readerSettings,
                                isBookmarked = isBookmarked,
                                isLastRead = isLastReadVerse,
                                isAudioActive = isAudioActiveOnVerse,
                                isAudioPlaying = isAudioActiveOnVerse && audioState.isPlaying,
                                onPlayAudio = {
                                    if (isAudioActiveOnVerse) {
                                        onTogglePlayPauseAudio()
                                    } else {
                                        onPlaySingleAyat(surah, ayats, ayat.nomorAyat)
                                    }
                                },
                                onOpenTafsir = { onOpenTafsir(surah, ayat) },
                                onToggleBookmark = { onToggleBookmark(surah, ayat) },
                                onMarkLastRead = { pendingReadAyat = ayat.nomorAyat },
                                onCopyMessage = onCopyMessage
                            )
                        }
                    }
                }
            }
        }


    }
    }
}

/**
 * Sticky top navigation bar in SurahDetailScreen with:
 * - Current Ayat position indicator & expandable Quick Jump to Ayat bar
 * - Auto-Scroll play/pause button
 * - Adjustable Auto-Scroll speed controls (- / + buttons and smooth slider 1x..10x)
 */
@Composable
private fun StickyAyatNavigationAndAutoScrollBar(
    surah: SurahEntity,
    ayats: List<AyatEntity>,
    currentAyat: () -> Int,
    isAutoScrolling: Boolean,
    speedLevel: Int,
    showJumpAyatBar: Boolean,
    onToggleAutoScroll: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onToggleJumpAyatBar: () -> Unit,
    onJumpToAyat: (Int) -> Unit
) {
    // Read the scroll-driven value here so only this bar recomposes while scrolling.
    val currentAyat = currentAyat()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sticky_autoscroll_navbar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Current Ayat Pill & Quick Jump Toggle
                Surface(
                    color = if (showJumpAyatBar) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onToggleJumpAyatBar)
                        .testTag("toggle_jump_ayat_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Ayat $currentAyat / ${surah.jumlahAyat}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (showJumpAyatBar) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardDoubleArrowDown,
                            contentDescription = "Pilih Lompat Ayat",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Right: Auto-Scroll Toggle + Speed Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Decrease Speed (-)
                    IconButton(
                        onClick = { onSpeedChange((speedLevel - 1).coerceAtLeast(1)) },
                        enabled = speedLevel > 1,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("autoscroll_speed_minus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Kurangi Kecepatan Auto-Scroll",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Speed Indicator Badge
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${speedLevel}x",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Increase Speed (+)
                    IconButton(
                        onClick = { onSpeedChange((speedLevel + 1).coerceAtMost(10)) },
                        enabled = speedLevel < 10,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("autoscroll_speed_plus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Kecepatan Auto-Scroll",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Start / Pause Auto-Scroll Button
                    Button(
                        onClick = onToggleAutoScroll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAutoScrolling) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("toggle_autoscroll_button")
                    ) {
                        Icon(
                            imageVector = if (isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAutoScrolling) "Jeda Scroll" else "Auto Scroll",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Expandable Speed Slider when Auto-Scroll is active
            AnimatedVisibility(
                visible = isAutoScrolling,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Kecepatan:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = speedLevel.toFloat(),
                        onValueChange = { onSpeedChange(it.roundToInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .testTag("autoscroll_speed_slider")
                    )
                    Text(
                        text = when {
                            speedLevel <= 2 -> "Lambat"
                            speedLevel <= 5 -> "Sedang"
                            speedLevel <= 8 -> "Cepat"
                            else -> "Sangat Cepat"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Expandable Quick-Jump Ayat Selector Bar
            AnimatedVisibility(
                visible = showJumpAyatBar,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item(key = "jump_label") {
                            Text(
                                text = "Lompat ke Ayat:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        items(ayats, key = { it.nomorAyat }) { ayat ->
                            val selected = ayat.nomorAyat == currentAyat
                            FilterChip(
                                selected = selected,
                                onClick = { onJumpToAyat(ayat.nomorAyat) },
                                label = { Text("${ayat.nomorAyat}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("jump_ayat_chip_${ayat.nomorAyat}")
                            )
                        }
                    }
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

@Composable
private fun ReaderOpening(surah: SurahEntity) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            ReaderArtworkSlice(0.293f, 1f, Modifier.fillMaxWidth()
                .aspectRatio(2f / 0.707f), blendIntoPaper = true)
            Column(Modifier.align(Alignment.BottomCenter).padding(horizontal = 36.dp, vertical = 17.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(surah.namaArab,
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = AmiriQuranFontFamily,
                        fontSize = 32.sp, lineHeight = 44.sp, textDirection = TextDirection.Rtl),
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Text(surah.namaLatin, style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(3.dp))
                Text("Surah ke-${surah.nomor} · ${surah.jumlahAyat} ayat",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        if (surah.nomor != 1 && surah.nomor != 9) {
            // Reuse the existing verified opening invocation from the original reader.
            Text(text = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = AmiriQuranFontFamily, lineHeight = 48.sp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background).padding(vertical = 12.dp))
        }
    }
}

@Composable
private fun AyatItemCard(
    surah: SurahEntity,
    ayat: AyatEntity,
    readerSettings: ReaderSettingsState,
    isBookmarked: Boolean,
    isLastRead: Boolean,
    isAudioActive: Boolean,
    isAudioPlaying: Boolean,
    onPlayAudio: () -> Unit,
    onOpenTafsir: () -> Unit,
    onToggleBookmark: () -> Unit,
    onMarkLastRead: () -> Unit,
    onCopyMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var showActions by remember { mutableStateOf(false) }
    val paper = MaterialTheme.colorScheme.background
    val highlight = if (isAudioActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else paper
    Column(Modifier.fillMaxWidth().background(highlight).padding(horizontal = 24.dp)
        .testTag("ayat_card_${ayat.nomorAyat}")) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { showActions = true }, modifier = Modifier.size(48.dp)
                    .semantics { contentDescription = "Menu ayat ${ayat.nomorAyat}" }) {
                    VerseNumberBadge(ayat.nomorAyat, size = 34.dp,
                        isHighlighted = isAudioActive || isLastRead || isBookmarked)
                }
                DropdownMenu(showActions, onDismissRequest = { showActions = false }) {
                    DropdownMenuItem(text = { Text(if (isAudioPlaying) "Jeda ayat" else "Putar ayat") },
                        onClick = { onPlayAudio(); showActions = false }, modifier = Modifier.testTag("play_ayat_button_${ayat.nomorAyat}"))
                    DropdownMenuItem(text = { Text(if (isBookmarked) "Hapus penanda" else "Simpan penanda") },
                        onClick = { onToggleBookmark(); showActions = false }, modifier = Modifier.testTag("bookmark_ayat_button_${ayat.nomorAyat}"))
                    DropdownMenuItem(text = { Text("Tandai selesai dibaca") }, onClick = { onMarkLastRead(); showActions = false },
                        modifier = Modifier.testTag("lastread_ayat_button_${ayat.nomorAyat}"))
                    DropdownMenuItem(text = { Text("Salin ayat") }, onClick = {
                        clipboard.setText(AnnotatedString("QS. ${surah.namaLatin} (${surah.nomor}): ${ayat.nomorAyat}\n${ayat.teksArab}\n${ayat.teksIndonesia}"))
                        onCopyMessage("Ayat disalin"); showActions = false
                    }, modifier = Modifier.testTag("copy_ayat_button_${ayat.nomorAyat}"))
                    DropdownMenuItem(text = { Text("Baca tafsir") }, onClick = { onOpenTafsir(); showActions = false },
                        modifier = Modifier.testTag("tafsir_ayat_button_${ayat.nomorAyat}"))
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(ayat.teksArab,
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = AmiriQuranFontFamily,
                    fontSize = readerSettings.arabicFontSizeSp.sp,
                    lineHeight = (readerSettings.arabicFontSizeSp * 1.9f).sp, textDirection = TextDirection.Rtl),
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Right,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp))
        }
        if (readerSettings.showLatin && ayat.teksLatin.isNotBlank()) {
            Text(ayat.teksLatin, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))
        }
        if (readerSettings.showTranslation && ayat.teksIndonesia.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(ayat.teksIndonesia,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif,
                        fontSize = readerSettings.translationFontSizeSp.sp,
                        lineHeight = (readerSettings.translationFontSizeSp * 1.5f).sp, textDirection = TextDirection.Ltr),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 23.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text("۞", fontFamily = AmiriQuranFontFamily, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ReaderArtworkSlice(start: Float, end: Float, modifier: Modifier = Modifier, blendIntoPaper: Boolean = false) {
    val artwork by rememberArtwork(R.drawable.reader_top)
    val paper = MaterialTheme.colorScheme.background
    val useDarkPaper = blendIntoPaper && paper.luminance() < 0.5f
    Canvas(modifier) {
        val bitmap = artwork
        if (bitmap == null) {
            if (blendIntoPaper) drawRect(paper)
            return@Canvas
        }
        val top = (bitmap.height * start).roundToInt()
        val bottom = (bitmap.height * end).roundToInt().coerceAtMost(bitmap.height)
        if (useDarkPaper) drawRect(paper)
        drawImage(bitmap, srcOffset = IntOffset(0, top),
            srcSize = IntSize(bitmap.width, (bottom - top).coerceAtLeast(1)),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            colorFilter = if (useDarkPaper) TransparentArtworkPaper else null,
            filterQuality = FilterQuality.High)
        if (blendIntoPaper) {
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, paper),
                startY = size.height * 0.88f, endY = size.height))
        }
    }
}
