package com.irengcloud.quran.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irengcloud.quran.R
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.data.local.SearchAyatItem
import com.irengcloud.quran.ui.SearchUiState
import com.irengcloud.quran.ui.components.AppArtwork
import com.irengcloud.quran.ui.components.VerseNumberBadge
import com.irengcloud.quran.ui.theme.AmiriQuranFontFamily

private val SearchIndigo = Color(0xFF202034)
private val SearchIvory = Color(0xFFFFF2E5)
private val PopularKeywords = listOf("rahmat", "sabar", "syukur", "takwa", "rezeki", "doa", "ilmu", "ampunan")

private enum class SearchOrder(val label: String) {
    RELEVANCE("Relevansi"),
    MUSHAF("Urutan mushaf")
}

@Composable
fun SearchAyatScreen(
    searchState: SearchUiState,
    readerSettings: ReaderSettingsState,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onExecuteSearch: (String) -> Unit,
    onPageSizeChange: (Int) -> Unit,
    onLoadNextPage: () -> Unit,
    onOpenSurahAtAyat: (Int, Int) -> Unit,
    onPlaySearchItemAudio: (SearchAyatItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var order by rememberSaveable { mutableStateOf(SearchOrder.RELEVANCE) }
    val submittedQuery = searchState.submittedQuery
    val results = remember(searchState.items, order, submittedQuery) {
        when (order) {
            SearchOrder.MUSHAF -> searchState.items.sortedWith(compareBy({ it.surahNomor }, { it.nomorAyat }))
            SearchOrder.RELEVANCE -> {
                val exactMatch = Regex(
                    "(?<![\\p{L}\\p{N}])${Regex.escape(submittedQuery)}(?![\\p{L}\\p{N}])",
                    RegexOption.IGNORE_CASE
                )
                searchState.items.sortedByDescending { exactMatch.findAll(it.teksIndonesia).count() }
            }
        }
    }

    fun submit(query: String) {
        focusManager.clearFocus()
        onExecuteSearch(query)
    }

    LaunchedEffect(Unit) {
        if (!searchState.hasSearchedOnce && !searchState.isLoading && searchState.query.isNotBlank()) {
            onExecuteSearch(searchState.query)
        }
    }

    BoxWithConstraints(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val density = LocalDensity.current
        val scale = (maxWidth / 380.dp).coerceIn(1f, 1.26f)
        CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale)) {
            LazyColumn(
                Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("search_ayat_lazy_column"),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item(key = "header") { SearchHeader(onBack) }
                item(key = "input") {
                    SearchField(
                        searchState.query, onQueryChange, onClearSearch,
                        onSubmit = { submit(searchState.query) }
                    )
                }
                if (results.isNotEmpty()) {
                    item(key = "result_summary") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                if (searchState.hasMore && searchState.totalItems <= results.size) {
                                    "${results.size} ayat dimuat"
                                } else {
                                    "Hasil pencarian: ${searchState.totalItems} ayat"
                                },
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            SearchSortMenu(order, { order = it }, searchState.pageSize, onPageSizeChange)
                        }
                    }
                }

                when {
                    searchState.isLoading -> item(key = "loading") {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 38.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(26.dp))
                            Text("Mencari ayat…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    results.isEmpty() && searchState.errorMessage != null -> item(key = "error") {
                        SearchFeedback("Pencarian gagal dimuat", searchState.errorMessage) {
                            TextButton(onClick = { submit(submittedQuery) }) { Text("Coba Lagi") }
                        }
                    }
                    results.isEmpty() && submittedQuery.isNotBlank() -> item(key = "no_results") {
                        SearchFeedback("Tidak ada ayat yang cocok", "Coba kata kunci terjemahan lain.")
                    }
                    results.isEmpty() -> item(key = "suggestions") {
                        SearchFeedback("Kata Kunci Populer") {
                            PopularKeywords.chunked(4).forEach { keywords ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                                ) {
                                    keywords.forEach { keyword ->
                                        AssistChip(
                                            onClick = { submit(keyword) },
                                            label = { Text(keyword, maxLines = 1) },
                                            modifier = Modifier.weight(1f, fill = false)
                                                .testTag("keyword_chip_$keyword")
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        items(
                            results,
                            key = { "${it.surahNomor}_${it.nomorAyat}" },
                            contentType = { "verse" }
                        ) { result ->
                            SearchResultCard(
                                result, submittedQuery, readerSettings,
                                onOpen = { onOpenSurahAtAyat(result.surahNomor, result.nomorAyat) },
                                onPlayAudio = { onPlaySearchItemAudio(result) }
                            )
                        }
                        if (searchState.hasMore || searchState.isLoadingMore) {
                            item(key = "next_page") {
                                SearchFeedback(
                                    title = if (searchState.errorMessage != null) "Halaman berikutnya gagal dimuat" else null
                                ) {
                                    if (searchState.isLoadingMore) {
                                        CircularProgressIndicator(Modifier.size(24.dp))
                                    } else {
                                        OutlinedButton(
                                            onClick = onLoadNextPage,
                                            modifier = Modifier.testTag("load_more_search_button")
                                        ) {
                                            Text(if (searchState.errorMessage == null) "Muat Hasil Berikutnya" else "Coba Lagi")
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
}

@Composable
private fun SearchHeader(onBack: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 190.dp).background(SearchIndigo)
    ) {
        AppArtwork(
            R.drawable.home_mosque_v2,
            Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )
        Column(
            Modifier.align(Alignment.TopCenter).padding(start = 54.dp, end = 54.dp, top = 14.dp, bottom = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val copper = Color(0xFFE9B182)
            Box(Modifier.size(38.dp), contentAlignment = Alignment.BottomCenter) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(35.dp), tint = copper)
                Icon(
                    Icons.Outlined.Search, null,
                    Modifier.align(Alignment.TopCenter).offset(x = (-4).dp).size(21.dp)
                        .background(SearchIndigo, RoundedCornerShape(50)),
                    tint = copper
                )
            }
            Text("Cari Ayat", style = MaterialTheme.typography.headlineMedium, color = SearchIvory)
            Text(
                "Temukan ayat Al-Qur’an\ndengan kata kunci",
                Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                color = Color(0xFFF0D5B9), textAlign = TextAlign.Center
            )
        }
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(9.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = SearchIvory)
        }
        Spacer(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(27.dp)
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val fieldShape = RoundedCornerShape(20.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
        cursorBrush = SolidColor(scheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            .testTag("search_ayat_input").semantics { contentDescription = "Kata kunci ayat" },
        decorationBox = { input ->
            Surface(
                shape = fieldShape, color = scheme.surface,
                border = BorderStroke(1.dp, scheme.outlineVariant), shadowElevation = 3.dp
            ) {
                Row(Modifier.heightIn(min = 46.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSubmit, modifier = Modifier.testTag("submit_search_ayat_button")) {
                        Icon(Icons.Outlined.Search, "Cari", Modifier.size(22.dp), tint = scheme.primary)
                    }
                    Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        if (query.isEmpty()) {
                            Text("Kata kunci terjemahan…", style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant)
                        }
                        input()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear, modifier = Modifier.testTag("clear_search_ayat_button")) {
                            Icon(Icons.Outlined.Close, "Bersihkan kata kunci", Modifier.size(18.dp), tint = scheme.primary)
                        }
                    } else {
                        Spacer(Modifier.width(14.dp))
                    }
                }
            }
        }
    )
}

@Composable
private fun SearchSortMenu(
    order: SearchOrder,
    onOrderChange: (SearchOrder) -> Unit,
    pageSize: Int,
    onPageSizeChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier.testTag("search_sort_button")
        ) {
            val scheme = MaterialTheme.colorScheme
            Icon(Icons.Outlined.Tune, null, Modifier.size(18.dp), tint = scheme.primary)
            Spacer(Modifier.width(7.dp))
            Text("Urut: ${order.label}", style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold, color = scheme.onBackground)
            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp), tint = scheme.primary)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            Text("Urutkan hasil yang dimuat", Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SearchOrder.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { onOrderChange(option); expanded = false },
                    trailingIcon = {
                        if (order == option) Icon(Icons.Outlined.Check, "Dipilih")
                    }
                )
            }
            HorizontalDivider()
            Text("Hasil per halaman", Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(10, 20, 30).forEach { size ->
                DropdownMenuItem(
                    text = { Text("$size ayat") },
                    onClick = {
                        expanded = false
                        if (pageSize != size) onPageSizeChange(size)
                    },
                    trailingIcon = {
                        if (pageSize == size) Icon(Icons.Outlined.Check, "Dipilih")
                    },
                    modifier = Modifier.testTag("page_size_chip_$size")
                )
            }
        }
    }
}

@Composable
private fun SearchFeedback(
    title: String? = null,
    message: String? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        title?.let { Text(it, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) }
        message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultCard(
    item: SearchAyatItem,
    keyword: String,
    readerSettings: ReaderSettingsState,
    onOpen: () -> Unit,
    onPlayAudio: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(15.dp)
    val translation = remember(item.teksIndonesia, keyword) {
        translationSnippet(item.teksIndonesia, keyword)
    }
    val highlight = if (scheme.background.luminance() > 0.5f) Color(0xFFECC39F) else scheme.tertiaryContainer
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(shape)
            .combinedClickable(
                onClickLabel = "Buka surah pada ayat ${item.nomorAyat}",
                onClick = onOpen,
                onLongClickLabel = "Putar audio ayat",
                onLongClick = onPlayAudio
            )
            .testTag("search_result_${item.surahNomor}_${item.nomorAyat}"),
        shape = shape, color = scheme.surface, contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, scheme.outlineVariant), shadowElevation = 1.dp
    ) {
        Column(Modifier.fillMaxWidth().heightIn(min = 124.dp).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VerseNumberBadge(number = item.surahNomor, size = 41.dp)
                Column(Modifier.weight(1f)) {
                    Text(item.surahNamaLatin, style = MaterialTheme.typography.titleLarge)
                    Text("Ayat ${item.nomorAyat}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif))
                }
                Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), tint = scheme.primary)
            }
            if (item.teksArab.isNotBlank()) {
                Text(
                    item.teksArab,
                    Modifier.fillMaxWidth().padding(horizontal = 15.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = AmiriQuranFontFamily,
                        fontSize = readerSettings.arabicFontSizeSp.coerceIn(22, 30).sp,
                        lineHeight = 36.sp,
                        textDirection = TextDirection.Rtl
                    ),
                    textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (translation.isNotBlank()) {
                Text(
                    highlightKeyword(translation, keyword, highlight, scheme.onTertiaryContainer),
                    Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = readerSettings.translationFontSizeSp.sp,
                        lineHeight = (readerSettings.translationFontSizeSp * 1.34f).sp
                    ),
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun translationSnippet(text: String, keyword: String): String {
    val normalized = text.trim().replace(Regex("\\s+"), " ")
    if (normalized.length <= 112) return normalized
    val match = normalized.indexOf(keyword, ignoreCase = true).coerceAtLeast(0)
    val desiredStart = (match - 32).coerceAtLeast(0)
    val start = if (desiredStart == 0) 0 else normalized.indexOf(' ', desiredStart)
        .takeIf { it in desiredStart until match }?.plus(1) ?: desiredStart
    val limit = (start + maxOf(112, match - start + keyword.length)).coerceAtMost(normalized.length)
    val end = if (limit == normalized.length) limit else normalized.lastIndexOf(' ', limit)
        .takeIf { it >= maxOf(start + 1, match + keyword.length) } ?: limit
    return (if (start > 0) "… " else "") + normalized.substring(start, end) +
        (if (end < normalized.length) " …" else "")
}

private fun highlightKeyword(text: String, keyword: String, background: Color, foreground: Color) =
    buildAnnotatedString {
        if (keyword.isBlank()) {
            append(text)
            return@buildAnnotatedString
        }
        var cursor = 0
        while (true) {
            val next = text.indexOf(keyword, cursor, ignoreCase = true)
            if (next < 0) break
            append(text.substring(cursor, next))
            cursor = next + keyword.length
            withStyle(SpanStyle(background = background, color = foreground, fontWeight = FontWeight.Bold)) {
                append(text.substring(next, cursor))
            }
        }
        append(text.substring(cursor))
    }
