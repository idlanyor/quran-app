package com.irengcloud.quran.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET

data class Dua(
    val id: Int, val name: String, val group: String, val arabic: String,
    val latin: String, val translation: String, val reference: String, val tags: List<String>
)

data class DuaCollection(val items: List<Dua>, val refreshFailed: Boolean = false)

interface DuaApi {
    @GET("api/doa") suspend fun all(): Response<ResponseBody>
}

class DuaRepository(context: Context, private val api: DuaApi = createApi()) {
    private val cache = AtomicFile(File(context.filesDir, "daily_duas.json"))

    suspend fun load(refresh: Boolean = false): DuaCollection = withContext(Dispatchers.IO) {
        val cached = runCatching { parse(cache.readFully().toString(Charsets.UTF_8)) }.getOrNull()
        if (!refresh && cached != null) return@withContext DuaCollection(cached)
        try {
            val response = api.all()
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                response.body()?.close()
                throw IOException("Doa belum dapat dimuat. Coba lagi nanti.")
            }
            val raw = response.body()?.use { it.string() } ?: throw IOException("Respons doa kosong.")
            val items = parse(raw)
            val output = cache.startWrite()
            try {
                output.write(raw.toByteArray(Charsets.UTF_8))
                cache.finishWrite(output)
            } catch (e: Exception) {
                cache.failWrite(output)
                throw e
            }
            DuaCollection(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (cached != null) DuaCollection(cached, refreshFailed = true) else throw e
        }
    }

    companion object {
        private fun createApi(): DuaApi = Retrofit.Builder().baseUrl("https://equran.id/")
            .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS).build()).build().create(DuaApi::class.java)

        fun parse(raw: String): List<Dua> {
            val root = JSONObject(raw)
            require(root.getString("status") == "success") { "Respons doa tidak valid." }
            val data = root.getJSONArray("data")
            return (0 until data.length()).map { index ->
                val item = data.getJSONObject(index)
                val tags = item.optJSONArray("tag")
                fun optional(key: String) = if (item.isNull(key)) "" else item.optString(key).trim()
                Dua(item.getInt("id"), item.getString("nama").trim(), optional("grup"),
                    item.getString("ar").trim(), optional("tr"), optional("idn"),
                    optional("tentang"),
                    if (tags == null) emptyList() else (0 until tags.length()).map { tags.getString(it) })
                    .also { require(it.id > 0 && it.name.isNotBlank() && it.arabic.isNotBlank()) }
            }.also { items ->
                require(items.isNotEmpty() && items.map { it.id }.distinct().size == items.size)
            }
        }
    }
}

fun filterDuas(items: List<Dua>, query: String, group: String?): List<Dua> {
    val words = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return items.filter { dua ->
        (group == null || dua.group == group) && words.all { word ->
            listOf(dua.name, dua.translation, dua.latin, dua.arabic, dua.group)
                .plus(dua.tags).any { it.contains(word, ignoreCase = true) }
        }
    }
}
