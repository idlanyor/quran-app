package com.irengcloud.quran.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irengcloud.quran.data.local.BookmarkEntity
import com.irengcloud.quran.data.local.KhatamLogEntity
import com.irengcloud.quran.data.local.KhatamTargetMode
import com.irengcloud.quran.data.local.LastReadState
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.ui.components.KhatamTrackerComponent
import com.irengcloud.quran.ui.components.TOTAL_QURAN_AYATS
import com.irengcloud.quran.ui.components.TOTAL_QURAN_SURAHS
import com.irengcloud.quran.ui.components.rememberTodayKey
import com.irengcloud.quran.ui.components.VerseNumberBadge
import com.irengcloud.quran.ui.theme.AmiriQuranFontFamily
import java.util.Locale
import kotlin.math.ceil

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
    val todayKey = rememberTodayKey()
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title, Description, and Filter Chips
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Khatam & Bookmark",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Pantau capaian target tilawah harian Anda menuju Khatam 30 Juz dan kelola ayat yang tersimpan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedSection == 0,
                            onClick = { selectedSection = 0 },
                            label = {
                                Text(
                                    text = "Target Khatam",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.TrackChanges,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedSection == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("section_chip_khatam")
                        )

                        FilterChip(
                            selected = selectedSection == 1,
                            onClick = { selectedSection = 1 },
                            label = {
                                Text(
                                    text = "Bookmark Ayat (${bookmarks.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedSection == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("section_chip_bookmarks")
                        )
                    }
                }
            }

            // ==================== SECTION 0: KHATAM TRACKER & OVERVIEW ====================
            if (selectedSection == 0) {
                // Hero Card: Ocean Blue Gradient Banner matching HomeScreen, PrayerScreen, & SettingsScreen
                item(key = "khatam_hero") {
                    KhatamHeroCard(
                        readerSettings = readerSettings,
                        todayReadAyats = todayReadAyats,
                        todayReadSurahs = todayReadSurahs,
                        lastRead = lastRead,
                        onContinueReading = {
                            onOpenSurahAtAyat(lastRead.surahNomor, lastRead.ayatNomor)
                        }
                    )
                }

                // Dedicated Khatam Tracker Component (Verses & Chapters daily target + progress indicators)
                item(key = "khatam_tracker_card") {
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
                    item(key = "khatam_history_card") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Riwayat Tilawah Terakhir",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Pencatatan harian membaca Al-Qur'an",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                khatamLogs.take(5).forEach { log ->
                                    val isChapterMode = readerSettings.khatamTargetMode == KhatamTargetMode.CHAPTERS
                                    val metTarget = if (isChapterMode) {
                                        log.surahCountRead >= readerSettings.dailyKhatamTargetSurah
                                    } else {
                                        log.ayatCountRead >= readerSettings.dailyKhatamTargetAyat
                                    }
                                    Surface(
                                        color = if (metTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (metTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                                    tint = if (metTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = if (log.dateKey == todayKey) "Hari Ini (${log.dateKey})" else log.dateKey,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Surface(
                                                color = if (metTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                val summaryText = if (log.surahCountRead > 0) {
                                                    "${log.surahCountRead} Surah • ${log.ayatCountRead} Ayat"
                                                } else {
                                                    "${log.ayatCountRead} Ayat"
                                                }
                                                Text(
                                                    text = summaryText,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (metTarget) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== SECTION 1: BOOKMARKS LIST ====================
            item(key = "bookmarks_section_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Ayat Tersimpan",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${bookmarks.size} Ayat",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (bookmarks.isEmpty()) {
                item(key = "empty_bookmarks") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "Belum Ada Ayat di-Bookmark",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ketuk ikon bookmark pada ayat mana pun saat membaca surah untuk menandai dan menyimpannya di sini.",
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

/**
 * Modern Hero Card matching the visual language of HomeScreen, PrayerScreen, and SettingsScreen.
 */
@Composable
private fun KhatamHeroCard(
    readerSettings: ReaderSettingsState,
    todayReadAyats: Int,
    todayReadSurahs: Int,
    lastRead: LastReadState,
    onContinueReading: () -> Unit
) {
    val gradientColors = listOf(
        Color(0xFF0284C7),
        Color(0xFF0369A1),
        Color(0xFF0F172A)
    )

    val isChapterMode = readerSettings.khatamTargetMode == KhatamTargetMode.CHAPTERS
    val totalCompletedUnits = if (isChapterMode) {
        readerSettings.totalKhatamSurahCompleted
    } else {
        readerSettings.totalKhatamAyatCompleted
    }
    val totalGoalUnits = if (isChapterMode) TOTAL_QURAN_SURAHS else TOTAL_QURAN_AYATS
    val unitLabel = readerSettings.khatamTargetMode.unitLabel
    val rawOverallProgress = (totalCompletedUnits.toFloat() / totalGoalUnits).coerceIn(0f, 1f)
    val percentageText = String.format(Locale.US, "%.1f%%", rawOverallProgress * 100f)

    val todayCompletedUnits = if (isChapterMode) todayReadSurahs else todayReadAyats
    val dailyTargetUnits = if (isChapterMode) {
        readerSettings.dailyKhatamTargetSurah.coerceAtLeast(1)
    } else {
        readerSettings.dailyKhatamTargetAyat.coerceAtLeast(1)
    }
    val remainingUnits = (totalGoalUnits - totalCompletedUnits).coerceAtLeast(0)
    val estimatedDaysLeft = if (dailyTargetUnits > 0) {
        ceil(remainingUnits.toDouble() / dailyTargetUnits.toDouble()).toInt()
    } else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = size.minDimension * 0.7f,
                        center = Offset(size.width * 0.9f, size.height * 0.15f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.minDimension * 0.45f,
                        center = Offset(size.width * 0.82f, size.height * 0.88f)
                    )
                }
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Top row: Pill badge & Arabic Calligraphy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "TARGET & PROGRES KHATAM",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = Color(0xFFE0F2FE)
                        )
                    }

                    Text(
                        text = "خَتْمُ الْقُرْآنِ",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 24.sp,
                            lineHeight = 30.sp
                        ),
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }

                // Middle: Progress Highlight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "$percentageText Khatam Selesai",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "$totalCompletedUnits dari $totalGoalUnits $unitLabel tercapai • ~$estimatedDaysLeft hari lagi",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE0F2FE)
                        )
                    }
                }

                // Bottom summary pills & CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Target ${dailyTargetUnits} ${unitLabel.lowercase()}/hari",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Hari ini: $todayCompletedUnits $unitLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Quick Action
                Button(
                    onClick = onContinueReading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0369A1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lanjut Baca QS. ${lastRead.surahNamaLatin}:${lastRead.ayatNomor}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_bookmark_${bookmark.surahNomor}_${bookmark.nomorAyat}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Bookmark",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Buka Ayat",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Arabic verse
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

            // Indonesian translation
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
