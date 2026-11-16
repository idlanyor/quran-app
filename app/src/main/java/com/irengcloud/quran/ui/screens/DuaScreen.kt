package com.irengcloud.quran.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.irengcloud.quran.R
import com.irengcloud.quran.data.Dua
import com.irengcloud.quran.data.filterDuas
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.ui.DuaState
import com.irengcloud.quran.ui.components.AppArtwork
import com.irengcloud.quran.ui.theme.AmiriQuranFontFamily

@Composable
fun DuaScreen(state: DuaState, settings: ReaderSettingsState, onBack: () -> Unit,
    onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var chooseGroup by rememberSaveable { mutableStateOf(false) }
    val selected = state.items.firstOrNull { it.id == selectedId }
    val results = remember(state.items, query, group) { filterDuas(state.items, query, group) }
    val groups = remember(state.items) { state.items.map { it.group }.filter { it.isNotBlank() }.distinct() }
    val listState = rememberLazyListState()
    val scheme = MaterialTheme.colorScheme
    BackHandler(selectedId != null) { selectedId = null }

    Surface(modifier.fillMaxSize(), color = scheme.background) {
        LazyColumn(Modifier.fillMaxSize().widthIn(max = 720.dp).testTag("dua_screen"),
            state = if (selected == null) listState else rememberLazyListState(),
            contentPadding = PaddingValues(bottom = 110.dp)) {
            item(key = "header") {
                Box(Modifier.fillMaxWidth().height(176.dp).background(Color(0xFF252439))) {
                    AppArtwork(R.drawable.bookmarks_header, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    Column(Modifier.align(Alignment.Center).padding(horizontal = 54.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Color(0xFFE0B48A), modifier = Modifier.size(28.dp))
                        Text(if (selected == null) "Doa Harian" else "Bacaan Doa",
                            style = MaterialTheme.typography.headlineMedium, color = Color(0xFFFFF1E3))
                        Text("Dekatkan hati dalam setiap aktivitas", style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF0D5B9), textAlign = TextAlign.Center)
                    }
                    IconButton(onClick = { if (selectedId != null) selectedId = null else onBack() },
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color(0xFFFFF1E3))
                    }
                    Spacer(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(20.dp)
                        .background(scheme.background, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)))
                }
            }
            if (selected != null) {
                item(key = "detail_${selected.id}") { DuaReading(selected, settings) }
            } else {
                item(key = "search") {
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = query, onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().testTag("dua_search"), singleLine = true,
                            placeholder = { Text("Cari doa, misalnya tidur atau makan") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, "Hapus pencarian")
                            } }, shape = RoundedCornerShape(16.dp))
                        OutlinedButton(onClick = { chooseGroup = true }, modifier = Modifier.fillMaxWidth().testTag("dua_category")) {
                            Text(group ?: "Semua kategori", Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ExpandMore, null)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${results.size} doa & dzikir", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = onRefresh, enabled = !state.loading) { Icon(Icons.Default.Refresh, "Perbarui doa") }
                        }
                        Text("Sumber: EQuran.id · Tersimpan untuk dibaca offline setelah dimuat.",
                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("dua_loading"))
                        if (state.refreshFailed) Text("Pembaruan gagal. Menampilkan doa yang tersimpan di perangkat.",
                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        if (state.error != null) {
                            Text(state.error, color = scheme.error)
                            Button(onClick = onRefresh) { Text("Coba lagi") }
                        }
                        if (!state.loading && state.error == null && results.isEmpty()) {
                            Text("Tidak ada doa yang cocok. Coba kata lain atau pilih semua kategori.", Modifier.padding(vertical = 22.dp))
                        }
                    }
                }
                items(results, key = { it.id }) { dua ->
                    Surface(onClick = { selectedId = dua.id }, modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp).testTag("dua_item_${dua.id}"),
                        shape = RoundedCornerShape(18.dp), color = scheme.surface,
                        border = BorderStroke(1.dp, scheme.outlineVariant)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(dua.name, style = MaterialTheme.typography.titleLarge)
                            Text(dua.group, style = MaterialTheme.typography.labelMedium, color = scheme.primary)
                            Text(dua.translation.ifBlank { "Buka untuk membaca doa dan keterangannya." }, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    if (chooseGroup) AlertDialog(onDismissRequest = { chooseGroup = false },
        title = { Text("Kategori doa") }, text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                item { TextButton(onClick = { group = null; chooseGroup = false }, modifier = Modifier.fillMaxWidth()) { Text("Semua kategori") } }
                items(groups) { category -> TextButton(onClick = { group = category; chooseGroup = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(category, Modifier.fillMaxWidth())
                } }
            }
        }, confirmButton = { TextButton(onClick = { chooseGroup = false }) { Text("Tutup") } })
}

@Composable
private fun DuaReading(dua: Dua, settings: ReaderSettingsState) {
    val scheme = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("dua_detail"),
        shape = RoundedCornerShape(20.dp), color = scheme.surface, border = BorderStroke(1.dp, scheme.outlineVariant)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(dua.name, style = MaterialTheme.typography.headlineSmall)
                Text(dua.group, style = MaterialTheme.typography.labelLarge, color = scheme.primary)
            }
            Text(dua.arabic, modifier = Modifier.fillMaxWidth(), color = scheme.onSurface,
                style = TextStyle(fontFamily = AmiriQuranFontFamily, fontSize = settings.arabicFontSizeSp.sp,
                    lineHeight = (settings.arabicFontSizeSp * 1.9f).sp, textDirection = TextDirection.Rtl, textAlign = TextAlign.Right))
            if (settings.showLatin && dua.latin.isNotBlank()) {
                Text(dua.latin, style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = settings.translationFontSizeSp.sp, lineHeight = (settings.translationFontSizeSp * 1.6f).sp), color = scheme.primary)
            }
            if (settings.showTranslation) Text(dua.translation.ifBlank { "Terjemahan belum tersedia dari sumber." }, style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = settings.translationFontSizeSp.sp, lineHeight = (settings.translationFontSizeSp * 1.6f).sp), color = scheme.onSurface)
            if (dua.reference.isNotBlank()) {
                HorizontalDivider(color = scheme.outlineVariant)
                Text("Keterangan & sumber", style = MaterialTheme.typography.titleMedium)
                val reference = remember(dua.reference) { HtmlCompat.fromHtml(dua.reference, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim() }
                Text(reference, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            Text("Sumber data: EQuran.id", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}
