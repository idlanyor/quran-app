package com.irengcloud.quran

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.irengcloud.quran.data.DuaApi
import com.irengcloud.quran.data.DuaRepository
import com.irengcloud.quran.data.filterDuas
import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

internal val duaFixture = """{"status":"success","data":[
    {"id":9,"nama":"Doa Sebelum Tidur","grup":"Tidur","ar":"اللَّهُمَّ","tr":"Allahumma","idn":"Ya Allah, dengan nama-Mu aku tidur.","tentang":"HR. Bukhari","tag":["malam"]},
    {"id":501,"nama":"Doa Makan","grup":"Makan","ar":"بِسْمِ اللّٰهِ","tr":"Bismillah","idn":"Dengan nama Allah.","tentang":"Referensi","tag":["makanan"]}
]}"""

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DuaRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private class Api(var raw: String = duaFixture) : DuaApi {
        var calls = 0
        override suspend fun all(): Response<ResponseBody> {
            calls++
            return Response.success(raw.toResponseBody())
        }
    }
    private fun repository(api: Api): DuaRepository {
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun getFilesDir(): File = folder.root
        }
        return DuaRepository(context, api)
    }
    @Test fun cachedCollectionWorksWithNewRepositoryAndNoNetworkRequest() = runBlocking {
        val api = Api()
        assertEquals(2, repository(api).load().items.size)
        api.raw = "unavailable"
        assertEquals(listOf(9, 501), repository(api).load().items.map { it.id })
        assertEquals(1, api.calls)
    }
    @Test fun malformedRefreshKeepsLastValidCache() = runBlocking {
        val api = Api()
        val repository = repository(api)
        repository.load()
        api.raw = """{"status":"success","data":[]}"""
        assertTrue(repository.load(refresh = true).refreshFailed)
        assertEquals(2, repository(api).load().items.size)
        assertEquals(2, api.calls)
    }
    @Test fun failedFirstLoadIsAnError() = runBlocking {
        assertTrue(runCatching { repository(Api("{\"status\":\"error\"}")).load() }.isFailure)
    }
    @Test fun filtersCombineCategoryAndCaseInsensitiveKeywordsAndTags() {
        val items = DuaRepository.parse(duaFixture)
        assertEquals(listOf(9), filterDuas(items, "MALAM", "Tidur").map { it.id })
        assertTrue(filterDuas(items, "malam", "Makan").isEmpty())
        assertEquals(listOf(501), filterDuas(items, "BISMILLAH nama", null).map { it.id })
    }
    @Test fun duplicateIdsAndMissingArabicAreRejected() {
        assertTrue(runCatching { DuaRepository.parse(duaFixture.replace("501", "9")) }.isFailure)
        assertTrue(runCatching { DuaRepository.parse(duaFixture.replace("اللَّهُمَّ", "")) }.isFailure)
    }
    @Test fun missingTranslationDoesNotBlockOtherPrayers() {
        val raw = org.json.JSONObject(duaFixture)
        raw.getJSONArray("data").getJSONObject(0).put("idn", org.json.JSONObject.NULL)
        val items = DuaRepository.parse(raw.toString())
        assertEquals(2, items.size)
        assertEquals("", items.first().translation)
    }
}
