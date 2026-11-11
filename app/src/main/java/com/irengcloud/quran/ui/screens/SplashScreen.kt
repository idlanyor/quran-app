package com.irengcloud.quran.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irengcloud.quran.R
import com.irengcloud.quran.ui.components.AppArtwork
import kotlinx.coroutines.delay

private val SplashNight = Color(0xFF1A1B2E)
private val SplashGoldGlow = Color(0xFFE5AF83)
private val SplashTitleIvory = Color(0xFFFFF4E6)
private val SplashTaglineCream = Color(0xFFEAD7C3)

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startEntrance by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startEntrance = true
        delay(2300)
        onFinished()
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (startEntrance) 1f else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (startEntrance) 1f else 0.90f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )

    val pulseTransition = rememberInfiniteTransition(label = "splash_glow_pulse")
    val glowPulse by pulseTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashNight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        AppArtwork(
            resource = R.drawable.splash_bg,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-34).dp)
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(210.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft warm golden radial halo behind the emblem (matching Option 1)
                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .graphicsLayer {
                            scaleX = glowPulse
                            scaleY = glowPulse
                        }
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SplashGoldGlow.copy(alpha = 0.34f),
                                SplashGoldGlow.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
                }

                // Subtle outer bloom layer of the vector emblem
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .size(196.dp)
                        .graphicsLayer { alpha = 0.28f },
                    colorFilter = ColorFilter.tint(SplashGoldGlow)
                )

                // Crisp main vector emblem (Golden Mihrab + 8-Point Star + Ivory Open Qur'an)
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier.size(188.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 31.sp,
                    lineHeight = 38.sp,
                    letterSpacing = 0.2.sp
                ),
                color = SplashTitleIvory,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.25.sp
                ),
                color = SplashTaglineCream,
                textAlign = TextAlign.Center
            )
        }
    }
}
