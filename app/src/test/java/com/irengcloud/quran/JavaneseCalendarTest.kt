package com.irengcloud.quran

import com.irengcloud.quran.data.HijriCalendar
import com.irengcloud.quran.data.JavaneseCalendar
import com.irengcloud.quran.data.JavaneseDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JavaneseCalendarTest {
    @Test fun epochAndKurupEndMatchDocumentedAsaponDates() {
        val first = JavaneseCalendar.date("1936-03-24")
        assertEquals("Selasa Pon", first.weton)
        assertEquals(JavaneseDate(1867, 0, 1), first.date)
        assertEquals(JavaneseDate(1986, 11, 29), JavaneseCalendar.date("2052-08-25").date)
        assertNull(JavaneseCalendar.date("2052-08-26").date)
        assertNull(JavaneseCalendar.date("1936-03-23").date)
    }

    @Test fun modernDatesMatchKratonAndDiyPublishedEvents() {
        // DIY 2025 event circular: 1 Sawal 1958 = 31 March; 10 Besar = 7 June.
        assertEquals(JavaneseDate(1958, 9, 1), JavaneseCalendar.date("2025-03-31").date)
        assertEquals(JavaneseDate(1958, 11, 10), JavaneseCalendar.date("2025-06-07").date)
        assertEquals(JavaneseDate(1959, 0, 1), JavaneseCalendar.date("2025-06-27").date)
        // Kraton: Tingalan Jumenengan Dalem, 29 Rejeb Dal 1959 = 18 January 2026.
        val anniversary = JavaneseCalendar.date("2026-01-18")
        assertEquals(JavaneseDate(1959, 6, 29), anniversary.date)
        assertEquals("Ahad Kliwon", anniversary.weton)
        assertEquals(JavaneseDate(1960, 0, 1), JavaneseCalendar.date("2026-06-17").date)
        assertEquals("Rabu Kliwon", JavaneseCalendar.date("2026-06-17").weton)
    }

    @Test fun pasaranCyclesRemainContinuousBeforeEpochAndAcrossLeapDays() {
        val expected = listOf("Legi", "Pahing", "Pon", "Wage", "Kliwon")
        for (start in listOf("1882-01-01", "1936-03-20", "2024-02-27", "2052-08-23")) {
            val date = LocalDate.parse(start)
            val firstIndex = expected.indexOf(JavaneseCalendar.date(start).pasaran)
            repeat(70) { offset ->
                assertEquals(expected[(firstIndex + offset) % 5], JavaneseCalendar.date(date.plusDays(offset.toLong()).toString()).pasaran)
            }
        }
        assertEquals("Jumat Legi", JavaneseCalendar.date("1945-08-17").weton)
    }

    @Test fun sameCivilDateHasSameJavaneseDateInEveryTimeZoneAndSundayIsAhad() {
        for (zone in listOf("Asia/Jakarta", "Asia/Makassar", "America/New_York", "Pacific/Kiritimati")) {
            val day = HijriCalendar.date("2026-01-18", zone)
            assertEquals("Ahad Kliwon", day.javanese.weton)
            assertEquals("Ahad, 18 Januari 2026", HijriCalendar.format(day, "EEEE, d MMMM yyyy", zone))
            assertEquals("Ahd", HijriCalendar.format(day, "EEE", zone))
        }
    }
}
