package com.irengcloud.quran.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irengcloud.quran.data.local.KhatamTargetMode
import com.irengcloud.quran.data.local.LastReadState
import com.irengcloud.quran.data.local.ReaderSettingsState
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

const val TOTAL_QURAN_AYATS = 6236
const val TOTAL_QURAN_SURAHS = 114

/**
 * Reusable Khatam Tracker component allowing users to:
 * 1. Switch between Daily Verses (Ayat) target mode and Daily Chapters (Surah) target mode.
 * 2. Customize their daily target number using presets, +/- steppers, or an interactive slider.
 * 3. View animated Daily Goal and Overall 30-Juz Khatam progress indicators & estimated days to finish.
 * 4. Log daily reading progress for verses or chapters with one tap.
 */
@Composable
fun KhatamTrackerComponent(
    readerSettings: ReaderSettingsState,
    todayReadAyats: Int,
    todayReadSurahs: Int,
    lastRead: LastReadState,
    onTargetModeChange: (KhatamTargetMode) -> Unit,
    onUpdateDailyTargetAyat: (Int) -> Unit,
    onUpdateDailyTargetSurah: (Int) -> Unit,
    onLogCustomAyats: (Int) -> Unit,
    onLogCustomSurahs: (Int) -> Unit,
    onResetKhatam: () -> Unit,
    onContinueReading: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetConfirmation by remember { mutableStateOf(false) }
    var pendingLog by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            shape = RoundedCornerShape(26.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Mulai Putaran Khatam Baru?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Text(
                    text = "Progres khatam dan riwayat harian putaran ini akan diatur ulang ke awal. Posisi terakhir dibaca dan bookmark ayat Anda tetap aman tersimpan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        onResetKhatam()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mulai Ulang", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmation = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    pendingLog?.let { (count, chapters) ->
        AlertDialog(
            onDismissRequest = { pendingLog = null },
            shape = RoundedCornerShape(26.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Catat Bacaan Berurutan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Text(
                    text = "Tandai $count ${if (chapters) "surah" else "ayat"} berikutnya sudah dibaca, dimulai dari surah pertama yang belum selesai dalam putaran ini. Jumlah ayat dihitung otomatis sesuai surah yang dibaca.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingLog = null
                        if (chapters) onLogCustomSurahs(count) else onLogCustomAyats(count)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sudah Dibaca", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingLog = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    val isChapterMode = readerSettings.khatamTargetMode == KhatamTargetMode.CHAPTERS

    val totalCompletedUnits = if (isChapterMode) {
        readerSettings.totalKhatamSurahCompleted
    } else {
        readerSettings.totalKhatamAyatCompleted
    }
    val totalGoalUnits = if (isChapterMode) TOTAL_QURAN_SURAHS else TOTAL_QURAN_AYATS
    val unitLabel = readerSettings.khatamTargetMode.unitLabel

    val todayCompletedUnits = if (isChapterMode) todayReadSurahs else todayReadAyats
    val dailyTargetUnits = if (isChapterMode) {
        readerSettings.dailyKhatamTargetSurah.coerceAtLeast(1)
    } else {
        readerSettings.dailyKhatamTargetAyat.coerceAtLeast(1)
    }

    val rawOverallProgress = (totalCompletedUnits.toFloat() / totalGoalUnits).coerceIn(0f, 1f)
    val animatedOverallProgress by animateFloatAsState(
        targetValue = rawOverallProgress,
        animationSpec = tween(durationMillis = 450),
        label = "overallKhatamProgress"
    )

    val rawDailyProgress = (todayCompletedUnits.toFloat() / dailyTargetUnits).coerceIn(0f, 1f)
    val animatedDailyProgress by animateFloatAsState(
        targetValue = rawDailyProgress,
        animationSpec = tween(durationMillis = 450),
        label = "dailyKhatamProgress"
    )

    val percentageText = String.format(Locale.US, "%.1f%%", rawOverallProgress * 100f)
    val remainingUnits = (totalGoalUnits - totalCompletedUnits).coerceAtLeast(0)
    val estimatedDaysLeft = if (dailyTargetUnits > 0) {
        ceil(remainingUnits.toDouble() / dailyTargetUnits.toDouble()).toInt()
    } else 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("khatam_tracker_component"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Switcher: Verses (Ayat) vs Chapters (Surah)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KhatamTargetMode.entries.forEach { mode ->
                    val selected = readerSettings.khatamTargetMode == mode
                    Surface(
                        onClick = { onTargetModeChange(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("khatam_mode_${mode.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (mode == KhatamTargetMode.VERSES) {
                                    Icons.Outlined.TrackChanges
                                } else {
                                    Icons.AutoMirrored.Outlined.MenuBook
                                },
                                contentDescription = null,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            // Overall Khatam Progress Ring & Stats
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isChapterMode) "PROGRES 114 SURAH" else "PROGRES 6.236 AYAT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "$totalCompletedUnits / $totalGoalUnits $unitLabel",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (remainingUnits == 0) {
                                "Alhamdulillah, putaran Khatam telah selesai!"
                            } else {
                                "Estimasi khatam ~$estimatedDaysLeft hari ($dailyTargetUnits ${unitLabel.lowercase()}/hari)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .testTag("khatam_overall_progress_ring"),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedOverallProgress },
                            modifier = Modifier.size(72.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            strokeWidth = 7.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = percentageText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Daily Target Progress Indicator Card
            Surface(
                color = if (todayCompletedUnits >= dailyTargetUnits) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (todayCompletedUnits >= dailyTargetUnits) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("khatam_daily_progress_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (todayCompletedUnits >= dailyTargetUnits) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (todayCompletedUnits >= dailyTargetUnits) Icons.Default.CheckCircle else Icons.Outlined.EmojiEvents,
                                    contentDescription = null,
                                    tint = if (todayCompletedUnits >= dailyTargetUnits) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Target Tilawah Hari Ini",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (todayCompletedUnits >= dailyTargetUnits) "Target hari ini tercapai! Masya Allah" else "Selesaikan target tilawah Anda",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = if (todayCompletedUnits >= dailyTargetUnits) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$todayCompletedUnits / $dailyTargetUnits $unitLabel (${(rawDailyProgress * 100).roundToInt()}%)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (todayCompletedUnits >= dailyTargetUnits) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedDailyProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("khatam_daily_progress_bar"),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Daily Target Customization (Presets + Stepper + Slider)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Atur Target Harian ($unitLabel/Hari)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isChapterMode) "Jumlah surah yang dibaca per hari" else "Jumlah ayat yang dibaca per hari",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Stepper (- / +) for precise daily target adjustment
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isChapterMode) {
                                    onUpdateDailyTargetSurah((dailyTargetUnits - 1).coerceAtLeast(1))
                                } else {
                                    val step = if (dailyTargetUnits > 20) 5 else 1
                                    onUpdateDailyTargetAyat((dailyTargetUnits - step).coerceAtLeast(1))
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("khatam_target_minus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Kurangi Target Harian",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "$dailyTargetUnits",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isChapterMode) {
                                    onUpdateDailyTargetSurah((dailyTargetUnits + 1).coerceAtMost(30))
                                } else {
                                    val step = if (dailyTargetUnits >= 20) 5 else 1
                                    onUpdateDailyTargetAyat((dailyTargetUnits + step).coerceAtMost(600))
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("khatam_target_plus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Target Harian",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (isChapterMode) {
                    Slider(
                        value = dailyTargetUnits.toFloat().coerceIn(1f, 30f),
                        onValueChange = { onUpdateDailyTargetSurah(it.roundToInt()) },
                        valueRange = 1f..30f,
                        steps = 28,
                        modifier = Modifier.testTag("khatam_target_surah_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 4, 5).forEach { target ->
                            val selected = dailyTargetUnits == target
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateDailyTargetSurah(target) },
                                label = { Text("$target Surah") },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("khatam_surah_preset_$target")
                            )
                        }
                    }
                } else {
                    Slider(
                        value = dailyTargetUnits.toFloat().coerceIn(1f, 600f),
                        onValueChange = { onUpdateDailyTargetAyat(it.roundToInt()) },
                        valueRange = 1f..600f,
                        modifier = Modifier.testTag("khatam_target_ayat_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 20, 50, 100).forEach { target ->
                            val selected = dailyTargetUnits == target
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateDailyTargetAyat(target) },
                                label = { Text("$target Ayat") },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("khatam_target_chip_$target")
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Quick Log Progress Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Catat Bacaan Berurutan:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tandai ayat/surah yang telah dibaca secara berurutan:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isChapterMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1 to "+1 Surah", 2 to "+2 Surah", 4 to "+4 Surah").forEach { (count, label) ->
                            FilledTonalButton(
                                onClick = { pendingLog = count to true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("log_${count}_surah_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5 to "+5 Ayat", 10 to "+10 Ayat", 20 to "+20 Ayat").forEach { (count, label) ->
                            FilledTonalButton(
                                onClick = { pendingLog = count to false },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("log_${count}_ayat_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // Continue Reading & Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onContinueReading,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("khatam_continue_reading_button"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lanjut Baca QS. ${lastRead.surahNamaLatin}:${lastRead.ayatNomor}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (readerSettings.totalKhatamAyatCompleted > 0 || readerSettings.totalKhatamSurahCompleted > 0) {
                    OutlinedButton(
                        onClick = { showResetConfirmation = true },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("reset_khatam_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Progres Khatam",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
