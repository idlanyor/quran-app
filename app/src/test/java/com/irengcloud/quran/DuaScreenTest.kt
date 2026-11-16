package com.irengcloud.quran

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.irengcloud.quran.data.DuaRepository
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.ui.DuaState
import com.irengcloud.quran.ui.screens.DuaScreen
import com.irengcloud.quran.ui.theme.QuranAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DuaScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun searchOpenExactDuaAndReturnToFilteredList() {
        compose.setContent { QuranAppTheme(darkTheme = true) {
            DuaScreen(DuaState(DuaRepository.parse(duaFixture), loading = false), ReaderSettingsState(), {}, {})
        } }
        compose.onNodeWithTag("dua_search").performTextReplacement("malam")
        compose.onNodeWithTag("dua_screen").performScrollToNode(hasTestTag("dua_item_9"))
        compose.onNodeWithTag("dua_item_9").performClick()
        compose.onNodeWithTag("dua_screen").performScrollToNode(hasText("Allahumma"))
        compose.onNodeWithText("Allahumma").assertExists()
        compose.onNodeWithText("Ya Allah, dengan nama-Mu aku tidur.").assertExists()
        compose.onNodeWithTag("dua_screen").performScrollToNode(hasContentDescription("Kembali"))
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.onNodeWithTag("dua_item_501").assertDoesNotExist()
    }
    @Test fun categoryFiltersAndReaderPreferencesAreRespected() {
        compose.setContent { QuranAppTheme {
            DuaScreen(DuaState(DuaRepository.parse(duaFixture), loading = false),
                ReaderSettingsState(showLatin = false, showTranslation = false), {}, {})
        } }
        compose.onNodeWithTag("dua_category").performScrollTo().performClick()
        compose.onNodeWithText("Makan").performClick()
        compose.onNodeWithTag("dua_screen").performScrollToNode(hasTestTag("dua_item_501"))
        compose.onNodeWithTag("dua_item_9").assertDoesNotExist()
        compose.onNodeWithTag("dua_item_501").performClick()
        compose.onNodeWithTag("dua_detail").assertExists()
        compose.onNodeWithText("Bismillah").assertDoesNotExist()
        compose.onNodeWithText("Dengan nama Allah.").assertDoesNotExist()
    }
}
