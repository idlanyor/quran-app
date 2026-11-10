package com.irengcloud.quran.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.irengcloud.quran.R
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.data.local.ThemeMode
import com.irengcloud.quran.ui.DailyFeaturesViewModel
import com.irengcloud.quran.ui.components.AppArtwork
import com.irengcloud.quran.ui.components.DailyReminderSettings
import com.irengcloud.quran.ui.components.SettingsLabel
import com.irengcloud.quran.ui.components.SettingsSwitch
import com.irengcloud.quran.ui.theme.AmiriQuranFontFamily
import kotlin.math.abs
import kotlin.math.roundToInt

private val HeaderIndigo = Color(0xFF242338)
private val CopperGradient = Brush.verticalGradient(listOf(Color(0xFFAC7653), Color(0xFF79452D)))
private val HeaderIvory = Color(0xFFFFF1E3)
private val ArabicSizePresets = listOf(22, 26, 30, 44)

@Composable
fun SettingsScreen(
    settings: ReaderSettingsState,
    cachedSurahCount: Int,
    onArabicFontSizeChange: (Int) -> Unit,
    onTranslationFontSizeChange: (Int) -> Unit,
    onShowLatinChange: (Boolean) -> Unit,
    onShowTranslationChange: (Boolean) -> Unit,
    onAutoNextAudioChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    dailyFeatures: DailyFeaturesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val panelShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    BoxWithConstraints(
        modifier.fillMaxSize().background(scheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val density = LocalDensity.current
        val scale = (maxWidth / 380.dp).coerceIn(1f, 1.26f)
        CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale)) {
            LazyColumn(
                Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("settings_screen_column"),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "header") {
                    Box(Modifier.fillMaxWidth().heightIn(min = 180.dp).background(HeaderIndigo)) {
                        AppArtwork(
                            R.drawable.settings_header_v2,
                            Modifier.matchParentSize().padding(bottom = 12.dp),
                            contentScale = ContentScale.FillBounds
                        )
                        Column(
                            Modifier.align(Alignment.TopCenter).padding(start = 53.dp, end = 53.dp, top = 14.dp, bottom = 38.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.Settings, null, Modifier.size(34.dp), tint = Color(0xFFE9B182))
                            Text(
                                "Pengaturan", style = MaterialTheme.typography.headlineMedium,
                                color = HeaderIvory
                            )
                            Text(
                                "Atur pengalaman membaca\nAl-Qur’an sesuai preferensi Anda",
                                Modifier.widthIn(max = 245.dp).padding(top = 3.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                                color = HeaderIvory, textAlign = TextAlign.Center
                            )
                        }
                        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(9.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = HeaderIvory)
                        }
                        Spacer(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(28.dp)
                                .background(scheme.background, panelShape)
                        )
                    }
                }

                item(key = "arabic_size") {
                    Column(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsLabel(Icons.Outlined.FormatSize, "Ukuran Huruf Baca", "Atur ukuran teks ayat Arab.")
                        ArabicSizeSelector(settings.arabicFontSizeSp, onArabicFontSizeChange)
                    }
                    SettingsDivider()
                }

                item(key = "translation") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsLabel(
                            Icons.Outlined.Translate,
                            "Tampilkan Terjemahan",
                            "Menampilkan terjemahan bahasa Indonesia saat membaca ayat.",
                            Modifier.weight(1f)
                        )
                        SettingsSwitch(
                            settings.showTranslation, onShowTranslationChange, "Tampilkan Terjemahan",
                            Modifier.testTag("toggle_translation")
                        )
                    }
                    SettingsDivider()
                }

                item(key = "theme") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        SettingsLabel(Icons.Outlined.Palette, "Tema Tampilan", "Pilih tema antarmuka aplikasi.")
                        Row(
                            Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemeOption("Klasik", "Midnight", R.drawable.settings_classic, ThemeMode.CLASSIC,
                                settings.themeMode, onThemeModeChange, Modifier.weight(1f))
                            ThemeOption("Terang", "Ivory", R.drawable.settings_ivory, ThemeMode.LIGHT,
                                settings.themeMode, onThemeModeChange, Modifier.weight(1f))
                            ThemeOption("Gelap", "Indigo", R.drawable.settings_dark, ThemeMode.DARK,
                                settings.themeMode, onThemeModeChange, Modifier.weight(1f))
                        }
                    }
                    SettingsDivider()
                }

                item(key = "reminder") {
                    DailyReminderSettings(
                        settings, dailyFeatures,
                        Modifier.padding(horizontal = 20.dp, vertical = 13.dp),
                        showTestAction = showAdvanced
                    )
                }

                item(key = "advanced") {
                    Surface(
                        onClick = { showAdvanced = !showAdvanced },
                        shape = RoundedCornerShape(12.dp),
                        color = scheme.surface,
                        border = BorderStroke(1.dp, scheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsLabel(Icons.Outlined.Tune, "Pengaturan Lainnya", modifier = Modifier.weight(1f))
                            Icon(
                                if (showAdvanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                if (showAdvanced) "Tutup" else "Buka",
                                tint = scheme.primary
                            )
                        }
                    }
                }

                if (showAdvanced) item(key = "advanced_controls") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Pratinjau Tampilan Ayat", style = MaterialTheme.typography.titleMedium)
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("settings_live_preview_card"),
                            shape = RoundedCornerShape(12.dp),
                            color = scheme.surface
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
                                    Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = AmiriQuranFontFamily,
                                        fontSize = settings.arabicFontSizeSp.sp,
                                        lineHeight = (settings.arabicFontSizeSp * 1.8f).sp,
                                        textDirection = TextDirection.Rtl
                                    ),
                                    textAlign = TextAlign.Right
                                )
                                if (settings.showLatin) {
                                    Text("Bismillāhir-raḥmānir-raḥīm(i).", fontStyle = FontStyle.Italic,
                                        fontSize = (settings.translationFontSizeSp - 1).coerceAtLeast(12).sp)
                                }
                                if (settings.showTranslation) {
                                    Text("Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.",
                                        fontSize = settings.translationFontSizeSp.sp,
                                        lineHeight = (settings.translationFontSizeSp * 1.55f).sp)
                                }
                            }
                        }
                        Text("Ukuran teks Arab · ${settings.arabicFontSizeSp} sp", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = settings.arabicFontSizeSp.toFloat(),
                            onValueChange = { onArabicFontSizeChange(it.roundToInt()) },
                            valueRange = 22f..44f, steps = 10, modifier = Modifier.testTag("arabic_font_slider")
                        )
                        Text("Ukuran terjemahan dan Latin · ${settings.translationFontSizeSp} sp",
                            style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = settings.translationFontSizeSp.toFloat(),
                            onValueChange = { onTranslationFontSizeChange(it.roundToInt()) },
                            valueRange = 12f..22f, steps = 9, modifier = Modifier.testTag("translation_font_slider")
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsLabel(Icons.Outlined.Spellcheck, "Tampilkan Latin", modifier = Modifier.weight(1f))
                            SettingsSwitch(settings.showLatin, onShowLatinChange, "Tampilkan Latin",
                                Modifier.testTag("toggle_latin"))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsLabel(Icons.Outlined.Audiotrack, "Putar Beruntun Otomatis", modifier = Modifier.weight(1f))
                            SettingsSwitch(settings.autoNextAudio, onAutoNextAudioChange, "Putar Beruntun Otomatis",
                                Modifier.testTag("toggle_auto_next_audio"))
                        }
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.SYSTEM,
                            onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                            label = { Text("Ikuti tema sistem") },
                            modifier = Modifier.testTag("theme_chip_system")
                        )
                        SettingsLabel(Icons.Outlined.CloudDone, "Penyimpanan Offline",
                            "$cachedSurahCount dari 114 surah tersimpan di perangkat.")
                        LinearProgressIndicator(
                            progress = { (cachedSurahCount / 114f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArabicSizeSelector(fontSize: Int, onSizeChange: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    val selectedPreset = ArabicSizePresets.minBy { abs(it - fontSize) }
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, shape),
        color = scheme.surface,
        shape = shape,
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ArabicSizePresets.forEachIndexed { index, size ->
                val selected = selectedPreset == size
                val optionShape = RoundedCornerShape(10.dp)
                Box(
                    Modifier.weight(1f).heightIn(min = 42.dp).clip(optionShape)
                        .then(if (selected) Modifier.background(CopperGradient) else Modifier)
                        .selectable(selected, role = Role.RadioButton, onClick = { onSizeChange(size) })
                        .semantics {
                            contentDescription = "Ukuran ${if (selected) fontSize else size} sp"
                        }
                        .testTag("arabic_size_$size"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        listOf("A", "A◊", "A⁺⁺", "A⁺⁺⁺")[index],
                        fontFamily = FontFamily.Serif,
                        fontSize = (18 + index * 2).sp,
                        color = if (selected) HeaderIvory else scheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    subtitle: String,
    artwork: Int,
    mode: ThemeMode,
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val selected = mode == currentMode
    val shape = RoundedCornerShape(13.dp)
    Box(modifier) {
        Surface(
            Modifier.fillMaxWidth().shadow(3.dp, shape).clip(shape)
                .selectable(selected, role = Role.RadioButton, onClick = { onSelect(mode) })
                .testTag("theme_chip_${mode.name.lowercase()}"),
            shape = shape,
            color = scheme.surface,
            border = BorderStroke(1.dp, if (selected) scheme.primary else scheme.outlineVariant)
        ) {
            Column(
                Modifier.padding(7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painterResource(artwork), null,
                    Modifier.fillMaxWidth().aspectRatio(1.56f).clip(RoundedCornerShape(9.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    title, Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    "($subtitle)", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                    textAlign = TextAlign.Center
                )
            }
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-2).dp)
                    .size(24.dp).clip(CircleShape).background(CopperGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = HeaderIvory)
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
