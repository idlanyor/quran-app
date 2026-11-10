package com.irengcloud.quran.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irengcloud.quran.R
import com.irengcloud.quran.data.local.*
import com.irengcloud.quran.ui.components.AppArtwork
import com.irengcloud.quran.ui.components.KhatamTrackerComponent
import com.irengcloud.quran.ui.components.TOTAL_QURAN_AYATS
import com.irengcloud.quran.ui.components.TOTAL_QURAN_SURAHS
import com.irengcloud.quran.ui.components.rememberTodayKey
import com.irengcloud.quran.ui.theme.AmiriQuranFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BookmarkKhatamScreen(
    bookmarks: List<BookmarkEntity>,
    khatamLogs: List<KhatamLogEntity>,
    lastRead: LastReadState,
    readerSettings: ReaderSettingsState,
    onOpenSurahAtAyat: (Int, Int) -> Unit,
    onRemoveBookmark: (Int, Int) -> Unit,
    onTargetModeChange: (KhatamTargetMode) -> Unit,
    onUpdateDailyTargetAyat: (Int) -> Unit,
    onUpdateDailyTargetSurah: (Int) -> Unit,
    onLogCustomAyats: (Int) -> Unit,
    onLogCustomSurahs: (Int) -> Unit,
    onResetKhatam: () -> Unit,
    selectedSection: Int = 1,
    onSectionChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val todayKey = rememberTodayKey()
    val todayLog = remember(khatamLogs, todayKey) { khatamLogs.find { it.dateKey == todayKey } }
    var pendingRemoval by remember { mutableStateOf<BookmarkEntity?>(null) }
    var preview by remember { mutableStateOf<BookmarkEntity?>(null) }
    val locale = remember { Locale.forLanguageTag("id-ID") }
    val shortDate = remember(locale) { SimpleDateFormat("dd MMM", locale) }
    val dayKey = remember(locale) { SimpleDateFormat("yyyy-MM-dd", locale) }
    val yesterday = remember(todayKey) {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time.let(dayKey::format)
    }
    pendingRemoval?.let { bookmark ->
        AlertDialog(onDismissRequest = { pendingRemoval = null },
            title = { Text("Hapus penanda?") },
            text = { Text("Penanda ${bookmark.surahNamaLatin} ayat ${bookmark.nomorAyat} akan dihapus.") },
            confirmButton = { TextButton(onClick = {
                onRemoveBookmark(bookmark.surahNomor, bookmark.nomorAyat); pendingRemoval = null
            }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Batal") } })
    }
    preview?.let { bookmark ->
        AlertDialog(onDismissRequest = { preview = null },
            title = { Text("${bookmark.surahNamaLatin} · Ayat ${bookmark.nomorAyat}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(bookmark.teksArab, fontFamily = AmiriQuranFontFamily,
                        fontSize = readerSettings.arabicFontSizeSp.sp,
                        lineHeight = (readerSettings.arabicFontSizeSp * 1.8f).sp,
                        textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    Text(bookmark.teksIndonesia, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif))
                }
            },
            confirmButton = { TextButton(onClick = {
                preview = null; onOpenSurahAtAyat(bookmark.surahNomor, bookmark.nomorAyat)
            }) { Text("Buka surah") } },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("Tutup") } })
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("bookmark_khatam_lazy_column"),
            contentPadding = PaddingValues(bottom = 110.dp)) {
            item(key = "header", contentType = "header") {
                Box(Modifier.fillMaxWidth().height(148.dp).background(Color(0xFF252439))) {
                    AppArtwork(R.drawable.bookmarks_header, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    Column(Modifier.align(Alignment.Center).padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = Color(0xFFE0B48A), modifier = Modifier.size(30.dp))
                        Text("Penanda & Khatam", style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFFFF1E3))
                    }
                }
            }
            item(key = "sections", contentType = "sections") {
                Box(Modifier.fillMaxWidth().background(Color(0xFF252439))) {
                    Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation = 2.dp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                            Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                BookmarkSectionTab("Penanda", selectedSection == 1, true, Modifier.weight(1f)) { onSectionChange(1) }
                                BookmarkSectionTab("Khatam", selectedSection == 0, false, Modifier.weight(1f)) { onSectionChange(0) }
                            }
                        }
                    }
                }
            }
            item(key = "progress", contentType = "progress") {
                KhatamSummaryCard(readerSettings, Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { onSectionChange(0) }
            }
            if (selectedSection == 0) {
                item(key = "khatam_tracker", contentType = "tracker") {
                    KhatamTrackerComponent(readerSettings, todayLog?.ayatCountRead ?: 0,
                        todayLog?.surahCountRead ?: 0, lastRead, onTargetModeChange,
                        onUpdateDailyTargetAyat, onUpdateDailyTargetSurah, onLogCustomAyats,
                        onLogCustomSurahs, onResetKhatam,
                        onContinueReading = { onOpenSurahAtAyat(lastRead.surahNomor, lastRead.ayatNomor) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
                if (khatamLogs.isNotEmpty()) {
                    item(key = "history", contentType = "history") {
                        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Riwayat Tilawah", style = MaterialTheme.typography.titleLarge)
                                khatamLogs.take(7).forEach { log ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (log.dateKey == todayKey) "Hari ini" else log.dateKey,
                                            style = MaterialTheme.typography.bodyMedium)
                                        Text("${log.surahCountRead} surah · ${log.ayatCountRead} ayat",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item(key = "bookmarks_heading", contentType = "heading") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 17.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Penanda (Bookmark)", style = MaterialTheme.typography.titleLarge)
                        Text("${bookmarks.size} ayat", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (bookmarks.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.padding(horizontal = 20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.BookmarkBorder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                                Text("Belum ada penanda", style = MaterialTheme.typography.titleLarge)
                                Text("Simpan ayat saat membaca surah. Semua penandamu akan muncul di sini.",
                                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { onOpenSurahAtAyat(lastRead.surahNomor, lastRead.ayatNomor) }) {
                                    Text("Lanjut membaca")
                                }
                            }
                        }
                    }
                } else {
                    items(bookmarks, key = { "${it.surahNomor}_${it.nomorAyat}" }, contentType = { "bookmark" }) { bookmark ->
                        val label = remember(bookmark.timestamp, todayKey) {
                            val date = Date(bookmark.timestamp)
                            when (dayKey.format(date)) {
                                todayKey -> "Hari ini"
                                yesterday -> "Kemarin"
                                else -> shortDate.format(date)
                            }
                        }
                        BookmarkItemCard(bookmark, label,
                            onOpen = { onOpenSurahAtAyat(bookmark.surahNomor, bookmark.nomorAyat) },
                            onPreview = { preview = bookmark }, onDelete = { pendingRemoval = bookmark })
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkSectionTab(label: String, selected: Boolean, bookmark: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    val copper = Brush.verticalGradient(listOf(Color(0xFFBA805B), Color(0xFF8B5035)))
    Row(modifier.height(48.dp).clip(RoundedCornerShape(10.dp))
        .then(if (selected) Modifier.background(copper) else Modifier)
        .clickable(onClick = onClick).testTag(if (bookmark) "section_chip_bookmarks" else "section_chip_khatam"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        val tint = if (selected) Color(0xFFFFF6ED) else MaterialTheme.colorScheme.onSurface
        Icon(if (bookmark) Icons.Outlined.BookmarkBorder else Icons.Outlined.TrackChanges, null,
            tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, color = tint, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun KhatamSummaryCard(settings: ReaderSettingsState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val chapterMode = settings.khatamTargetMode == KhatamTargetMode.CHAPTERS
    val completed = if (chapterMode) settings.totalKhatamSurahCompleted else settings.totalKhatamAyatCompleted
    val total = if (chapterMode) TOTAL_QURAN_SURAHS else TOTAL_QURAN_AYATS
    val unit = if (chapterMode) "surah" else "ayat"
    val progress = (completed.toFloat() / total).coerceIn(0f, 1f)
    val percent = "${(progress * 100).toInt()}%"
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = Color(0xFF252439),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = "Progres khatam $percent. $completed dari $total $unit." }
            .testTag("khatam_summary_card")) {
        Box(Modifier.fillMaxWidth().height(196.dp)) {
            AppArtwork(R.drawable.khatam_arch, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Progres Khatam", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFFFF4E8))
                Text("Target 1 kali khatam", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                    color = Color(0xFFF1DFCD))
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(0.73f), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(),
                            color = Color(0xFFD7A47B), trackColor = Color(0xFF696A7B), strokeWidth = 7.dp, strokeCap = StrokeCap.Round)
                        Text(percent, style = MaterialTheme.typography.headlineMedium, color = Color(0xFFFFF4E8))
                    }
                    Text("$completed dari $total $unit\nsudah dibaca", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                        color = Color(0xFFFFF1E2))
                }
            }
        }
    }
}

@Composable
private fun BookmarkItemCard(bookmark: BookmarkEntity, dateLabel: String,
    onOpen: () -> Unit, onPreview: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Surface(onClick = onOpen, shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)), shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)
            .testTag("bookmark_card_${bookmark.surahNomor}_${bookmark.nomorAyat}")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(start = 12.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(Icons.Filled.Bookmark, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp).size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(bookmark.surahNamaLatin, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Surah ke-${bookmark.surahNomor} · Ayat ${bookmark.nomorAyat}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.MoreVert, "Menu penanda ${bookmark.surahNamaLatin} ayat ${bookmark.nomorAyat}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
                }
                DropdownMenu(showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Buka ayat") }, onClick = { showMenu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Lihat rincian") }, onClick = { showMenu = false; onPreview() })
                    DropdownMenuItem(text = { Text("Hapus penanda") }, onClick = { showMenu = false; onDelete() },
                        modifier = Modifier.testTag("delete_bookmark_${bookmark.surahNomor}_${bookmark.nomorAyat}"))
                }
            }
        }
    }
}
