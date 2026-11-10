package com.irengcloud.quran.ui.components

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/** Shared, bounded artwork cache. Decoding never blocks composition or navigation. */
private object ArtworkBitmaps {
    private val cache = object : LruCache<String, ImageBitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    private val decodeLock = Mutex()

    fun peek(key: String): ImageBitmap? = cache.get(key)

    suspend fun load(resources: Resources, resource: Int, width: Int, key: String): ImageBitmap =
        withContext(Dispatchers.IO) {
            decodeLock.withLock {
                cache.get(key)?.let { return@withLock it }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
                BitmapFactory.decodeResource(resources, resource, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= width) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample; inScaled = false }
                val bitmap = checkNotNull(BitmapFactory.decodeResource(resources, resource, options))
                    .asImageBitmap()
                cache.put(key, bitmap)
                bitmap
            }
        }
}

@Composable
fun rememberArtwork(@DrawableRes resource: Int, targetWidth: Dp? = null, delayMillis: Long = 0): State<ImageBitmap?> {
    val resources = LocalContext.current.resources
    val density = LocalDensity.current.density
    val widthDp = targetWidth?.value ?: LocalConfiguration.current.screenWidthDp.toFloat()
    // Buckets avoid decoding again for tiny window or density changes.
    val width = (ceil(widthDp * density / 64f).toInt() * 64).coerceIn(64, 1536)
    val key = "$resource:$width"
    return produceState(ArtworkBitmaps.peek(key), resources, key) {
        if (delayMillis > 0 && ArtworkBitmaps.peek(key) == null) delay(delayMillis)
        value = ArtworkBitmaps.load(resources, resource, width, key)
    }
}

@Composable
fun AppArtwork(
    @DrawableRes resource: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = 1f,
    targetWidth: Dp? = null,
    colorFilter: ColorFilter? = null
) {
    val artwork = rememberArtwork(resource, targetWidth).value
    if (artwork != null) {
        Image(artwork, contentDescription = null, modifier = modifier,
            contentScale = contentScale, alpha = alpha, colorFilter = colorFilter)
    }
}

// These illustrations share pale paper and copper lines. Use the green channel
// to fade the paper to transparent while retaining the dark ink and its color.
internal val TransparentArtworkPaper = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, -18f, 0f, 0f, 3960f
)))
