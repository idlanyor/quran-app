package com.irengcloud.quran

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.irengcloud.quran.data.PrayerApi
import com.irengcloud.quran.data.PrayerRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private fun fixture(): String {
        val days = JSONArray()
        for (day in 1..31) days.put(JSONObject().put("tanggal", day)
            .put("tanggal_lengkap", "2026-10-${day.toString().padStart(2, '0')}").put("hari", "Hari")
            .put("imsak", "04:13").put("subuh", "04:23").put("terbit", "05:35").put("dhuha", "06:02")
            .put("dzuhur", "11:46").put("ashar", "14:51").put("maghrib", "17:50").put("isya", "18:59"))
        return JSONObject().put("code", 200).put("data", JSONObject().put("provinsi", "DKI Jakarta")
            .put("kabkota", "Kota Jakarta").put("tahun", 2026).put("bulan", 10).put("jadwal", days)).toString()
    }
    private class Api(var raw: String) : PrayerApi {
        var calls = 0
        var offline = false
        override suspend fun provinces(): Response<ResponseBody> = error("Unexpected provinces request")
        override suspend fun cities(body: RequestBody): Response<ResponseBody> = error("Unexpected cities request")
        override suspend fun month(body: RequestBody): Response<ResponseBody> {
            calls++
            if (offline) throw java.io.IOException("Offline")
            return Response.success(raw.toResponseBody())
        }
    }
    private fun repository(api: Api): PrayerRepository {
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun getFilesDir(): File = folder.root
        }
        return PrayerRepository(context, api)
    }
    @Test fun completeMonthHasEightValidatedTimesEveryDay() {
        val month = PrayerRepository.parseMonth(fixture(), "DKI Jakarta", "Kota Jakarta", 2026, 10)
        assertEquals(31, month.days.size)
        assertEquals("2026-10-17", month.days[16].date)
        assertEquals(8, month.days[16].times.size)
        assertEquals("04:23", month.days[16].times.first { it.first == "Subuh" }.second)
    }
    @Test fun downloadedMonthIsReusableOfflineAndAfterRepositoryRecreation() = runBlocking {
        val api = Api(fixture())
        val first = repository(api).month("DKI Jakarta", "Kota Jakarta", 2026, 10)
        api.offline = true
        val second = repository(api).month("DKI Jakarta", "Kota Jakarta", 2026, 10)
        assertEquals(first.days, second.days)
        assertTrue(second.fromCache)
        assertEquals(1, api.calls)
    }
    @Test fun failedRefreshKeepsCachedMonth() = runBlocking {
        val api = Api(fixture())
        val repository = repository(api)
        repository.month("DKI Jakarta", "Kota Jakarta", 2026, 10)
        api.offline = true
        assertTrue(repository.month("DKI Jakarta", "Kota Jakarta", 2026, 10, true).fromCache)
        assertEquals(2, api.calls)
    }
    @Test fun differentMonthNeverReusesStaleSchedule() = runBlocking {
        val api = Api(fixture())
        val repository = repository(api)
        repository.month("DKI Jakarta", "Kota Jakarta", 2026, 10)
        api.offline = true
        assertTrue(runCatching { repository.month("DKI Jakarta", "Kota Jakarta", 2026, 11) }.isFailure)
    }
    @Test fun invalidPrayerTimesAreRejected() {
        assertTrue(runCatching { PrayerRepository.parseMonth(fixture().replace("04:23", "29:99"),
            "DKI Jakarta", "Kota Jakarta", 2026, 10) }.isFailure)
    }
    @Test fun wrongCityResponseIsRejected() {
        assertTrue(runCatching { PrayerRepository.parseMonth(fixture(), "Jawa Barat", "Kota Bandung", 2026, 10) }.isFailure)
    }
    @Test fun incompleteMonthIsRejected() {
        val root = JSONObject(fixture())
        root.getJSONObject("data").getJSONArray("jadwal").remove(0)
        assertTrue(runCatching { PrayerRepository.parseMonth(root.toString(), "DKI Jakarta", "Kota Jakarta", 2026, 10) }.isFailure)
    }
}
