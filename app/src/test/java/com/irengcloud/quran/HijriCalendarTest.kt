package com.irengcloud.quran

import com.irengcloud.quran.data.HijriCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HijriCalendarTest {
    @Test
    fun monthLengthsAndDatesMatchIndependentUmmAlQuraImplementation() {
        val lengths = mutableSetOf<Int>()
        for (year in 1447..1449) {
            for (month in 0..11) {
                val calendar = HijriCalendar.month(year, month, "Asia/Jakarta")
                val expected = HijrahDate.of(year, month + 1, 1)
                assertEquals(expected.lengthOfMonth(), calendar.days.size)
                assertEquals(LocalDate.from(expected).toString(), calendar.days.first().gregorianKey)
                lengths += calendar.days.size
                calendar.days.forEachIndexed { index, day ->
                    assertEquals(index + 1, day.day)
                    assertEquals(year, day.year)
                    assertEquals(month, day.month)
                    assertEquals(LocalDate.from(expected).plusDays(index.toLong()).toString(), day.gregorianKey)
                }
                val firstWeekday = LocalDate.parse(calendar.days.first().gregorianKey).dayOfWeek.value
                assertEquals(firstWeekday - 1, calendar.leadingEmptyCells)
            }
        }
        assertEquals(setOf(29, 30), lengths)
    }

    @Test
    fun localGregorianDatesRoundTripAcrossTimeZones() {
        for (zone in listOf("Asia/Jakarta", "Asia/Makassar", "America/New_York", "Pacific/Kiritimati")) {
            for (date in listOf("2026-02-18", "2026-03-08", "2026-11-01", "2026-12-31")) {
                val actual = HijriCalendar.date(date, zone)
                val expected = HijrahDate.from(LocalDate.parse(date))
                assertEquals(date, actual.gregorianKey)
                assertEquals(expected.get(ChronoField.YEAR), actual.year)
                assertEquals(expected.get(ChronoField.MONTH_OF_YEAR) - 1, actual.month)
                assertEquals(expected.get(ChronoField.DAY_OF_MONTH), actual.day)
                val month = HijriCalendar.month(actual.year, actual.month, zone)
                assertEquals(date, month.days[actual.day - 1].gregorianKey)
            }
        }
    }

    @Test
    fun lastDayOfYearIsFollowedByFirstDayOfMuharram() {
        val last = HijriCalendar.month(1447, 11, "Asia/Jakarta").days.last()
        val first = HijriCalendar.month(1448, 0, "Asia/Jakarta").days.first()
        assertEquals(LocalDate.parse(last.gregorianKey).plusDays(1).toString(), first.gregorianKey)
        assertTrue(first.hijriLabel.startsWith("1 Muharram"))
    }

    @Test
    fun islamicOccasionsFollowHijriDayRatherThanGregorianDate() {
        val ramadan = HijriCalendar.month(1448, 8, "Asia/Jakarta")
        assertEquals("Awal Ramadan", ramadan.days[0].occasions.single().title)
        assertEquals("Nuzulul Qur’an", ramadan.days[16].occasions.single().title)
        assertTrue(ramadan.days[1].occasions.isEmpty())
        val nextRamadan = HijriCalendar.month(1449, 8, "Asia/Jakarta")
        assertEquals(ramadan.days[16].occasions, nextRamadan.days[16].occasions)
    }
}
