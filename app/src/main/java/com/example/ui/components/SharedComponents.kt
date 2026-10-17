package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AudioPlaybackState
import com.example.ui.TafsirSheetUiState
import com.example.ui.theme.AmiriQuranFontFamily
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimalist 8-point Rub el Hizb geometric badge for Surah or Ayat numbers.
 */
@Composable
fun VerseNumberBadge(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    isHighlighted: Boolean = false
) {
    val primaryColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    }
    val containerColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = this.size.minDimension / 2f * 0.88f
            val center = this.center
            val points = 8
            val path = Path()
            for (i in 0 until points * 2) {
                val r = if (i % 2 == 0) radius else radius * 0.78f
                val angle = Math.toRadians((i * 360.0 / (points * 2)) - 90.0)
                val x = center.x + (r * cos(angle)).toFloat()
                val y = center.y + (r * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path = path, color = containerColor)
            drawPath(path = path, color = primaryColor, style = Stroke(width = 1.6.dp.toPx()))
        }
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}

/**
 * Floating Mini Audio Player Bar for Murottal playback.
 */
@Composable
fun MiniAudioPlayerBar(
    audioState: AudioPlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    onJumpToSurah: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = audioState.isActive,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("mini_audio_player_bar"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        if (audioState.surahNomor > 0) {
                            onJumpToSurah(audioState.surahNomor, audioState.currentAyatNomor)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (audioState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Murottal Aktif",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "QS. ${audioState.surahNamaLatin} : Ayat ${audioState.currentAyatNomor}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = audioState.errorMessage
                            ?: if (audioState.isBuffering) "Memuat audio murottal…"
                            else "Murottal Syaikh Mishary Rasyid",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (audioState.errorMessage != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.testTag("audio_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Ayat Sebelumnya",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("audio_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (audioState.isPlaying) "Jeda Audio" else "Putar Audio",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("audio_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Ayat Berikutnya",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("audio_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Pemutar Audio",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Modal Bottom Sheet for displaying Kemenag RI Tafsir (`GET /api/v1/quran/tafsir/{nomor}`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirBottomSheet(
    state: TafsirSheetUiState,
    arabicFontSizeSp: Int,
    onDismiss: () -> Unit,
    onCopyTafsir: (String) -> Unit
) {
    if (!state.isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("tafsir_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tafsir Kemenag RI",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "QS. ${state.surahNamaLatin} (${state.surahNomor}) : Ayat ${state.nomorAyat}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_tafsir_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Tafsir"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Verse card inside Tafsir Sheet
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (state.teksArab.isNotBlank()) {
                        Text(
                            text = state.teksArab,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = AmiriQuranFontFamily,
                                fontSize = (arabicFontSizeSp - 2).coerceAtLeast(22).sp,
                                lineHeight = ((arabicFontSizeSp - 2) * 1.95f).sp,
                                textDirection = TextDirection.Rtl
                            ),
                            textAlign = TextAlign.Right,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Text(
                        text = "\"${state.teksIndonesia}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.Ltr
                        ),
                        textAlign = TextAlign.Left,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Uraian & Penjelasan Tafsir",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Memuat Tafsir Kemenag RI dari server…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = state.teksTafsir,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = {
                        val fullCopy = buildString {
                            appendLine("QS. ${state.surahNamaLatin} (${state.surahNomor}): ${state.nomorAyat}")
                            appendLine(state.teksArab)
                            appendLine("\"${state.teksIndonesia}\"")
                            appendLine()
                            appendLine("Tafsir Kemenag RI:")
                            append(state.teksTafsir)
                        }
                        clipboard.setText(AnnotatedString(fullCopy))
                        onCopyTafsir("Tafsir ayat ${state.nomorAyat} disalin ke papan klip")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("copy_tafsir_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salin Ayat & Tafsir")
                }
            }
        }
    }
}
