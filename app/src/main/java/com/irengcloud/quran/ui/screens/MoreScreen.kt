package com.irengcloud.quran.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.irengcloud.quran.R
import com.irengcloud.quran.ui.components.AppArtwork

@Composable
fun MoreScreen(
    onOpenSearch: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("more_screen"),
            contentPadding = PaddingValues(bottom = 110.dp)) {
            item(key = "header", contentType = "header") {
                Box(Modifier.fillMaxWidth().height(148.dp).background(Color(0xFF252439))) {
                    AppArtwork(R.drawable.bookmarks_header, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    Column(Modifier.align(Alignment.Center).padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Icon(Icons.Outlined.Apps, null, tint = Color(0xFFE0B48A), modifier = Modifier.size(30.dp))
                        Text("Lainnya", style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFFFF1E3))
                    }
                }
            }
            item(key = "introduction", contentType = "introduction") {
                Box(Modifier.fillMaxWidth().background(Color(0xFF252439))) {
                    Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 23.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("Temani ibadah harianmu", style = MaterialTheme.typography.titleLarge)
                            Text("Temukan ayat, lihat waktu sholat, dan sesuaikan pengalaman membaca.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item(key = "search", contentType = "feature") {
                MoreMenuCard("Cari Ayat", "Temukan ayat melalui kata kunci terjemahan.",
                    Icons.Outlined.Search, "more_search", onOpenSearch)
            }
            item(key = "prayer", contentType = "feature") {
                MoreMenuCard("Jadwal Sholat & Kiblat", "Waktu sholat dan arah kiblat di lokasimu.",
                    Icons.Outlined.AccessTime, "more_prayer", onOpenPrayer)
            }
            item(key = "settings", contentType = "feature") {
                MoreMenuCard("Pengaturan", "Ukuran huruf, tema, terjemahan, dan pengingat.",
                    Icons.Outlined.Settings, "more_settings", onOpenSettings)
            }
        }
    }
}

@Composable
private fun MoreMenuCard(title: String, description: String, icon: ImageVector,
    tag: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).testTag(tag)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(13.dp).size(25.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        }
    }
}
