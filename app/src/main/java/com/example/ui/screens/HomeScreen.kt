package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.KhatamLogEntity
import com.example.data.local.LastReadState
import com.example.data.local.ReaderSettingsState
import com.example.data.local.SurahEntity
import com.example.ui.SurahFilterCategory
import com.example.ui.components.VerseNumberBadge
import com.example.ui.theme.AmiriQuranFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    surahs: List<SurahEntity>,
    searchQuery: String,
    selectedFilter: SurahFilterCategory,
    lastRead: LastReadState,
    readerSettings: ReaderSettingsState,
    khatamLogs: List<KhatamLogEntity>,
    isSyncing: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (SurahFilterCategory) -> Unit,
    onRefreshSurahs: () -> Unit,
    onOpenSurah: (Int, Int?) -> Unit,
    onNavigateToKhatamTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todayLog = khatamLogs.find { it.dateKey == todayKey }
    val isChapterMode = readerSettings.khatamTargetMode == com.example.data.local.KhatamTargetMode.CHAPTERS
    val todayCompletedUnits = if (isChapterMode) (todayLog?.surahCountRead ?: 0) else (todayLog?.ayatCountRead ?: 0)
    val dailyTargetUnits = if (isChapterMode) readerSettings.dailyKhatamTargetSurah else readerSettings.dailyKhatamTargetAyat
    val unitLabel = if (isChapterMode) "surah" else "ayat"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("surah_list_column"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NurQuran",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Al-Qur'an Digital & Tafsir Kemenag RI",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onRefreshSurahs,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("refresh_surah_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sinkronkan Daftar Surah",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Hero Card: Terakhir Dibaca + Khatam Daily Pill
            item {
                LastReadHeroCard(
                    lastRead = lastRead,
                    todayCompletedUnits = todayCompletedUnits,
                    dailyTargetUnits = dailyTargetUnits,
                    unitLabel = unitLabel,
                    onContinueReading = {
                        onOpenSurah(lastRead.surahNomor, lastRead.ayatNomor)
                    },
                    onOpenKhatamTracker = onNavigateToKhatamTab
                )
            }

            // Search Bar & Category Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("surah_filter_input"),
                        placeholder = {
                            Text("Cari nama surah, arti, atau nomor (1-114)…")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.testTag("clear_surah_filter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Hapus Pencarian"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SurahFilterCategory.entries.forEach { category ->
                            val selected = selectedFilter == category
                            FilterChip(
                                selected = selected,
                                onClick = { onFilterChange(category) },
                                label = { Text(category.label) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Empty State if no surahs match filter
            if (surahs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Surah tidak ditemukan",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Coba kata kunci lain atau pilih kategori filter 'Semua'.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = surahs,
                    key = { it.nomor }
                ) { surah ->
                    SurahItemCard(
                        surah = surah,
                        onClick = { onOpenSurah(surah.nomor, null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LastReadHeroCard(
    lastRead: LastReadState,
    todayCompletedUnits: Int,
    dailyTargetUnits: Int,
    unitLabel: String,
    onContinueReading: () -> Unit,
    onOpenKhatamTracker: () -> Unit
) {
    val gradientColors = listOf(
        Color(0xFF0284C7),
        Color(0xFF0369A1),
        Color(0xFF0F172A)
    )
    val dailyFraction = (todayCompletedUnits.toFloat() / dailyTargetUnits.coerceAtLeast(1)).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("last_read_hero_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .drawBehind {
                    // Decorative minimalist geometric circles in background
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = size.minDimension * 0.65f,
                        center = Offset(size.width * 0.9f, size.height * 0.2f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.minDimension * 0.4f,
                        center = Offset(size.width * 0.85f, size.height * 0.85f)
                    )
                }
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "TERAKHIR DIBACA",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = Color(0xFFE0F2FE)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "QS. ${lastRead.surahNamaLatin}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Ayat ${lastRead.ayatNomor} dari ${lastRead.totalAyatSurah} • ${lastRead.surahArti}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = lastRead.surahNamaArab,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 30.sp,
                            lineHeight = 38.sp
                        ),
                        color = Color.White.copy(alpha = 0.95f),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Daily Khatam mini progress pill
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenKhatamTracker)
                            .testTag("hero_khatam_pill")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.TrackChanges,
                                        contentDescription = null,
                                        tint = Color(0xFF5EEAD4),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Target Harian",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Text(
                                    text = "$todayCompletedUnits/$dailyTargetUnits $unitLabel",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF5EEAD4)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { dailyFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF2DD4BF),
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onContinueReading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0369A1)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("continue_last_read_button")
                    ) {
                        Text(
                            text = "Lanjut",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahItemCard(
    surah: SurahEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("surah_card_${surah.nomor}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VerseNumberBadge(
                number = surah.nomor,
                size = 44.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = surah.namaLatin,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (surah.isCachedOffline) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDone,
                            contentDescription = "Tersimpan Offline",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${surah.arti} • ${surah.tempatTurun} • ${surah.jumlahAyat} Ayat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = surah.namaArab,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = AmiriQuranFontFamily,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
