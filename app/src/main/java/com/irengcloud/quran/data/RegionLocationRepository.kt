package com.irengcloud.quran.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.AtomicFile
import java.io.File
import java.util.Locale
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/** Resolves and caches a selected administrative area's representative location. */
class RegionLocationRepository(context: Context) {
    private val context = context.applicationContext
    private val directory = File(this.context.filesDir, "qibla_regions")

    suspend fun resolve(province: String, city: String): QiblaLocation = withContext(Dispatchers.IO) {
        require(province.isNotBlank() && city.isNotBlank())
        val key = MessageDigest.getInstance("SHA-256").digest("$province|$city".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val file = AtomicFile(File(directory, "$key.json"))
        val cached = runCatching {
            val json = JSONObject(file.readFully().toString(Charsets.UTF_8))
            require(json.getString("province") == province && json.getString("city") == city)
            val lat = json.getDouble("latitude"); val lon = json.getDouble("longitude")
            require(lat.isFinite() && lat in -90.0..90.0 && lon.isFinite() && lon in -180.0..180.0)
            QiblaLocation(lat, lon, 0.0, 0f, json.getLong("updatedAt"), province, city)
        }.getOrNull()
        if (cached != null) return@withContext cached

        check(Geocoder.isPresent()) { "Layanan pencarian titik wilayah belum tersedia di HP ini." }
        val name = city.replace(Regex("^Kab\\.\\s*", RegexOption.IGNORE_CASE), "Kabupaten ")
        val geocoder = Geocoder(context, Locale.forLanguageTag("id-ID"))
        val addresses: List<Address> = if (Build.VERSION.SDK_INT >= 33) {
            withTimeout(12_000) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName("$name, $province, Indonesia", 5, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }
                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resumeWithException(
                                IllegalStateException("Titik wilayah belum dapat dimuat. Periksa koneksi lalu coba lagi."))
                        }
                    })
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName("$name, $province, Indonesia", 5).orEmpty()
        }
        ensureActive()
        val address = addresses.firstOrNull { address ->
            address.hasLatitude() && address.hasLongitude() && address.countryCode.equals("ID", true) &&
                matchCity(listOfNotNull(address.subAdminArea, address.locality, address.featureName), province, listOf(city)) == city
        } ?: error("Titik $city belum ditemukan. Coba perbarui titik wilayah.")
        val result = QiblaLocation(address.latitude, address.longitude, 0.0, 0f,
            System.currentTimeMillis(), province, city)
        val json = JSONObject().put("province", province).put("city", city)
            .put("latitude", result.latitude).put("longitude", result.longitude).put("updatedAt", result.updatedAt)
        directory.mkdirs()
        val output = file.startWrite()
        try { output.write(json.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(output) }
        catch (error: Exception) { file.failWrite(output); throw error }
        result
    }
}
