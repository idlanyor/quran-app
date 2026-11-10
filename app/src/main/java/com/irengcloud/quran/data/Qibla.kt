package com.irengcloud.quran.data

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class QiblaLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracyMeters: Float,
    val updatedAt: Long,
    val regionProvince: String = "",
    val regionCity: String = ""
) {
    val isManualRegion: Boolean get() = regionCity.isNotBlank()
}

fun normalizeDegrees(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

/** Initial great-circle bearing to the Kaaba, clockwise from true north. */
fun qiblaBearing(latitude: Double, longitude: Double): Double? {
    require(latitude.isFinite() && latitude in -90.0..90.0)
    require(longitude.isFinite() && longitude in -180.0..180.0)
    val start = Math.toRadians(latitude)
    val destination = Math.toRadians(21.4225)
    val delta = Math.toRadians(39.8262 - longitude)
    val y = sin(delta) * cos(destination)
    val x = cos(start) * sin(destination) - sin(start) * cos(destination) * cos(delta)
    if (abs(x) + abs(y) < 1e-12) return null
    return normalizeDegrees(Math.toDegrees(atan2(y, x)))
}

/** Shortest signed turn: negative is left, positive is right. */
fun shortestTurn(target: Double, heading: Double): Double =
    (normalizeDegrees(target - heading) + 180.0) % 360.0 - 180.0
