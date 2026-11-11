package com.irengcloud.quran.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irengcloud.quran.R
import com.irengcloud.quran.ui.TafsirSheetUiState
import com.irengcloud.quran.ui.components.TransparentArtworkPaper
import com.irengcloud.quran.ui.components.VerseNumberBadge
import com.irengcloud.quran.ui.components.rememberArtwork
import com.irengcloud.quran.ui.theme.AmiriQuranFontFamily
import kotlin.math.roundToInt

private val TafsirIndigo = Color(0xFF252439)
private val TafsirIvory = Color(0xFFFFF4E6)
private val TafsirCream = Color(0xFFF1DFCE)

private enum class TafsirSourceOption(
    val cardTitle: String,
    val subtitle: String,
    val description: String,
    val isConciseByDefault: Boolean
) {
    IBNU_KATSIR_RINGKAS(
        cardTitle = "Tafsir Ibnu Katsir",
        subtitle = "Tafsir Ibnu Katsir (Ringkas)",
        description = "Menyajikan intisari penjelasan makna ayat secara ringkas, padat, dan mudah dipahami untuk tadabbur harian.",
        isConciseByDefault = true
    ),
    KEMENAG_LENGKAP(
        cardTitle = "Tafsir Kemenag RI",
        subtitle = "Tafsir Kemenag RI (Lengkap)",
        description = "Menampilkan uraian tafsir tahlili lengkap dari Lajnah Pentashihan Mushaf Al-Qur'an Kementerian Agama RI.",
        isConciseByDefault = false
    )
}

