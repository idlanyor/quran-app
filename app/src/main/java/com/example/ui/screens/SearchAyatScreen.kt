package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ReaderSettingsState
import com.example.data.local.SearchAyatItem
import com.example.ui.SearchUiState
import com.example.ui.components.VerseNumberBadge
import com.example.ui.theme.AmiriQuranFontFamily

private val PopularKeywords = listOf(
    "rahmat",
    "sabar",
    "syukur",
    "takwa",
    "rezeki",
    "doa",
    "ilmu",
    "ampunan"
)

@Composable
fun SearchAyatScreen(
    searchState: SearchUiState,
    readerSettings: ReaderSettingsState,
    onQueryChange: (String) -> Unit,
    onExecuteSearch: (String) -> Unit,
    onPageSizeChange: (Int) -> Unit,
    onLoadNextPage: () -> Unit,
    onOpenSurahAtAyat: (Int, Int) -> Unit,
    onPlaySearchItemAudio: (SearchAyatItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    // Automatically run initial default search ("rahmat") on first open
    LaunchedEffect(Unit) {
        if (!searchState.hasSearchedOnce && searchState.items.isEmpty()) {
            onExecuteSearch(searchState.query.ifBlank { "rahmat" })
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("search_ayat_lazy_column"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pencarian Ayat Al-Qur'an",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Cari ayat berdasarkan kata kunci terjemahan Bahasa Indonesia beserta teks Arab & audio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchState.query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_ayat_input"),
                            placeholder = { Text("Kata kunci (contoh: rahmat, sabar)…") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchState.query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onQueryChange("") },
                                        modifier = Modifier.testTag("clear_search_ayat_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Bersihkan kata kunci"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    onExecuteSearch(searchState.query)
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onExecuteSearch(searchState.query)
                            },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                            modifier = Modifier.testTag("submit_search_ayat_button")
                        ) {
                            Text("Cari")
                        }
                    }

                    // Popular Keyword Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        PopularKeywords.forEach { kw ->
                            AssistChip(
                                onClick = {
                                    onQueryChange(kw)
                                    focusManager.clearFocus()
                                    onExecuteSearch(kw)
                                },
                                label = { Text(kw) },
                                modifier = Modifier.testTag("keyword_chip_$kw")
                            )
                        }
                    }

                    // Pagination Page Size selector (`size` parameter 10, 20, 30) & Status info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchState.items.isNotEmpty()) {
                                "Menampilkan ${searchState.items.size} ayat (Hal. ${searchState.currentPage})"
                            } else {
                                "Hasil per halaman:"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(10, 20, 30).forEach { sz ->
                                val selected = searchState.pageSize == sz
                                FilterChip(
                                    selected = selected,
                                    onClick = { onPageSizeChange(sz) },
                                    label = { Text("$sz/hal") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("page_size_chip_$sz")
                                )
                            }
                        }
                    }
                }
            }

            when {
                searchState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Mencari ayat dengan kata kunci \"${searchState.query}\"…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                searchState.errorMessage != null && searchState.items.isEmpty() -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Pencarian Gagal Dimuat",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = searchState.errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(onClick = { onExecuteSearch(searchState.query) }) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                }

                searchState.items.isEmpty() && searchState.hasSearchedOnce -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Tidak ada ayat yang cocok",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Coba kata kunci terjemahan lain seperti 'rahmat', 'sabar', atau 'doa'.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                else -> {
                    items(
                        items = searchState.items,
                        key = { "${it.surahNomor}_${it.nomorAyat}" }
                    ) { item ->
                        SearchResultAyatCard(
                            item = item,
                            highlightKeyword = searchState.query,
                            readerSettings = readerSettings,
                            onOpenSurah = {
                                onOpenSurahAtAyat(item.surahNomor, item.nomorAyat)
                            },
                            onPlayAudio = {
                                onPlaySearchItemAudio(item)
                            }
                        )
                    }

                    // Pagination Load More button (`page` + 1)
                    if (searchState.hasMore || searchState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (searchState.isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    OutlinedButton(
                                        onClick = onLoadNextPage,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("load_more_search_button")
                                    ) {
                                        Text("Muat Halaman Berikutnya (Hal. ${searchState.currentPage + 1})")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultAyatCard(
    item: SearchAyatItem,
    highlightKeyword: String,
    readerSettings: ReaderSettingsState,
    onOpenSurah: () -> Unit,
    onPlayAudio: () -> Unit
) {
    val highlightBg = MaterialTheme.colorScheme.tertiaryContainer
    val highlightFg = MaterialTheme.colorScheme.onTertiaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSurah)
            .testTag("search_result_${item.surahNomor}_${item.nomorAyat}"),
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
                    VerseNumberBadge(number = item.nomorAyat, size = 36.dp)
                    Column {
                        Text(
                            text = "QS. ${item.surahNamaLatin} (${item.surahNomor}) : Ayat ${item.nomorAyat}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (item.surahNamaArab.isNotBlank()) {
                            Text(
                                text = item.surahNamaArab,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayAudio,
                        modifier = Modifier.testTag("play_search_item_${item.surahNomor}_${item.nomorAyat}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Putar Audio Ayat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable(onClick = onOpenSurah)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Buka Surah",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            if (item.teksArab.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = item.teksArab,
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
            }

            if (readerSettings.showLatin && item.teksLatin.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item.teksLatin,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = (readerSettings.translationFontSizeSp - 1).coerceAtLeast(12).sp,
                        lineHeight = ((readerSettings.translationFontSizeSp - 1) * 1.55f).sp,
                        textDirection = TextDirection.Ltr
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (item.teksIndonesia.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                val highlightedText = buildHighlightedTranslation(
                    text = item.teksIndonesia,
                    keyword = highlightKeyword,
                    highlightBackground = highlightBg,
                    highlightColor = highlightFg
                )
                Text(
                    text = highlightedText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = readerSettings.translationFontSizeSp.sp,
                        lineHeight = (readerSettings.translationFontSizeSp * 1.55f).sp,
                        textDirection = TextDirection.Ltr
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun buildHighlightedTranslation(
    text: String,
    keyword: String,
    highlightBackground: androidx.compose.ui.graphics.Color,
    highlightColor: androidx.compose.ui.graphics.Color
) = buildAnnotatedString {
    val cleanKw = keyword.trim()
    if (cleanKw.length < 2) {
        append(text)
        return@buildAnnotatedString
    }
    val lowerText = text.lowercase()
    val lowerKw = cleanKw.lowercase()
    var startIndex = 0
    while (startIndex < text.length) {
        val matchIdx = lowerText.indexOf(lowerKw, startIndex)
        if (matchIdx == -1) {
            append(text.substring(startIndex))
            break
        }
        if (matchIdx > startIndex) {
            append(text.substring(startIndex, matchIdx))
        }
        withStyle(
            SpanStyle(
                background = highlightBackground,
                color = highlightColor,
                fontWeight = FontWeight.Bold
            )
        ) {
            append(text.substring(matchIdx, matchIdx + cleanKw.length))
        }
        startIndex = matchIdx + cleanKw.length
    }
}
