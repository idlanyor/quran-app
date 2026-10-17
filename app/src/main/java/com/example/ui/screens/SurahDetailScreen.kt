package com.example.ui.screens

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.local.AyatEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.LastReadState
import com.example.data.local.ReaderSettingsState
import com.example.data.local.SurahEntity
import com.example.ui.AudioPlaybackState
import com.example.ui.SurahDetailUiState
import com.example.ui.components.VerseNumberBadge
import com.example.ui.theme.AmiriQuranFontFamily
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
    onOpenTafsir: (SurahEntity, AyatEntity) -> Unit,
    onToggleBookmark: (SurahEntity, AyatEntity) -> Unit,
    onMarkLastRead: (SurahEntity, Int) -> Unit,
    onUpdateAutoScrollSpeed: (Int) -> Unit,
    onClearTargetScroll: () -> Unit,
    onCopyMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val surah = detailState.surah
    val ayats = detailState.ayats

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
            while (isActive && isAutoScrolling) {
                if (!listState.canScrollForward) {
                    isAutoScrolling = false
                    break
                }
                // Speed level 1..10 maps to ~0.7px .. ~5.2px per 16ms frame (~44px/s to ~325px/s)
                val pixelsPerFrame = 0.45f + (speedLevel * 0.48f)
                val consumed = listState.scrollBy(pixelsPerFrame)
                if (consumed == 0f && !listState.canScrollForward) {
                    isAutoScrolling = false
                    break
                }
                delay(16L)
            }
        }
    }

    // Scroll to target verse if opened from Last Read, Bookmark, or Search
    LaunchedEffect(detailState.targetScrollAyat, ayats.size) {
        val target = detailState.targetScrollAyat
        if (target != null && ayats.isNotEmpty()) {
            val indexInAyats = ayats.indexOfFirst { it.nomorAyat == target }
            if (indexInAyats >= 0) {
                // +1 offset for the Surah Header card item
                listState.animateScrollToItem(indexInAyats + 1)
            }
            onClearTargetScroll()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = surah?.let { "${it.nomor}. ${it.namaLatin}" } ?: "Memuat Surah…",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (surah != null) {
                        Text(
                            text = "${surah.arti} • ${surah.tempatTurun} • ${surah.jumlahAyat} Ayat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        isAutoScrolling = false
                        onBack()
                    },
                    modifier = Modifier.testTag("surah_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali"
                    )
                }
            },
            actions = {
                if (surah != null) {
                    if (surah.nomor > 1) {
                        IconButton(
                            onClick = {
                                isAutoScrolling = false
                                onOpenOtherSurah(surah.nomor - 1)
                            },
                            modifier = Modifier.testTag("prev_surah_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Surah Sebelumnya",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (surah.nomor < 114) {
                        IconButton(
                            onClick = {
                                isAutoScrolling = false
                                onOpenOtherSurah(surah.nomor + 1)
                            },
                            modifier = Modifier.testTag("next_surah_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Surah Berikutnya",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Sticky Top Navigation & Auto-Scroll Bar (Pinned above LazyColumn so it stays fixed at top)
        if (surah != null && ayats.isNotEmpty()) {
            StickyAyatNavigationAndAutoScrollBar(
                surah = surah,
                ayats = ayats,
                currentAyat = currentVisibleAyat,
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                detailState.isLoading && ayats.isEmpty() -> {
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
                            .testTag("ayat_lazy_column"),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 10.dp,
                            bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Surah Banner Card + Bismillah
                        item {
                            SurahHeaderBanner(
                                surah = surah,
                                isPlayingSurah = isSurahPlaying,
                                isOfflineSaving = detailState.isOfflineSaving,
                                onPlayFullSurah = {
                                    if (isSurahPlaying) {
                                        onTogglePlayPauseAudio()
                                    } else {
                                        onPlayFullSurah(surah, ayats)
                                    }
                                },
                                onSaveOffline = { onSaveOffline(surah.nomor) }
                            )
                        }

                        items(
                            items = ayats,
                            key = { "${it.surahNomor}_${it.nomorAyat}" }
                        ) { ayat ->
                            val isBookmarked = bookmarks.any {
                                it.surahNomor == surah.nomor && it.nomorAyat == ayat.nomorAyat
                            }
                            val isLastReadVerse = lastRead.surahNomor == surah.nomor &&
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
                                onMarkLastRead = { onMarkLastRead(surah, ayat.nomorAyat) },
                                onCopyMessage = onCopyMessage
                            )
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
    currentAyat: Int,
    isAutoScrolling: Boolean,
    speedLevel: Int,
    showJumpAyatBar: Boolean,
    onToggleAutoScroll: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onToggleJumpAyatBar: () -> Unit,
    onJumpToAyat: (Int) -> Unit
) {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lompat ke Ayat:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        ayats.forEach { ayat ->
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
private fun SurahHeaderBanner(
    surah: SurahEntity,
    isPlayingSurah: Boolean,
    isOfflineSaving: Boolean,
    onPlayFullSurah: () -> Unit,
    onSaveOffline: () -> Unit
) {
    val gradient = Brush.linearGradient(
        listOf(
            Color(0xFF0284C7),
            Color(0xFF0369A1),
            Color(0xFF0F172A)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("surah_header_banner"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = surah.namaArab,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = AmiriQuranFontFamily,
                    fontSize = 34.sp,
                    lineHeight = 42.sp
                ),
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = surah.namaLatin,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Text(
                text = "${surah.arti} • ${surah.tempatTurun} • ${surah.jumlahAyat} Ayat",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onPlayFullSurah,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0369A1)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("play_full_surah_button")
                ) {
                    Icon(
                        imageVector = if (isPlayingSurah) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPlayingSurah) "Jeda Murottal" else "Putar Murottal")
                }

                OutlinedButton(
                    onClick = onSaveOffline,
                    enabled = !isOfflineSaving,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("save_surah_offline_button")
                ) {
                    if (isOfflineSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = if (surah.isCachedOffline) Icons.Default.CheckCircle else Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (surah.isCachedOffline) Color(0xFF5EEAD4) else Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (surah.isCachedOffline) "Tersimpan Offline" else "Simpan Offline"
                    )
                }
            }

            // Show Bismillah for all Surahs except Surah 1 (already verse 1) and Surah 9 (At-Taubah)
            if (surah.nomor != 1 && surah.nomor != 9) {
                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = AmiriQuranFontFamily,
                        fontSize = 24.sp,
                        lineHeight = 36.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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

    val cardBgColor by animateColorAsState(
        targetValue = when {
            isAudioActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            isLastRead -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "ayatCardBg"
    )

    val borderStroke = when {
        isAudioActive -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        isLastRead -> BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ayat_card_${ayat.nomorAyat}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top bar of verse card: Verse badge, status chips, quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VerseNumberBadge(
                        number = ayat.nomorAyat,
                        size = 38.dp,
                        isHighlighted = isAudioActive || isLastRead
                    )

                    if (isLastRead) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Terakhir Dibaca",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Play Audio Verse Button
                    IconButton(
                        onClick = onPlayAudio,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("play_ayat_button_${ayat.nomorAyat}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAudioActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Putar Audio Ayat ${ayat.nomorAyat}",
                                tint = if (isAudioActive) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Bookmark Verse Button
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("bookmark_ayat_button_${ayat.nomorAyat}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Simpan Bookmark Ayat ${ayat.nomorAyat}",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mark as Last Read Button
                    IconButton(
                        onClick = onMarkLastRead,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("lastread_ayat_button_${ayat.nomorAyat}")
                    ) {
                        Icon(
                            imageVector = if (isLastRead) Icons.Default.CheckCircle else Icons.Outlined.Flag,
                            contentDescription = "Tandai Terakhir Dibaca Ayat ${ayat.nomorAyat}",
                            tint = if (isLastRead) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Copy Verse Button
                    IconButton(
                        onClick = {
                            val copied = buildString {
                                appendLine("QS. ${surah.namaLatin} (${surah.nomor}): ${ayat.nomorAyat}")
                                appendLine(ayat.teksArab)
                                if (ayat.teksLatin.isNotBlank()) appendLine(ayat.teksLatin)
                                append("\"${ayat.teksIndonesia}\"")
                            }
                            clipboard.setText(AnnotatedString(copied))
                            onCopyMessage("QS. ${surah.namaLatin}: ${ayat.nomorAyat} disalin")
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("copy_ayat_button_${ayat.nomorAyat}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Salin Ayat ${ayat.nomorAyat}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Arabic Uthmani Text (Rata Kanan)
            Text(
                text = ayat.teksArab,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = AmiriQuranFontFamily,
                    fontSize = readerSettings.arabicFontSizeSp.sp,
                    lineHeight = (readerSettings.arabicFontSizeSp * 2.0f).sp,
                    textDirection = TextDirection.Rtl
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // Transliteration Latin (Rata Kiri)
            if (readerSettings.showLatin && ayat.teksLatin.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = ayat.teksLatin,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = (readerSettings.translationFontSizeSp - 1).coerceAtLeast(12).sp,
                        lineHeight = ((readerSettings.translationFontSizeSp - 1) * 1.55f).sp,
                        textDirection = TextDirection.Ltr
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Indonesian Translation (Rata Kiri)
            if (readerSettings.showTranslation && ayat.teksIndonesia.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ayat.teksIndonesia,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = readerSettings.translationFontSizeSp.sp,
                        lineHeight = (readerSettings.translationFontSizeSp * 1.6f).sp,
                        textDirection = TextDirection.Ltr
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom action bar: Open Kemenag RI Tafsir Sheet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onOpenTafsir,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("tafsir_ayat_button_${ayat.nomorAyat}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Baca Tafsir Kemenag RI",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
