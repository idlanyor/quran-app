package com.irengcloud.quran

import com.irengcloud.quran.reminders.nextReminderTime
import com.irengcloud.quran.data.matchCity
import com.irengcloud.quran.data.matchProvince
import com.irengcloud.quran.data.prayerTimeZone
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class ReminderTimeTest {
    private fun time(value: String, zone: TimeZone): Long = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        .apply { timeZone = zone }.parse(value)!!.time

    @Test fun beforeFiveSchedulesToday() {
        val zone = TimeZone.getTimeZone("Asia/Jakarta")
        assertEquals(time("2026-10-17 05:00:00", zone), nextReminderTime(time("2026-10-17 04:59:00", zone), 5, 0, zone))
    }
    @Test fun atOrAfterFiveSchedulesTomorrow() {
        val zone = TimeZone.getTimeZone("Asia/Jakarta")
        for (now in listOf("2026-10-17 05:00:00", "2026-10-17 23:59:00")) {
            assertEquals(time("2026-10-18 05:00:00", zone), nextReminderTime(time(now, zone), 5, 0, zone))
        }
    }
    @Test fun followsLocalTimeAcrossDaylightSavingChange() {
        val zone = TimeZone.getTimeZone("Europe/Berlin")
        assertEquals(time("2026-03-29 05:00:00", zone), nextReminderTime(time("2026-03-28 06:00:00", zone), 5, 0, zone))
    }
    @Test fun cityAndRegencyWithSameNameStayDistinct() {
        val options = listOf("Kab. Bandung", "Kab. Bandung Barat", "Kota Bandung")
        assertEquals("Kota Bandung", matchCity(listOf("Kota Bandung"), "Jawa Barat", options))
        assertEquals("Kab. Bandung", matchCity(listOf("Kabupaten Bandung"), "Jawa Barat", options))
        assertNull(matchCity(listOf("Bandung"), "Jawa Barat", options))
    }
    @Test fun provinceAliasesAndJakartaAdministrativeCitiesMatchApi() {
        assertEquals("DKI Jakarta", matchProvince("Daerah Khusus Ibukota Jakarta", listOf("DKI Jakarta")))
        assertEquals("D.I. Yogyakarta", matchProvince("Daerah Istimewa Yogyakarta", listOf("D.I. Yogyakarta")))
        assertEquals("Kota Jakarta", matchCity(listOf("Kota Jakarta Pusat"), "DKI Jakarta", listOf("Kab. Kepulauan Seribu", "Kota Jakarta")))
    }
    @Test fun indonesianTimeZonesFollowSelectedProvince() {
        assertEquals("Asia/Jakarta", prayerTimeZone("Jawa Barat").id)
        assertEquals("Asia/Makassar", prayerTimeZone("Bali").id)
        assertEquals("Asia/Makassar", prayerTimeZone("Sulawesi Selatan").id)
        assertEquals("Asia/Jayapura", prayerTimeZone("Papua Barat").id)
    }
}
