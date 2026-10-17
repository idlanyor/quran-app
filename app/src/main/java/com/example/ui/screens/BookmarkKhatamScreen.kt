package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookmarkEntity
import com.example.data.local.KhatamLogEntity
import com.example.data.local.KhatamTargetMode
import com.example.data.local.LastReadState
import com.example.data.local.ReaderSettingsState
import com.example.ui.components.KhatamTrackerComponent
import com.example.ui.components.VerseNumberBadge
import com.example.ui.theme.AmiriQuranFontFamily
import java.text.SimpleDateFormat
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
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Khatam & Semua, 1 = Hanya Bookmark
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todayLog = khatamLogs.find { it.dateKey == todayKey }
    val todayReadAyats = todayLog?.ayatCountRead ?: 0
    val todayReadSurahs = todayLog?.surahCountRead ?: 0

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("bookmark_khatam_lazy_column"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Khatam Tracker & Bookmark",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Atur target ayat atau surah harian Anda, pantau indikator progres Khatam, dan kelola ayat tersimpan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedSection == 0,
                            onClick = { selectedSection = 0 },
                            label = { Text("Khatam & Semua") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.TrackChanges,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("section_chip_khatam")
                        )

                        FilterChip(
                            selected = selectedSection == 1,
                            onClick = { selectedSection = 1 },
                            label = { Text("Bookmark Ayat (${bookmarks.size})") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("section_chip_bookmarks")
                        )
                    }
                }
            }

            if (selectedSection == 0) {
                // Dedicated Khatam Tracker Component (Verses & Chapters daily target + progress indicators)
                item {
                    KhatamTrackerComponent(
                        readerSettings = readerSettings,
                        todayReadAyats = todayReadAyats,
                        todayReadSurahs = todayReadSurahs,
                        lastRead = lastRead,
                        onTargetModeChange = onTargetModeChange,
                        onUpdateDailyTargetAyat = onUpdateDailyTargetAyat,
                        onUpdateDailyTargetSurah = onUpdateDailyTargetSurah,
                        onLogCustomAyats = onLogCustomAyats,
                        onLogCustomSurahs = onLogCustomSurahs,
                        onResetKhatam = onResetKhatam,
                        onContinueReading = {
                            onOpenSurahAtAyat(lastRead.surahNomor, lastRead.ayatNomor)
                        }
                    )
                }

                // Recent Daily Reading History
                if (khatamLogs.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Riwayat Tilawah Harian",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                khatamLogs.take(5).forEach { log ->
                                    val isChapterMode = readerSettings.khatamTargetMode == KhatamTargetMode.CHAPTERS
                                    val metTarget = if (isChapterMode) {
                                        log.surahCountRead >= readerSettings.dailyKhatamTargetSurah
                                    } else {
                                        log.ayatCountRead >= readerSettings.dailyKhatamTargetAyat
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (metTarget) MaterialTheme.colorScheme.secondary
                                                else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (log.dateKey == todayKey) "Hari Ini (${log.dateKey})" else log.dateKey,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Surface(
                                            color = if (metTarget) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            val summaryText = if (log.surahCountRead > 0) {
                                                "${log.surahCountRead} Surah • ${log.ayatCountRead} Ayat"
                                            } else {
                                                "${log.ayatCountRead} Ayat"
                                            }
                                            Text(
                                                text = summaryText,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (metTarget) MaterialTheme.colorScheme.onSecondaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bookmarks Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Ayat Tersimpan (${bookmarks.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (bookmarks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Belum Ada Ayat di-Bookmark",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ketuk ikon pita (Bookmark) pada ayat mana pun saat membaca surah untuk menyimpannya di sini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = bookmarks,
                    key = { "${it.surahNomor}_${it.nomorAyat}" }
                ) { bookmark ->
                    BookmarkItemCard(
                        bookmark = bookmark,
                        readerSettings = readerSettings,
                        onOpen = {
                            onOpenSurahAtAyat(bookmark.surahNomor, bookmark.nomorAyat)
                        },
                        onDelete = {
                            onRemoveBookmark(bookmark.surahNomor, bookmark.nomorAyat)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkItemCard(
    bookmark: BookmarkEntity,
    readerSettings: ReaderSettingsState,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("bookmark_card_${bookmark.surahNomor}_${bookmark.nomorAyat}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VerseNumberBadge(number = bookmark.nomorAyat, size = 36.dp, isHighlighted = true)
                    Column {
                        Text(
                            text = "QS. ${bookmark.surahNamaLatin} (${bookmark.surahNomor}) : Ayat ${bookmark.nomorAyat}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = bookmark.surahNamaArab,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_bookmark_${bookmark.surahNomor}_${bookmark.nomorAyat}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Bookmark",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Buka Ayat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = bookmark.teksArab,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = AmiriQuranFontFamily,
                    fontSize = (readerSettings.arabicFontSizeSp - 2).coerceAtLeast(22).sp,
                    lineHeight = ((readerSettings.arabicFontSizeSp - 2) * 1.95f).sp,
                    textDirection = TextDirection.Rtl
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = bookmark.teksIndonesia,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = readerSettings.translationFontSizeSp.sp,
                    lineHeight = (readerSettings.translationFontSizeSp * 1.55f).sp,
                    textDirection = TextDirection.Ltr
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
