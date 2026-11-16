package com.irengcloud.quran

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.irengcloud.quran.data.local.BookmarkEntity
import com.irengcloud.quran.data.local.LastReadState
import com.irengcloud.quran.data.local.QuranSeedData
import com.irengcloud.quran.data.local.ReaderSettingsState
import com.irengcloud.quran.ui.screens.BookmarkKhatamScreen
import com.irengcloud.quran.ui.theme.QuranAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BookmarkKhatamScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun showScreen(
        onOpen: (Int, Int) -> Unit = { _, _ -> },
        onRemove: (Int, Int) -> Unit = { _, _ -> }
    ) {
        val bookmarks = QuranSeedData.getInitialCachedAyats()
            .filter { it.surahNomor == 1 && it.nomorAyat in listOf(1, 6) }
            .reversed().map {
                BookmarkEntity(1, "Al-Fatihah", "الفاتحة", it.nomorAyat, it.teksArab, it.teksIndonesia)
            }
        compose.setContent {
            var section by remember { mutableIntStateOf(1) }
            QuranAppTheme(darkTheme = true) {
                BookmarkKhatamScreen(
                    bookmarks = bookmarks, khatamLogs = emptyList(),
                    lastRead = LastReadState(), readerSettings = ReaderSettingsState(),
                    onOpenSurahAtAyat = onOpen, onRemoveBookmark = onRemove,
                    onTargetModeChange = {}, onUpdateDailyTargetAyat = {}, onUpdateDailyTargetSurah = {},
                    onLogCustomAyats = {}, onLogCustomSurahs = {}, onResetKhatam = {},
                    selectedSection = section, onSectionChange = { section = it }
                )
            }
        }
    }

    @Test
    fun progressIsShownOnlyInKhatamTab() {
        showScreen()
        compose.onNodeWithTag("khatam_summary_card").assertDoesNotExist()
        compose.onNodeWithTag("section_chip_khatam").performClick()
        compose.onNodeWithTag("bookmark_khatam_lazy_column")
            .performScrollToNode(hasTestTag("khatam_summary_card"))
        compose.onNodeWithTag("khatam_summary_card").assertExists()
        compose.onNodeWithTag("bookmark_khatam_lazy_column")
            .performScrollToNode(hasTestTag("section_chip_bookmarks"))
        compose.onNodeWithTag("section_chip_bookmarks").performClick()
        compose.onNodeWithTag("khatam_summary_card").assertDoesNotExist()
    }

    @Test
    fun bookmarkPreviewIdentifiesAndOpensTheExactVerse() {
        var opened: Pair<Int, Int>? = null
        showScreen(onOpen = { surah, verse -> opened = surah to verse })
        compose.onNodeWithTag("bookmark_khatam_lazy_column")
            .performScrollToNode(hasTestTag("bookmark_card_1_6"))
        compose.onNodeWithTag("bookmark_card_1_6")
            .assertTextContains("Al-Fatihah · Ayat 6")
            .assertTextContains("Tunjukilah kami jalan yang lurus,")
            .performClick()
        assertEquals(1 to 6, opened)
    }

    @Test
    fun deleteMenuRemovesOnlyTheChosenBookmarkWithoutOpeningIt() {
        var removed: Pair<Int, Int>? = null
        var opened: Pair<Int, Int>? = null
        showScreen(
            onOpen = { surah, verse -> opened = surah to verse },
            onRemove = { surah, verse -> removed = surah to verse }
        )
        compose.onNodeWithTag("bookmark_khatam_lazy_column")
            .performScrollToNode(hasTestTag("bookmark_card_1_6"))
        compose.onNodeWithContentDescription("Menu penanda Al-Fatihah ayat 6").performClick()
        compose.onNodeWithTag("delete_bookmark_1_6").performClick()
        compose.onNodeWithText("Hapus").performClick()
        assertEquals(1 to 6, removed)
        assertNull(opened)
    }
}
