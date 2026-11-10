package com.irengcloud.quran.ui.screens

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irengcloud.quran.R
import com.irengcloud.quran.data.SavedMurottal
import com.irengcloud.quran.data.local.AyatEntity
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.data.local.SurahEntity
import com.irengcloud.quran.ui.AudioPlaybackState
import com.irengcloud.quran.ui.MurottalAudioController
import com.irengcloud.quran.ui.MurottalDownloadsState
import com.irengcloud.quran.ui.QuranViewModel
import com.irengcloud.quran.ui.SurahDetailUiState
import com.irengcloud.quran.ui.components.AppArtwork
import com.irengcloud.quran.ui.components.SettingsSwitch
import com.irengcloud.quran.ui.components.TransparentArtworkPaper
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

private val MurottalIndigo = Color(0xFF202034)
private val MurottalIvory = Color(0xFFFFF2E5)
private val MurottalGold = Color(0xFFE4B789)
private val PlayGradient = Brush.verticalGradient(listOf(Color(0xFFD1976C), Color(0xFF925737)))
private val CoverArch = GenericShape { size, _ ->
    fun x(fraction: Float) = size.width * fraction
    fun y(fraction: Float) = size.height * fraction
    moveTo(x(0.5f), 0f)
    cubicTo(x(0.45f), y(0.12f), x(0.14f), y(0.13f), x(0.10f), y(0.36f))
    cubicTo(x(0.02f), y(0.40f), 0f, y(0.45f), 0f, y(0.57f))
    lineTo(0f, y(0.88f))
    quadraticTo(0f, y(1f), x(0.10f), y(1f))
    lineTo(x(0.90f), y(1f))
    quadraticTo(x(1f), y(1f), x(1f), y(0.88f))
    lineTo(x(1f), y(0.57f))
    cubicTo(x(1f), y(0.45f), x(0.98f), y(0.40f), x(0.90f), y(0.36f))
    cubicTo(x(0.86f), y(0.13f), x(0.55f), y(0.12f), x(0.5f), 0f)
    close()
}

