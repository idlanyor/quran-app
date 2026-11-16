package com.irengcloud.quran

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.irengcloud.quran.data.HijriCalendar
import com.irengcloud.quran.data.JavaneseCalendar
import com.irengcloud.quran.ui.screens.HijriCalendarScreen
import com.irengcloud.quran.ui.theme.QuranAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HijriCalendarScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun chooseMonthSelectOccasionAndReturnToToday() {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val today = HijriCalendar.date(todayKey, TimeZone.getDefault().id)
        compose.setContent {
            QuranAppTheme(darkTheme = true) { HijriCalendarScreen(onBack = {}) }
        }

        compose.onNodeWithTag("hijri_choose_month").performClick()
        compose.onNodeWithTag("hijri_year_input").performTextReplacement("1448")
        compose.onNodeWithTag("hijri_month_input").performClick()
        compose.onNodeWithTag("hijri_month_option_8").performScrollTo().performClick()
        compose.onNodeWithText("Tampilkan").performClick()
        compose.onNodeWithTag("hijri_choose_month").assertTextContains("Ramadan 1448 H")
        compose.onNodeWithText("Ahad").assertExists()
        compose.onNodeWithText("Min").assertDoesNotExist()

        compose.onNodeWithTag("hijri_day_17").performScrollTo().performClick()
        compose.onNodeWithTag("hijri_calendar").performScrollToNode(hasTestTag("hijri_selected_date"))
        compose.onNodeWithTag("hijri_selected_date")
            .assert(hasAnyDescendant(hasText("Nuzulul Qur’an")))
        val selectedJawa = JavaneseCalendar.date(HijriCalendar.month(1448, 8, TimeZone.getDefault().id).days[16].gregorianKey)
        compose.onNodeWithTag("hijri_calendar").performScrollToNode(hasTestTag("javanese_selected_date"))
        compose.onNodeWithTag("javanese_selected_date").assert(hasAnyDescendant(hasText(selectedJawa.weton)))
        compose.onNodeWithTag("javanese_selected_date").assert(hasAnyDescendant(hasText(requireNotNull(selectedJawa.date).label)))

        compose.onNodeWithTag("hijri_calendar").performScrollToNode(hasTestTag("hijri_today"))
        compose.onNodeWithTag("hijri_today").performClick()
        compose.onNodeWithTag("hijri_calendar").performScrollToNode(hasTestTag("hijri_choose_month"))
        compose.onNodeWithTag("hijri_choose_month")
            .assertTextContains("${com.irengcloud.quran.data.HijriMonthNames[today.month]} ${today.year} H")
    }
}
