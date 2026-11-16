package com.irengcloud.quran.data

import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

val IndonesianWeekdays = listOf("Ahad", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
val JavaneseMonthNames = listOf("Sura", "Sapar", "Mulud", "Bakdamulud", "Jumadilawal", "Jumadilakhir",
    "Rejeb", "Ruwah", "Pasa", "Sawal", "Dulkangidah", "Besar")
private val javaneseYearNames = listOf("Alip", "Ehe", "Jimawal", "Je", "Dal", "Be", "Wawu", "Jimakir")
private val pasarans = listOf("Legi", "Pahing", "Pon", "Wage", "Kliwon")

fun indonesianDateFormat(pattern: String, zone: TimeZone): SimpleDateFormat =
    SimpleDateFormat(pattern, Locale.forLanguageTag("id-ID")).apply {
        timeZone = zone
        dateFormatSymbols = DateFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
            weekdays = arrayOf("") + IndonesianWeekdays.toTypedArray()
            shortWeekdays = arrayOf("", "Ahd", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
        }
    }

data class JavaneseDate(val year: Int, val month: Int, val day: Int) {
    val yearName: String get() = javaneseYearNames[(year - 1867) % 8]
    val label: String get() = "$day ${JavaneseMonthNames[month]} $year $yearName"
}

data class JavaneseDay(val weekday: String, val pasaran: String, val date: JavaneseDate?) {
    val weton: String get() = "$weekday $pasaran"
}

/**
 * Civil-date mapping of the Asapon calendar: 1 Sura Alip 1867 = 24 March 1936 (Selasa Pon).
 * Source: BMKG Almanak 2026 (epoch), Kraton Jogja / Kalender Jawa Sultan Agungan (windu rules).
 * Uses the Kraton's Ehe/Dal/Jimakir long years and alternating 30/29-day months.
 * The final Jimakir of the 120-year kurup loses its leap day (25 August 2052 is the last day).
 * Outside this documented kurup, pasaran is still available but the lunar date is not extrapolated.
 * Dates label the daytime of each civil date; they do not switch at sunset.
 */
object JavaneseCalendar {
    const val FIRST_DATE = "1936-03-24"
    const val LAST_DATE = "2052-08-25"
    private val epoch = epochDay(FIRST_DATE)

    fun date(gregorianKey: String): JavaneseDay {
        val day = epochDay(gregorianKey)
        val offset = day - epoch
        val pasaran = pasarans[Math.floorMod(offset + 2, 5L).toInt()]
        val weekday = IndonesianWeekdays[Math.floorMod(day + 4, 7L).toInt()]
        var lunarDate: JavaneseDate? = null
        if (offset in 0 until 42524) {
            var remainder = offset.toInt()
            var year = 1867
            while (remainder >= yearLength(year)) {
                remainder -= yearLength(year)
                year++
            }
            var month = 0
            while (remainder >= monthLength(year, month)) {
                remainder -= monthLength(year, month)
                month++
            }
            lunarDate = JavaneseDate(year, month, remainder + 1)
        }
        return JavaneseDay(weekday, pasaran, lunarDate)
    }

    private fun yearLength(year: Int): Int = if (isLeap(year)) 355 else 354
    private fun isLeap(year: Int): Boolean = year != 1986 && (year - 1867) % 8 in listOf(1, 4, 7)
    private fun monthLength(year: Int, month: Int): Int =
        if (month == 11 && isLeap(year)) 30 else if (month % 2 == 0) 30 else 29

    private fun epochDay(key: String): Long {
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(key))
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        return requireNotNull(formatter.parse(key)).time / 86_400_000L
    }
}
