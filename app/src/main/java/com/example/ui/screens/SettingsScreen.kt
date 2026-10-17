package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ReaderSettingsState
import com.example.data.local.ThemeMode
import com.example.ui.components.VerseNumberBadge
import com.example.ui.theme.AmiriQuranFontFamily
import kotlin.math.roundToInt

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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 120.dp)
                .testTag("settings_screen_column"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "Pengaturan Bacaan",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Sesuaikan ukuran font Al-Qur'an, tampilan terjemahan, audio murottal, dan mode warna",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Live Verse Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_live_preview_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VerseNumberBadge(number = 1, size = 34.dp, isHighlighted = true)
                            Text(
                                text = "Pratinjau Tampilan Ayat",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Arab ${settings.arabicFontSizeSp}sp",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = settings.arabicFontSizeSp.sp,
                            lineHeight = (settings.arabicFontSizeSp * 2.0f).sp,
                            textDirection = TextDirection.Rtl
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (settings.showLatin) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Bismillāhir-raḥmānir-raḥīm(i).",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = (settings.translationFontSizeSp - 1).coerceAtLeast(12).sp,
                                textDirection = TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (settings.showTranslation) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = settings.translationFontSizeSp.sp,
                                lineHeight = (settings.translationFontSizeSp * 1.55f).sp,
                                textDirection = TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Font Size & Text Visibility Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Tipografi & Tampilan Ayat",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ukuran Font Arab (Adobe Naskh)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${settings.arabicFontSizeSp} sp",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.arabicFontSizeSp.toFloat(),
                            onValueChange = { onArabicFontSizeChange(it.roundToInt()) },
                            valueRange = 22f..44f,
                            steps = 10,
                            modifier = Modifier.testTag("arabic_font_slider")
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ukuran Font Terjemahan & Latin",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${settings.translationFontSizeSp} sp",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.translationFontSizeSp.toFloat(),
                            onValueChange = { onTranslationFontSizeChange(it.roundToInt()) },
                            valueRange = 12f..22f,
                            steps = 9,
                            modifier = Modifier.testTag("translation_font_slider")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tampilkan Teks Transliterasi Latin",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Membantu pelafalan bacaan Arab secara tepat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.showLatin,
                            onCheckedChange = onShowLatinChange,
                            modifier = Modifier.testTag("toggle_show_latin")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tampilkan Terjemahan Bahasa Indonesia",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Terjemahan resmi Kementerian Agama RI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.showTranslation,
                            onCheckedChange = onShowTranslationChange,
                            modifier = Modifier.testTag("toggle_show_translation")
                        )
                    }
                }
            }

            // Audio & Theme Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Pemutar Audio & Tema Visual",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Putar Beruntun Otomatis (Auto-Next Ayat)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Otomatis lanjut ke ayat berikutnya saat murottal selesai",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoNextAudio,
                            onCheckedChange = onAutoNextAudioChange,
                            modifier = Modifier.testTag("toggle_auto_next_audio")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "Mode Tema Aplikasi:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            ThemeMode.SYSTEM to "Otomatis Sistem",
                            ThemeMode.LIGHT to "Terang Bersih",
                            ThemeMode.DARK to "Gelap Tenang"
                        )
                        modes.forEach { (mode, label) ->
                            val selected = settings.themeMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Offline Storage Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Status Penyimpanan Offline & Sumber Data",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• $cachedSurahCount dari 114 Surah telah tersimpan di database lokal perangkat untuk akses tanpa internet.\n• Sumber API: https://apiku.irengcloud.com (Surah, Ayat, Tafsir Kemenag RI, dan Pencarian Terjemahan).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