@Composable
fun MurottalScreen(
    detail: SurahDetailUiState,
    surah: SurahEntity,
    initialVerse: Int,
    settings: ReaderSettingsState,
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val controller = viewModel.audioController
    val playback by viewModel.audioState.collectAsStateWithLifecycle()
    val downloads by viewModel.murottalDownloads.collectAsStateWithLifecycle()
    var showDownloads by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showDownloads) { showDownloads = false }

    val current = playback.takeIf { it.isActive && it.surahNomor == surah.nomor }
    val currentVerse = current?.currentAyatNomor ?: initialVerse
    val queue = remember(surah.nomor, surah.jumlahAyat, detail.ayats) {
        detail.ayats.ifEmpty {
            (1..surah.jumlahAyat).map { verse ->
                AyatEntity(surah.nomor, verse, "", "", "")
            }
        }
    }
    val ranges = remember(queue) { queue.chunked(10) }
    val playAt: (Int) -> Unit = { viewModel.playMurottal(surah, queue, it) }
    val onPlayPause = {
        if (current != null) controller.togglePlayPause() else playAt(initialVerse)
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val density = LocalDensity.current
        val scale = (maxWidth / 380.dp).coerceIn(1f, 1.26f)
        CompositionLocalProvider(
            LocalDensity provides Density(density.density * scale, density.fontScale),
            LocalContentColor provides MaterialTheme.colorScheme.onBackground
        ) {
            val page = Modifier.widthIn(max = 640.dp).fillMaxSize()
            if (showDownloads) {
                MurottalDownloadsPage(
                    surah, downloads, viewModel,
                    onBack = { showDownloads = false }, modifier = page
                )
            } else {
                LazyColumn(page.testTag("murottal_screen"), contentPadding = PaddingValues(bottom = 22.dp)) {
                    item(key = "cover") { MurottalCover(surah, onBack) }
                    item(key = "controls") {
                        MurottalControls(
                            playback = playback,
                            isCurrentSurah = current != null,
                            currentVerse = currentVerse,
                            ayats = queue,
                            controller = controller,
                            onPlayPause = onPlayPause,
                            onPlayVerse = playAt
                        )
                    }
                    item(key = "source") {
                        val saved = downloads.saved[surah.nomor]?.verseCount ?: 0
                        val isComplete = saved >= surah.jumlahAyat
                        Surface(
                            onClick = { showDownloads = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 4.dp)
                                .testTag("open_murottal_downloads")
                                .semantics { contentDescription = "Kelola unduhan murottal" },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Outlined.PersonOutline, null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f)) {
                                    Text("Qari’ (Pembaca)", style = MaterialTheme.typography.titleLarge)
                                    Text("Misyari Rasyid Al-‘Afasy", style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(
                                    if (isComplete) Icons.Outlined.OfflinePin else Icons.Outlined.Download,
                                    if (isComplete) "Tersimpan offline" else null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    item(key = "verse_heading") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 18.dp, end = 17.dp, top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Daftar Ayat", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                            Text("Auto Play", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(8.dp))
                            SettingsSwitch(
                                settings.autoNextAudio, viewModel::updateAutoNextAudio, "Auto Play",
                                Modifier.testTag("murottal_auto_play")
                            )
                        }
                    }
                    if (ranges.isEmpty()) item(key = "empty") {
                        Text(
                            if (detail.isLoading) "Memuat daftar ayat…" else "Daftar ayat belum tersedia.",
                            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    items(ranges, key = { it.first().nomorAyat }) { verses ->
                        val active = currentVerse in verses.first().nomorAyat..verses.last().nomorAyat
                        MurottalVerseRange(
                            verses, active, active && current?.isPlaying == true,
                            onClick = {
                                if (active && current != null) onPlayPause() else playAt(verses.first().nomorAyat)
                            },
                            onPlayVerse = playAt
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MurottalCover(surah: SurahEntity, onBack: () -> Unit) {
    val paper = MaterialTheme.colorScheme.background
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val coverWidth = (maxWidth * 0.70f).coerceAtMost(290.dp)
        val coverHeight = coverWidth * 0.86f
        val coverTop = 139.dp
        Spacer(Modifier.fillMaxWidth().height(coverTop + coverHeight + 6.dp))
        Box(Modifier.fillMaxWidth().height(185.dp).background(MurottalIndigo)) {
            AppArtwork(R.drawable.murottal_header, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
            Column(
                Modifier.align(Alignment.TopCenter).padding(top = 13.dp, start = 45.dp, end = 45.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.Headphones, null, Modifier.size(34.dp), tint = MurottalGold)
                Text("Murottal", style = MaterialTheme.typography.headlineMedium, color = MurottalIvory)
                Text(
                    "Dengarkan lantunan ayat\nAl-Qur’an yang menenangkan",
                    Modifier.padding(top = 3.dp), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                    color = MurottalIvory
                )
            }
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = MurottalIvory)
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(coverTop + coverHeight + 6.dp - 159.dp)
                .background(paper, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        )
        AppArtwork(
            R.drawable.home_lace,
            Modifier.align(Alignment.BottomEnd).size(114.dp, 86.dp).padding(bottom = 7.dp),
            colorFilter = TransparentArtworkPaper, alpha = 0.31f, targetWidth = 114.dp
        )
        AppArtwork(
            R.drawable.home_lace,
            Modifier.align(Alignment.BottomStart).size(114.dp, 86.dp).padding(bottom = 7.dp)
                .graphicsLayer { scaleX = -1f },
            colorFilter = TransparentArtworkPaper, alpha = 0.31f, targetWidth = 114.dp
        )
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = coverTop).size(coverWidth, coverHeight)
                .shadow(3.dp, CoverArch).clip(CoverArch).background(MurottalGold).padding(4.dp)
                .clip(CoverArch).background(MurottalIndigo)
        ) {
            AppArtwork(
                R.drawable.murottal_cover, Modifier.matchParentSize(), targetWidth = coverWidth,
                contentScale = ContentScale.Crop
            )
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, MurottalIndigo.copy(alpha = 0.9f))))
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 13.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(surah.namaLatin, style = MaterialTheme.typography.headlineMedium,
                    color = MurottalIvory, textAlign = TextAlign.Center)
                Text("Surah ke-${surah.nomor} · ${surah.jumlahAyat} Ayat",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                    color = MurottalIvory, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun MurottalControls(
    playback: AudioPlaybackState,
    isCurrentSurah: Boolean,
    currentVerse: Int,
    ayats: List<AyatEntity>,
    controller: MurottalAudioController,
    onPlayPause: () -> Unit,
    onPlayVerse: (Int) -> Unit
) {
    val progress by controller.progress.collectAsStateWithLifecycle()
    val playing = isCurrentSurah && playback.isPlaying
    val buffering = isCurrentSurah && playback.isBuffering
    val duration = if (isCurrentSurah) progress.durationMs else 0
    val position = if (isCurrentSurah) progress.positionMs else 0
    var dragPosition by remember(currentVerse, duration) { mutableStateOf<Float?>(null) }
    LaunchedEffect(playing, currentVerse) {
        if (isCurrentSurah) controller.refreshProgress()
        while (playing) {
            delay(250)
            controller.refreshProgress()
        }
    }

    val index = ayats.indexOfFirst { it.nomorAyat == currentVerse }.coerceAtLeast(0)
    val fullQueue = isCurrentSurah && playback.totalAyatInQueue == ayats.size
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(audioTime(dragPosition?.roundToInt() ?: position), style = MaterialTheme.typography.labelSmall)
            Slider(
                value = (dragPosition ?: position.toFloat()).coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                onValueChange = { dragPosition = it },
                onValueChangeFinished = {
                    dragPosition?.let { controller.seekTo(it.roundToInt()) }
                    dragPosition = null
                },
                valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                enabled = duration > 0 && !buffering,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                    .semantics { contentDescription = "Posisi audio ayat" }
            )
            Text(if (duration > 0) audioTime(duration) else "—:—", style = MaterialTheme.typography.labelSmall)
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconToggleButton(
                checked = playback.shuffleEnabled, onCheckedChange = controller::setShuffle,
                modifier = Modifier.testTag("murottal_shuffle")
            ) {
                Icon(Icons.Outlined.Shuffle, "Acak ayat",
                    tint = if (playback.shuffleEnabled) scheme.primary else scheme.onSurface)
            }
            IconButton(
                onClick = {
                    if (fullQueue) controller.playPreviousVerse() else ayats.getOrNull(index - 1)?.let { onPlayVerse(it.nomorAyat) }
                },
                enabled = if (fullQueue) playback.hasPrevious else index > 0
            ) { Icon(Icons.Filled.SkipPrevious, "Ayat sebelumnya") }
            IconButton(
                onClick = onPlayPause, enabled = ayats.isNotEmpty() && !buffering,
                modifier = Modifier.size(61.dp).shadow(4.dp, CircleShape).clip(CircleShape)
                    .background(PlayGradient).testTag("murottal_play")
            ) {
                if (buffering) CircularProgressIndicator(Modifier.size(24.dp), color = MurottalIvory, strokeWidth = 2.dp)
                else Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (playing) "Jeda" else "Putar", Modifier.size(34.dp), tint = MurottalIvory)
            }
            IconButton(
                onClick = {
                    if (fullQueue) controller.playNextVerse() else ayats.getOrNull(index + 1)?.let { onPlayVerse(it.nomorAyat) }
                },
                enabled = if (fullQueue) playback.hasNext else index + 1 < ayats.size
            ) { Icon(Icons.Filled.SkipNext, "Ayat berikutnya") }
            IconToggleButton(
                checked = playback.repeatVerse, onCheckedChange = controller::setRepeatVerse,
                modifier = Modifier.testTag("murottal_repeat")
            ) {
                Icon(
                    if (playback.repeatVerse) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                    "Ulang ayat", tint = if (playback.repeatVerse) scheme.primary else scheme.onSurface
                )
            }
        }
        if (isCurrentSurah) {
            val status = playback.errorMessage ?: if (buffering) "Memuat ayat $currentVerse…"
                else if (playback.usesLocalAudio) "Ayat $currentVerse · File lokal" else "Ayat $currentVerse"
            Text(status, Modifier.padding(bottom = 3.dp),
                color = if (playback.errorMessage != null) scheme.error else scheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MurottalVerseRange(
    ayats: List<AyatEntity>,
    active: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onPlayVerse: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val first = ayats.first().nomorAyat
    val last = ayats.last().nomorAyat
    val rangeLabel = if (first == last) "Ayat $first" else "Ayat $first – $last"
    val scheme = MaterialTheme.colorScheme
    val color = if (active) scheme.primaryContainer else scheme.surface
    val ink = if (active) scheme.onPrimaryContainer else scheme.onSurface
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 2.dp)
            .testTag("murottal_range_$first"),
        shape = RoundedCornerShape(13.dp),
        color = color, contentColor = ink,
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(27.dp).clip(CircleShape).background(
                    if (active) scheme.primary else Color.Transparent
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null,
                    Modifier.size(20.dp), tint = if (active) scheme.onPrimary else ink
                )
            }
            Text(rangeLabel, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.MoreVert, "Pilih ayat dari $rangeLabel", Modifier.size(19.dp))
                }
                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    ayats.forEach { ayat ->
                        DropdownMenuItem(
                            text = { Text("Putar ayat ${ayat.nomorAyat}") },
                            onClick = { expanded = false; onPlayVerse(ayat.nomorAyat) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MurottalDownloadsPage(
    currentSurah: SurahEntity,
    downloads: MurottalDownloadsState,
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSurahs by viewModel.allSurahs.collectAsStateWithLifecycle()
    val surahsByNumber = remember(allSurahs, currentSurah) {
        allSurahs.associateBy { it.nomor } + (currentSurah.nomor to currentSurah)
    }
    var deleteSurah by rememberSaveable { mutableStateOf<Int?>(null) }
    val busy = downloads.downloadingSurah != null || downloads.removingSurah != null

    LazyColumn(modifier.testTag("murottal_downloads"), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "downloads_header") {
            Box(Modifier.fillMaxWidth().heightIn(min = 96.dp).background(MurottalIndigo)) {
                AppArtwork(R.drawable.murottal_header, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                Text(
                    "Unduhan Murottal", Modifier.align(Alignment.Center).padding(horizontal = 55.dp),
                    style = MaterialTheme.typography.headlineMedium, color = MurottalIvory,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = MurottalIvory)
                }
            }
        }
        item(key = "storage") {
            val context = LocalContext.current
            val bytes = downloads.saved.values.sumOf { it.bytes }
            Text(
                "Penyimpanan perangkat · ${Formatter.formatShortFileSize(context, bytes)}",
                Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        item(key = "current_surah") {
            DownloadSurahRow(
                currentSurah.nomor, currentSurah, downloads.saved[currentSurah.nomor], downloads, busy,
                onDownload = { viewModel.downloadMurottal(currentSurah) },
                onCancel = viewModel::cancelMurottalDownload,
                onDelete = { deleteSurah = currentSurah.nomor }
            )
        }
        val others = (downloads.saved.keys + listOfNotNull(downloads.downloadingSurah))
            .filter { it != currentSurah.nomor }.sorted()
        items(others, key = { it }) { number ->
            val surah = surahsByNumber[number]
            DownloadSurahRow(
                number, surah, downloads.saved[number], downloads, busy,
                onDownload = { surah?.let(viewModel::downloadMurottal) },
                onCancel = viewModel::cancelMurottalDownload,
                onDelete = { deleteSurah = number }
            )
        }
    }

    deleteSurah?.let { number ->
        val title = surahsByNumber[number]?.namaLatin ?: "Surah $number"
        AlertDialog(
            onDismissRequest = { deleteSurah = null },
            title = { Text("Hapus audio $title?") },
            text = { Text("File murottal surah ini akan dihapus dari perangkat.") },
            dismissButton = { TextButton(onClick = { deleteSurah = null }) { Text("Batal") } },
            confirmButton = {
                TextButton(onClick = { deleteSurah = null; viewModel.removeMurottal(number) }) { Text("Hapus") }
            }
        )
    }
}

@Composable
private fun DownloadSurahRow(
    number: Int,
    surah: SurahEntity?,
    saved: SavedMurottal?,
    downloads: MurottalDownloadsState,
    busy: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val active = downloads.downloadingSurah == number
    val count = if (active) downloads.savedVerses else saved?.verseCount ?: 0
    val total = if (active) downloads.totalVerses else surah?.jumlahAyat ?: 0
    val complete = total > 0 && count >= total
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).testTag("murottal_download_$number"),
        shape = RoundedCornerShape(15.dp), color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (complete) Icons.Outlined.OfflinePin else Icons.Outlined.Download, null, tint = scheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(surah?.namaLatin ?: "Surah $number", style = MaterialTheme.typography.titleLarge)
                    val size = if (!active && saved != null) " · ${Formatter.formatShortFileSize(context, saved.bytes)}" else ""
                    Text(
                        when {
                            downloads.removingSurah == number -> "Menghapus audio…"
                            active -> "Mengunduh $count/$total ayat"
                            complete -> "$count ayat tersimpan$size"
                            count > 0 -> "$count/$total ayat tersimpan$size"
                            else -> "$total ayat"
                        },
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant
                    )
                }
                if (saved != null && !active) {
                    IconButton(onClick = onDelete, enabled = !busy) {
                        Icon(Icons.Outlined.DeleteOutline, "Hapus unduhan ${surah?.namaLatin ?: number}")
                    }
                }
            }
            if (active) {
                LinearProgressIndicator(progress = { (count.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth())
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) { Text("Batal") }
            } else if (!complete && surah != null) {
                Button(onClick = onDownload, enabled = !busy && total > 0,
                    modifier = Modifier.align(Alignment.End).testTag("download_surah_$number")) {
                    Text(if (count > 0) "Lanjutkan" else "Unduh")
                }
            }
        }
    }
}

private fun audioTime(ms: Int): String {
    val seconds = ms / 1000
    return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
}
