package com.irengcloud.quran.ui.components

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface as DisplaySurface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.irengcloud.quran.data.QiblaLocation
import com.irengcloud.quran.data.normalizeDegrees
import com.irengcloud.quran.data.qiblaBearing
import com.irengcloud.quran.data.shortestTurn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class CompassReading(
    val heading: Double? = null,
    val supported: Boolean = true,
    val unreliable: Boolean = false,
    val flat: Boolean = true
)

@Composable
private fun compassReading(location: QiblaLocation): CompassReading {
    val context = LocalContext.current
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    val manager = remember(context) { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val vector = remember(manager) { manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val accelerometer = remember(manager) { manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val magnetometer = remember(manager) { manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    val available = vector != null || (accelerometer != null && magnetometer != null)
    var reading by remember(location, available) { mutableStateOf(CompassReading(supported = available)) }

    DisposableEffect(manager, owner, location) {
        val declination = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis()
        ).declination
        val rotation = FloatArray(9)
        val mapped = FloatArray(9)
        val orientation = FloatArray(3)
        var gravity: FloatArray? = null
        var magnetic: FloatArray? = null
        var filtered: Double? = null
        var unreliable = false
        var registered = false

        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    unreliable = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                    reading = reading.copy(unreliable = unreliable)
                }
            }

            override fun onSensorChanged(event: SensorEvent) {
                if (vector != null) {
                    SensorManager.getRotationMatrixFromVector(rotation, event.values)
                } else {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> gravity = event.values.copyOf()
                        Sensor.TYPE_MAGNETIC_FIELD -> magnetic = event.values.copyOf()
                    }
                    val g = gravity ?: return
                    val m = magnetic ?: return
                    if (!SensorManager.getRotationMatrix(rotation, null, g, m)) return
                }
                val axes = when (view.display?.rotation ?: DisplaySurface.ROTATION_0) {
                    DisplaySurface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    DisplaySurface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    DisplaySurface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                if (!SensorManager.remapCoordinateSystem(rotation, axes.first, axes.second, mapped)) return
                SensorManager.getOrientation(mapped, orientation)
                val raw = normalizeDegrees(Math.toDegrees(orientation[0].toDouble()) + declination)
                if (!raw.isFinite()) return
                filtered = filtered?.let { normalizeDegrees(it + shortestTurn(raw, it) * 0.2) } ?: raw
                val flat = abs(orientation[1]) < Math.PI / 4 && abs(orientation[2]) < Math.PI / 4
                if (reading.heading == null || abs(shortestTurn(filtered!!, reading.heading!!)) > 0.3 ||
                    reading.flat != flat || reading.unreliable != unreliable
                ) {
                    reading = CompassReading(filtered, true, unreliable, flat)
                }
            }
        }

        fun start() {
            if (!available || registered) return
            registered = if (vector != null) {
                manager.registerListener(listener, vector, SensorManager.SENSOR_DELAY_UI)
            } else {
                val first = manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                val second = manager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
                first && second
            }
            if (!registered) {
                manager.unregisterListener(listener)
                reading = CompassReading(supported = false)
            }
        }

        fun stop() {
            manager.unregisterListener(listener)
            registered = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            stop()
        }
    }
    return reading
}

@Composable
fun QiblaCompassCard(
    location: QiblaLocation?,
    locating: Boolean,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qibla_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Arah Kiblat",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kompas Ka'bah (Makkah)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (location != null) {
                    val bearing = remember(location) { qiblaBearing(location.latitude, location.longitude) }
                    if (bearing != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f°", bearing),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            if (location == null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Lokasi Belum Terdeteksi",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Gunakan GPS HP untuk menentukan arah kiblat yang presisi dari posisi Anda saat ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    enabled = !locating,
                    onClick = onRequestLocation,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (locating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Mendeteksi lokasi...")
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tentukan Lokasi Kiblat")
                    }
                }
            } else {
                val bearing = remember(location) { qiblaBearing(location.latitude, location.longitude) }
                val compass = compassReading(location)
                if (bearing == null) {
                    Text(
                        text = "Arah kiblat tidak dapat ditentukan dari titik ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    val heading = compass.heading
                    if (!compass.supported) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sensor kompas tidak tersedia di HP ini. Gunakan sudut patokan kiblat di atas (${String.format(Locale.getDefault(), "%.1f°", bearing)}) dengan kompas fisik.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else if (heading == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Menunggu pembacaan kompas...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Letakkan HP di permukaan mendatar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        val turn = shortestTurn(bearing, heading)
                        val aligned = abs(turn) <= 5 && compass.flat && !compass.unreliable
                        var unwrapped by remember { mutableStateOf(turn.toFloat()) }
                        LaunchedEffect(turn) {
                            unwrapped += shortestTurn(turn, unwrapped.toDouble()).toFloat()
                        }
                        val arrowAngle by animateFloatAsState(unwrapped, tween(180), label = "qiblaArrow")
                        val activeColor = if (aligned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        val ringColor = MaterialTheme.colorScheme.outlineVariant
                        val tickColor = MaterialTheme.colorScheme.outline

                        // Compass Dial Container
                        Box(
                            Modifier.size(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Dial background circle
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationZ = -heading.toFloat() }
                            ) {
                                Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                                    drawCircle(
                                        color = ringColor,
                                        style = Stroke(2.dp.toPx())
                                    )
                                    val radius = size.minDimension / 2f
                                    for (angle in 0 until 360 step 10) {
                                        val a = Math.toRadians(angle - 90.0)
                                        val isCardinal = angle % 90 == 0
                                        val length = if (isCardinal) 12.dp.toPx() else 5.dp.toPx()
                                        val strokeW = if (isCardinal) 2.dp.toPx() else 1.dp.toPx()
                                        val c = if (isCardinal) activeColor else tickColor
                                        drawLine(
                                            color = c,
                                            start = Offset(
                                                center.x + cos(a).toFloat() * (radius - length),
                                                center.y + sin(a).toFloat() * (radius - length)
                                            ),
                                            end = Offset(
                                                center.x + cos(a).toFloat() * radius,
                                                center.y + sin(a).toFloat() * radius
                                            ),
                                            strokeWidth = strokeW
                                        )
                                    }
                                }
                                Text(
                                    text = "U",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp)
                                )
                                Text(
                                    text = "T",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                                )
                                Text(
                                    text = "S",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                                )
                                Text(
                                    text = "B",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
                                )
                            }

                            // Center Qibla Needle
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Panah arah kiblat",
                                tint = activeColor,
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer { rotationZ = arrowAngle }
                            )
                        }

                        // Guidance Status Pill
                        Surface(
                            color = if (aligned) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (aligned) Icons.Default.CheckCircle else Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = if (aligned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = when {
                                        compass.unreliable -> "Akurasi kompas rendah. Gerakkan HP membentuk pola angka 8."
                                        !compass.flat -> "Pegang HP mendatar agar arah kompas presisi."
                                        aligned -> "HP sudah tepat mengarah ke Kiblat."
                                        else -> "Putar HP ${if (turn > 0) "ke kanan" else "ke kiri"} (± ${abs(turn.roundToInt())}°) sampai panah mengarah ke atas."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (aligned) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (aligned) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GPS: " + SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(location.updatedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            enabled = !locating,
                            onClick = onRequestLocation
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (locating) "Mendeteksi..." else "Perbarui Lokasi")
                        }
                    }
                }
            }
        }
    }
}
