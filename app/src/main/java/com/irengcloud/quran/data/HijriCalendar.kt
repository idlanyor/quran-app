package com.irengcloud.quran.data

import android.icu.util.Calendar
import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val HijriMonthNames = listOf(
    "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir",
    "Jumadil Awal", "Jumadil Akhir", "Rajab", "Syakban",
    "Ramadan", "Syawal", "Zulkaidah", "Zulhijah"
)

data class HijriDay(
    val year: Int,
    val month: Int,
    val day: Int,
    val gregorianKey: String,
    val timeMillis: Long,
    val weekday: Int,
    val gregorianDay: Int
) {
    val javanese: JavaneseDay by lazy { JavaneseCalendar.date(gregorianKey) }
    val hijriLabel: String get() = "$day ${HijriMonthNames[month]} $year H"
    val occasions: List<HijriOccasion> get() = HijriOccasions.filter { it.month == month && it.day == day }
}

data class HijriOccasion(val month: Int, val day: Int, val title: String)

// Recurring Hijri dates, not a list of Indonesian public holidays or isbat decisions.
val HijriOccasions = listOf(
    HijriOccasion(0, 1, "Tahun Baru Islam"),
    HijriOccasion(2, 12, "Maulid Nabi Muhammad SAW"),
    HijriOccasion(6, 27, "Isra Mikraj Nabi Muhammad SAW"),
    HijriOccasion(8, 1, "Awal Ramadan"),
    HijriOccasion(8, 17, "Nuzulul Qur’an"),
    HijriOccasion(9, 1, "Idulfitri"),
    HijriOccasion(11, 10, "Iduladha")
)

data class HijriMonth(val year: Int, val month: Int, val days: List<HijriDay>) {
    // Calendar uses Sunday=1. The grid starts on Monday.
    val leadingEmptyCells: Int get() = (days.first().weekday + 5) % 7
    val label: String get() = "${HijriMonthNames[month]} $year H"
}

object HijriCalendar {
    const val MIN_YEAR = 1300
    const val MAX_YEAR = 1600

    fun date(gregorianKey: String, zoneId: String): HijriDay {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone(zoneId)
            isLenient = false
        }
        val instant = requireNotNull(formatter.parse(gregorianKey))
        return day(calendar(zoneId).apply { timeInMillis = instant.time }, zoneId)
    }

    fun month(year: Int, month: Int, zoneId: String): HijriMonth {
        require(year in MIN_YEAR..MAX_YEAR && month in 0..11)
        val calendar = calendar(zoneId).apply {
            clear()
            set(year, month, 1, 12, 0, 0)
        }
        val count = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val days = List(count) {
            day(calendar, zoneId).also { calendar.add(Calendar.DAY_OF_MONTH, 1) }
        }
        return HijriMonth(year, month, days)
    }

    fun format(day: HijriDay, pattern: String, zoneId: String): String =
        indonesianDateFormat(pattern, java.util.TimeZone.getTimeZone(zoneId)).format(Date(day.timeMillis))

    private fun calendar(zoneId: String) =
        IslamicCalendar(TimeZone.getTimeZone(zoneId), Locale.forLanguageTag("id-ID")).apply {
            calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        }

    private fun day(calendar: IslamicCalendar, zoneId: String): HijriDay {
        val instant = Date(calendar.timeInMillis)
        val zone = java.util.TimeZone.getTimeZone(zoneId)
        val gregorian = java.util.Calendar.getInstance(zone).apply { time = instant }
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.format(instant)
        return HijriDay(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH),
            day = calendar.get(Calendar.DAY_OF_MONTH),
            gregorianKey = key,
            timeMillis = instant.time,
            weekday = calendar.get(Calendar.DAY_OF_WEEK),
            gregorianDay = gregorian.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
}
