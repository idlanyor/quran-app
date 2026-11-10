package com.irengcloud.quran.ui.screens

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Notifications
import com.irengcloud.quran.R
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irengcloud.quran.data.local.KhatamLogEntity
import com.irengcloud.quran.data.local.LastReadState
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.data.local.SurahEntity
import com.irengcloud.quran.ui.SurahFilterCategory
import com.irengcloud.quran.ui.components.rememberTodayKey
import com.irengcloud.quran.ui.components.VerseNumberBadge
import com.irengcloud.quran.ui.components.AppArtwork
import com.irengcloud.quran.ui.components.TransparentArtworkPaper
import com.irengcloud.quran.ui.components.rememberArtwork
import com.irengcloud.quran.ui.theme.AmiriQuranFontFamily

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
    onNavigateToPrayer: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBookmarks: () -> Unit = onNavigateToKhatamTab,
    modifier: Modifier = Modifier
) {
    // Prepare the reader decoration after the home screen's own artwork loads.
    rememberArtwork(R.drawable.reader_top, delayMillis = 400)
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
                start = 0.dp,
                end = 0.dp,
                top = 0.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item(key = "home_mosque_header", contentType = "mosque_header") {
                Box(Modifier.fillMaxWidth().height(198.dp).background(Color(0xFF202034))) {
                    AppArtwork(R.drawable.home_mosque_v2,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Column(Modifier.padding(start = 24.dp, top = 26.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(38.dp))
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge,
                                color = Color(0xFFFFF4E6))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.app_tagline), color = Color(0xFFF1E5DB),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                        Icon(Icons.Outlined.Notifications, "Pengaturan pengingat", tint = Color(0xFFF3DFCC))
                    }
                }
            }
            item(key = "home_welcome", contentType = "welcome") {
                Box(Modifier.fillMaxWidth()
                    .background(Color(0xFF202034))
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(MaterialTheme.colorScheme.background)
                    ) {
                    AppArtwork(R.drawable.home_lace, targetWidth = 145.dp,
                        contentScale = ContentScale.Fit,
                        alpha = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.8f else 0.58f,
                        colorFilter = TransparentArtworkPaper,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp).width(145.dp).height(104.dp))
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {

                    Text("Assalamu’alaikum", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(5.dp))
                    Text("Mari mendekat kepada Al-Qur’an,\nmembaca, memahami, dan\nmengamalkannya.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif))
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        HomeShortcut("Baca\nAl-Qur’an", Icons.AutoMirrored.Outlined.MenuBook, Modifier.weight(1f)) {
                            onOpenSurah(lastRead.surahNomor, lastRead.ayatNomor)
                        }
                        HomeShortcut("Penanda", Icons.Outlined.BookmarkBorder, Modifier.weight(1f), onNavigateToBookmarks)
                        HomeShortcut("Khatam", Icons.Outlined.TrackChanges, Modifier.weight(1f), onNavigateToKhatamTab)
                        HomeShortcut("Jadwal\nSholat", Icons.Outlined.DateRange, Modifier.weight(1f), onNavigateToPrayer)
                    }
                }
            }
            }

            // Search Bar & Category Filters
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daftar Surah", style = MaterialTheme.typography.titleLarge)
                        Text("114 Surah", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BasicTextField(
                        value = searchQuery, onValueChange = onSearchQueryChange, singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("surah_filter_input")
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(15.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(15.dp)),
                        decorationBox = { input ->
                            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(10.dp))
                                Box(Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) Text("Cari surah, nomor, atau arti…", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    input()
                                }
                                if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, "Hapus pencarian", Modifier.size(18.dp))
                                }
                            }
                        }
                    )

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
                    key = { it.nomor },
                    contentType = { "surah" }
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
private fun SurahItemCard(
    surah: SurahEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("surah_card_${surah.nomor}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VerseNumberBadge(
                number = surah.nomor,
                size = 36.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = surah.namaLatin,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = surah.arti,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = surah.namaArab,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = AmiriQuranFontFamily, fontSize = 21.sp, lineHeight = 25.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, softWrap = false
                )
                Text("${surah.jumlahAyat} ayat", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ChevronRight, null,
                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider(Modifier.padding(start = 56.dp, end = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun HomeShortcut(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp, shadowElevation = 3.dp) {
        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(27.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
