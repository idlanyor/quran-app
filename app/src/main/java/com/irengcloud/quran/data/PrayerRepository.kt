package com.irengcloud.quran.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class PrayerDay(val date: String, val label: String, val times: List<Pair<String, String>>)
data class PrayerMonth(val province: String, val city: String, val days: List<PrayerDay>, val fromCache: Boolean = false)

fun prayerTimeZone(province: String): TimeZone = TimeZone.getTimeZone(when {
    province.startsWith("Papua") || province.startsWith("Maluku") -> "Asia/Jayapura"
    province in listOf("Bali", "Nusa Tenggara Barat", "Nusa Tenggara Timur", "Kalimantan Selatan",
        "Kalimantan Timur", "Kalimantan Utara", "Gorontalo") || province.startsWith("Sulawesi") -> "Asia/Makassar"
    else -> "Asia/Jakarta"
})

fun normalizedRegion(name: String): String = name.lowercase(Locale.ROOT)
    .replace(Regex("\\b(kabupaten|kab|kota|administrasi)\\b"), "")
    .replace(Regex("[^a-z0-9]"), "")

fun matchProvince(name: String, options: List<String>): String? {
    val alias = when {
        name.contains("Jakarta", true) -> "DKI Jakarta"
        name.contains("Yogyakarta", true) -> "D.I. Yogyakarta"
        name.contains("Aceh", true) -> "Aceh"
        else -> name
    }
    return options.firstOrNull { normalizedRegion(it) == normalizedRegion(alias) }
}

fun matchCity(names: List<String>, province: String, options: List<String>): String? {
    fun typed(name: String) = name.lowercase(Locale.ROOT).replace("kabupaten", "kab")
        .replace(Regex("[^a-z0-9]"), "")
    names.forEach { name ->
        options.firstOrNull { typed(it) == typed(name) }?.let { return it }
        options.filter { normalizedRegion(it) == normalizedRegion(name) }.singleOrNull()?.let { return it }
    }
    // EQuran groups Jakarta's administrative cities under a single "Kota Jakarta".
    if (province == "DKI Jakarta" && names.any { it.contains("Jakarta", true) }) {
        return options.firstOrNull { it == "Kota Jakarta" }
    }
    return null
}

interface PrayerApi {
    @GET("api/v2/shalat/provinsi") suspend fun provinces(): Response<ResponseBody>
    @POST("api/v2/shalat/kabkota") suspend fun cities(@Body body: RequestBody): Response<ResponseBody>
    @POST("api/v2/shalat") suspend fun month(@Body body: RequestBody): Response<ResponseBody>
}

class PrayerRepository(context: Context, private val api: PrayerApi = createApi()) {
    private val directory = File(context.filesDir, "prayer_cache")
    private fun body(json: JSONObject) = json.toString().toRequestBody("application/json".toMediaType())
    private fun file(key: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$hash.json")
    }
    private fun read(key: String): String? = runCatching { AtomicFile(file(key)).readFully().toString(Charsets.UTF_8) }.getOrNull()
    private fun write(key: String, text: String) {
        directory.mkdirs()
        val atomic = AtomicFile(file(key))
        val output = atomic.startWrite()
        try { output.write(text.toByteArray()); atomic.finishWrite(output) }
        catch (e: Exception) { atomic.failWrite(output); throw e }
    }
    private fun response(response: Response<ResponseBody>): String {
        val raw = response.body()?.use { it.string() }
        if (!response.isSuccessful || raw == null) {
            response.errorBody()?.close()
            throw IOException("Jadwal belum dapat dimuat (${response.code()}). Coba lagi nanti.")
        }
        val json = JSONObject(raw)
        if (json.optInt("code") != 200) throw IOException(json.optString("message", "Respons jadwal tidak valid"))
        return raw
    }
    private suspend fun list(key: String, request: suspend () -> Response<ResponseBody>): List<String> = withContext(Dispatchers.IO) {
        fun parse(raw: String): List<String> {
            val data = JSONObject(raw).getJSONArray("data")
            return (0 until data.length()).map { data.getString(it) }.also { require(it.isNotEmpty()) }
        }
        read(key)?.let { cached -> runCatching { parse(cached) }.getOrNull()?.let { return@withContext it } }
        val raw = response(request())
        val result = parse(raw)
        write(key, raw)
        result
    }
    suspend fun provinces(): List<String> = list("provinces") { api.provinces() }
    suspend fun cities(province: String): List<String> = list("cities:$province") {
        api.cities(body(JSONObject().put("provinsi", province)))
    }
    suspend fun month(province: String, city: String, year: Int, month: Int, refresh: Boolean = false): PrayerMonth = withContext(Dispatchers.IO) {
        val key = "$province|$city|$year|$month"
        val cached = read(key)?.let { runCatching { parseMonth(it, province, city, year, month) }.getOrNull() }
        if (!refresh && cached != null) return@withContext cached.copy(fromCache = true)
        try {
            val raw = response(api.month(body(JSONObject().put("provinsi", province).put("kabkota", city)
                .put("bulan", month).put("tahun", year))))
            val result = parseMonth(raw, province, city, year, month)
            write(key, raw)
            result
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { cached?.copy(fromCache = true) ?: throw e }
    }

    companion object {
        private fun createApi(): PrayerApi = Retrofit.Builder().baseUrl("https://equran.id/")
            .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS).build()).build().create(PrayerApi::class.java)

        fun parseMonth(raw: String, province: String, city: String, year: Int, month: Int): PrayerMonth {
            val root = JSONObject(raw)
            require(root.getInt("code") == 200)
            val data = root.getJSONObject("data")
            require(data.getString("provinsi") == province && data.getString("kabkota") == city)
            require(data.getInt("tahun") == year && data.getInt("bulan") == month)
            val list = data.getJSONArray("jadwal")
            val fields = listOf("Imsak" to "imsak", "Subuh" to "subuh", "Terbit" to "terbit", "Duha" to "dhuha", "Zuhur" to "dzuhur",
                "Asar" to "ashar", "Magrib" to "maghrib", "Isya" to "isya")
            val timesPattern = Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]")
            val expectedDays = Calendar.getInstance().apply {
                set(year, month - 1, 1)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)
            val days = (0 until list.length()).map { index ->
                val day = list.getJSONObject(index)
                val date = day.getString("tanggal_lengkap")
                val number = day.getInt("tanggal")
                require(number in 1..expectedDays && date == String.format(Locale.US, "%04d-%02d-%02d", year, month, number))
                PrayerDay(date, day.getString("hari"), fields.map { (label, key) ->
                    label to day.getString(key).also { require(timesPattern.matches(it)) }
                })
            }
            require(days.map { it.date }.toSet().size == expectedDays)
            return PrayerMonth(province, city, days.sortedBy { it.date })
        }
    }
}
