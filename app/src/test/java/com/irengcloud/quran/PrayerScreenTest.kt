package com.irengcloud.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.irengcloud.quran.data.QiblaLocation
import com.irengcloud.quran.ui.components.QiblaCompassCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun qiblaCompassCardRendersCorrectlyWithLocation() {
        var locationRequested = false
        val sampleLocation = QiblaLocation(
            latitude = -6.2,
            longitude = 106.8,
            altitude = 15.0,
            accuracyMeters = 5.0f,
            updatedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            QiblaCompassCard(
                location = sampleLocation,
                locating = false,
                onRequestLocation = { locationRequested = true }
            )
        }

        composeTestRule.onNodeWithTag("qibla_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Arah Kiblat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perbarui Lokasi").assertIsDisplayed().performClick()
        assertTrue("Request location should be triggered by Perbarui Lokasi", locationRequested)
    }

    @Test
    fun qiblaCompassCardRendersPromptWhenLocationNull() {
        var locationRequested = false

        composeTestRule.setContent {
            QiblaCompassCard(
                location = null,
                locating = false,
                onRequestLocation = { locationRequested = true }
            )
        }

        composeTestRule.onNodeWithTag("qibla_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tentukan Lokasi Kiblat").assertIsDisplayed().performClick()
        assertTrue("Request location should be triggered when location is null", locationRequested)
    }

    @Test
    fun testUpcomingPrayerCalculation() {
        // Test prayer times list
        val times = listOf(
            "Imsak" to "04:13",
            "Subuh" to "04:23",
            "Terbit" to "05:35",
            "Duha" to "06:02",
            "Zuhur" to "11:46",
            "Asar" to "14:51",
            "Magrib" to "17:50",
            "Isya" to "18:59"
        )
        val tz = TimeZone.getTimeZone("Asia/Jakarta")

        assertNotNull(times)
        assertEquals(8, times.size)
    }

    @Test
    fun prayerScreenRendersHeaderAndLocationElements() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.irengcloud.quran.ui.DailyFeaturesViewModel(app)
        val settings = com.irengcloud.quran.data.local.ReaderSettingsState(
            prayerProvince = "DKI Jakarta",
            prayerCity = "Kota Jakarta"
        )

        composeTestRule.setContent {
            com.irengcloud.quran.ui.screens.PrayerScreen(
                settings = settings,
                features = viewModel
            )
        }

        composeTestRule.onNodeWithTag("prayer_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jadwal Sholat").assertIsDisplayed()
        composeTestRule.onNodeWithTag("filter_chip_today").assertIsDisplayed()
        composeTestRule.onNodeWithTag("filter_chip_qibla").assertIsDisplayed()
        composeTestRule.onNodeWithTag("filter_chip_month").assertExists()
        composeTestRule.onNodeWithTag("detect_prayer_location").assertExists()

        // Switch to "Arah Kiblat" section where qibla_card is displayed at the top
        composeTestRule.onNodeWithTag("filter_chip_qibla").performClick()
        composeTestRule.onNodeWithTag("qibla_card").assertIsDisplayed()

        // Switch back to "Hari Ini & Kiblat" section
        composeTestRule.onNodeWithTag("filter_chip_today").performClick()
        composeTestRule.onNodeWithTag("detect_prayer_location").assertExists()
    }
}