@Composable
fun TafsirScreen(
    state: TafsirSheetUiState,
    arabicFontSizeSp: Int,
    showLatin: Boolean = false,
    isBookmarked: Boolean = false,
    onBack: () -> Unit,
    onPreviousAyat: () -> Unit,
    onNextAyat: () -> Unit,
    onToggleBookmark: () -> Unit,
    onPlayAudio: () -> Unit,
    onRefreshTafsir: () -> Unit,
    onCopyTafsir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    BackHandler(onBack = onBack)

    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    var showOverflowMenu by remember { mutableStateOf(false) }
    var isTafsirExpanded by rememberSaveable { mutableStateOf(true) }
    var isFullTextExpanded by rememberSaveable(state.surahNomor, state.nomorAyat) { mutableStateOf(false) }
    var selectedSource by rememberSaveable { mutableStateOf(TafsirSourceOption.IBNU_KATSIR_RINGKAS) }
    var showSourceDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.surahNomor, state.nomorAyat) {
        listState.scrollToItem(0)
    }

    val copyCurrentTafsir = {
        val fullCopy = buildString {
            appendLine("QS. ${state.surahNamaLatin} (${state.surahNomor}): Ayat ${state.nomorAyat}")
            if (state.teksArab.isNotBlank()) appendLine(state.teksArab)
            if (state.teksIndonesia.isNotBlank()) appendLine("\"${state.teksIndonesia}\"")
            appendLine()
            appendLine("${selectedSource.cardTitle}:")
            append(state.teksTafsir)
        }
        clipboard.setText(AnnotatedString(fullCopy))
        onCopyTafsir("Tafsir ayat ${state.nomorAyat} disalin ke papan klip")
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = {
                Text(
                    text = "Sumber Tafsir",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TafsirSourceOption.entries.forEach { option ->
                        val selected = option == selectedSource
                        Surface(
                            onClick = {
                                selectedSource = option
                                showSourceDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = {
                                        selectedSource = option
                                        showSourceDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSourceDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    val paper = MaterialTheme.colorScheme.background

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TafsirIndigo)
            .testTag("tafsir_bottom_sheet"),
        contentAlignment = Alignment.TopCenter
    ) {
        val density = LocalDensity.current
        val scale = (maxWidth / 380.dp).coerceIn(1f, 1.24f)
        CompositionLocalProvider(
            LocalDensity provides Density(density.density * scale, density.fontScale),
            LocalContentColor provides MaterialTheme.colorScheme.onBackground
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = TafsirIndigo,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(TafsirIndigo)
                ) {
                    // Top App Bar over the starry midnight sky slice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        TafsirArtworkSlice(
                            start = 0f,
                            end = 0.326f,
                            modifier = Modifier.fillMaxSize()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("close_tafsir_sheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = TafsirIvory
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier.testTag("tafsir_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = if (isBookmarked) "Hapus penanda" else "Simpan penanda",
                                    tint = TafsirCream
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                    modifier = Modifier.testTag("tafsir_more_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Menu tafsir",
                                        tint = TafsirCream
                                    )
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Salin ayat & tafsir") },
                                        onClick = {
                                            showOverflowMenu = false
                                            copyCurrentTafsir()
                                        },
                                        modifier = Modifier.testTag("copy_tafsir_button")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Putar audio ayat") },
                                        onClick = {
                                            showOverflowMenu = false
                                            onPlayAudio()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Pilih sumber tafsir") },
                                        onClick = {
                                            showOverflowMenu = false
                                            showSourceDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Muat ulang tafsir") },
                                        onClick = {
                                            showOverflowMenu = false
                                            onRefreshTafsir()
                                        }
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Tafsir",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TafsirIvory,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Main Scrollable Tafsir Content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(paper),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 680.dp)
                                .background(paper)
                                .testTag("tafsir_lazy_column"),
                            contentPadding = PaddingValues(bottom = 28.dp)
                        ) {
                            // 1. Pointed Islamic Arch Opening with Surah Badge & Ayat Prev/Next Navigation
                            item(key = "tafsir_opening") {
                                TafsirArchHeader(
                                    surahNomor = state.surahNomor,
                                    surahNamaLatin = state.surahNamaLatin,
                                    nomorAyat = state.nomorAyat,
                                    jumlahAyat = state.jumlahAyat,
                                    onPreviousAyat = onPreviousAyat,
                                    onNextAyat = onNextAyat
                                )
                            }

                            // 2. Top Ornamental Copper Divider
                            item(key = "ornament_top") {
                                TafsirOrnamentalDivider(
                                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)
                                )
                            }

                            // 3. Arabic Verse Card
                            if (state.teksArab.isNotBlank()) {
                                item(key = "arabic_card") {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 6.dp)
                                            .testTag("tafsir_arabic_card"),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        shadowElevation = 1.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 18.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = state.teksArab,
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontFamily = AmiriQuranFontFamily,
                                                    fontSize = (arabicFontSizeSp - 1).coerceAtLeast(24).sp,
                                                    lineHeight = ((arabicFontSizeSp - 1) * 1.9f).sp,
                                                    textDirection = TextDirection.Rtl
                                                ),
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (showLatin && state.teksLatin.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = state.teksLatin,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontStyle = FontStyle.Italic
                                                    ),
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Terjemahan Card
                            item(key = "translation_card") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 6.dp)
                                        .testTag("tafsir_translation_card"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 13.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Terjemahan",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = FontFamily.Serif,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(7.dp))
                                        Text(
                                            text = "“${state.teksIndonesia}”",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 14.5.sp,
                                                lineHeight = 22.sp,
                                                textDirection = TextDirection.Ltr
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // 5. Expandable Tafsir Ibnu Katsir Card
                            item(key = "tafsir_explanation_card") {
                                val (displayedText, isTruncated) = remember(
                                    state.teksTafsir,
                                    selectedSource,
                                    isFullTextExpanded
                                ) {
                                    formatTafsirText(
                                        rawText = state.teksTafsir,
                                        conciseMode = selectedSource.isConciseByDefault && !isFullTextExpanded
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 6.dp)
                                        .testTag("tafsir_content_card"),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shadowElevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isTafsirExpanded = !isTafsirExpanded }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(21.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = selectedSource.cardTitle,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontFamily = FontFamily.Serif,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = if (isTafsirExpanded) {
                                                    Icons.Outlined.ExpandMore
                                                } else {
                                                    Icons.Outlined.ExpandLess
                                                },
                                                contentDescription = if (isTafsirExpanded) {
                                                    "Sembunyikan uraian tafsir"
                                                } else {
                                                    "Tampilkan uraian tafsir"
                                                },
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = isTafsirExpanded,
                                            enter = expandVertically(),
                                            exit = shrinkVertically()
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                                )
                                                if (state.isLoading) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 32.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            CircularProgressIndicator(
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(28.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(10.dp))
                                                            Text(
                                                                text = "Memuat tafsir ayat…",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                                    ) {
                                                        Text(
                                                            text = displayedText,
                                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                                fontFamily = FontFamily.Serif,
                                                                fontSize = 14.5.sp,
                                                                lineHeight = 23.sp
                                                            ),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (selectedSource.isConciseByDefault && (isTruncated || isFullTextExpanded)) {
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Text(
                                                                text = if (isFullTextExpanded) {
                                                                    "Tampilkan ringkasan"
                                                                } else {
                                                                    "Baca tafsir selengkapnya…"
                                                                },
                                                                style = MaterialTheme.typography.labelLarge.copy(
                                                                    fontFamily = FontFamily.Serif,
                                                                    fontWeight = FontWeight.Bold
                                                                ),
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .clickable { isFullTextExpanded = !isFullTextExpanded }
                                                                    .padding(vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 6. Bottom Ornamental Copper Divider
                            item(key = "ornament_bottom") {
                                TafsirOrnamentalDivider(
                                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)
                                )
                            }

                            // 7. Sumber Tafsir Card
                            item(key = "sumber_tafsir_card") {
                                Surface(
                                    onClick = { showSourceDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 4.dp)
                                        .testTag("tafsir_source_card"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shadowElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Sumber Tafsir",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontFamily = FontFamily.Serif,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = selectedSource.subtitle,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Serif
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.ChevronRight,
                                            contentDescription = "Pilih sumber tafsir",
                                            tint = MaterialTheme.colorScheme.primary
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
}

@Composable
private fun TafsirArchHeader(
    surahNomor: Int,
    surahNamaLatin: String,
    nomorAyat: Int,
    jumlahAyat: Int,
    onPreviousAyat: () -> Unit,
    onNextAyat: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        TafsirArtworkSlice(
            start = 0.326f,
            end = 1f,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.35f),
            blendIntoPaper = true
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerseNumberBadge(
                number = surahNomor,
                size = 38.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousAyat,
                    enabled = nomorAyat > 1,
                    modifier = Modifier.testTag("tafsir_prev_ayat_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = "Ayat sebelumnya",
                        tint = if (nomorAyat > 1) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = surahNamaLatin,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ayat $nomorAyat",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Serif
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                IconButton(
                    onClick = onNextAyat,
                    enabled = nomorAyat < jumlahAyat,
                    modifier = Modifier.testTag("tafsir_next_ayat_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Ayat berikutnya",
                        tint = if (nomorAyat < jumlahAyat) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Delicate copper diamond arabesque divider (`── ❖ ──`) matching the Tafsir mockup.
 */
@Composable
private fun TafsirOrnamentalDivider(modifier: Modifier = Modifier) {
    val copper = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, lineColor)
                    )
                )
        )
        Canvas(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(width = 28.dp, height = 14.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val mainR = 4.8.dp.toPx()
            val sideR = 2.6.dp.toPx()
            val sideOffset = 8.dp.toPx()

            // Central hollow diamond
            val centerDiamond = Path().apply {
                moveTo(cx, cy - mainR)
                lineTo(cx + mainR, cy)
                lineTo(cx, cy + mainR)
                lineTo(cx - mainR, cy)
                close()
            }
            drawPath(centerDiamond, color = copper, style = Stroke(width = 1.4.dp.toPx()))
            drawCircle(color = copper, radius = 1.3.dp.toPx(), center = Offset(cx, cy))

            // Left & right petal diamonds
            for (dir in listOf(-1f, 1f)) {
                val sx = cx + dir * sideOffset
                val sideDiamond = Path().apply {
                    moveTo(sx, cy - sideR)
                    lineTo(sx + sideR, cy)
                    lineTo(sx, cy + sideR)
                    lineTo(sx - sideR, cy)
                    close()
                }
                drawPath(sideDiamond, color = copper, style = Stroke(width = 1.1.dp.toPx()))
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(lineColor, Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun TafsirArtworkSlice(
    start: Float,
    end: Float,
    modifier: Modifier = Modifier,
    blendIntoPaper: Boolean = false
) {
    val artwork by rememberArtwork(R.drawable.tafsir_top)
    val paper = MaterialTheme.colorScheme.background
    val useDarkPaper = blendIntoPaper && paper.luminance() < 0.5f
    Canvas(modifier) {
        val bitmap = artwork
        if (bitmap == null) {
            if (blendIntoPaper) drawRect(paper)
            return@Canvas
        }
        val top = (bitmap.height * start).roundToInt()
        val bottom = (bitmap.height * end).roundToInt().coerceAtMost(bitmap.height)
        if (useDarkPaper) drawRect(paper)
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(0, top),
            srcSize = IntSize(bitmap.width, (bottom - top).coerceAtLeast(1)),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            colorFilter = if (useDarkPaper) TransparentArtworkPaper else null,
            filterQuality = FilterQuality.High
        )
        if (blendIntoPaper) {
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, paper),
                    startY = size.height * 0.86f,
                    endY = size.height
                )
            )
        }
    }
}

private fun formatTafsirText(rawText: String, conciseMode: Boolean): Pair<String, Boolean> {
    val cleaned = rawText.trim()
    if (!conciseMode || cleaned.length <= 540) {
        return cleaned to false
    }
    val maxLen = 520
    val periodIdx = cleaned.indexOf(". ", startIndex = 320)
    val cutIndex = when {
        periodIdx in 320..620 -> periodIdx + 1
        else -> {
            val lastSpace = cleaned.lastIndexOf(' ', startIndex = maxLen)
            if (lastSpace > 200) lastSpace else maxLen
        }
    }
    val snippet = cleaned.substring(0, cutIndex).trimEnd()
    return (if (snippet.endsWith(".")) snippet else "$snippet…") to true
}
