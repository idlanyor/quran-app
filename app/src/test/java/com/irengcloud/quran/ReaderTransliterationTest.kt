package com.irengcloud.quran

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.irengcloud.quran.data.QuranRepository
import com.irengcloud.quran.data.local.AyatEntity
import com.irengcloud.quran.data.local.QuranDao
import com.irengcloud.quran.data.local.QuranLatinSource
import com.irengcloud.quran.data.local.QuranSeedData
import com.irengcloud.quran.data.remote.QuranApiService
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderTransliterationTest {
    @Test
    fun existingOfflineSurahGetsMissingLatinWithoutNetwork() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = QuranLatinSource(context.assets)
        val chapters = QuranSeedData.getAll114Surahs()
        for (chapter in chapters) {
            val latin = source.forSurah(chapter.nomor)
            assertEquals(chapter.namaLatin, chapter.jumlahAyat, latin.size)
            assertTrue(chapter.namaLatin, latin.all { it.isNotBlank() })
        }

        val surah = chapters.first { it.nomor == 2 }
        val verses = (1..surah.jumlahAyat).map {
            AyatEntity(surah.nomor, it, "arab", if (it == 2) "Latin tersimpan" else "", "terjemahan")
        }
        val dao = Proxy.newProxyInstance(
            QuranDao::class.java.classLoader, arrayOf(QuranDao::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getAllSurahsFlow", "getAllBookmarksFlow", "getRecentKhatamLogsFlow" -> emptyFlow<Any>()
                "getSurahByNomor" -> surah
                "getAyatsBySurahOnce" -> verses
                else -> error("Unexpected DAO call: ${method.name}")
            }
        } as QuranDao
        val api = Proxy.newProxyInstance(
            QuranApiService::class.java.classLoader, arrayOf(QuranApiService::class.java)
        ) { _, method, _ -> error("Offline reader must not call API: ${method.name}") } as QuranApiService

        val (_, opened) = QuranRepository(dao, api, source::forSurah).fetchAndCacheSurahDetail(surah.nomor).getOrThrow()
        assertEquals("Latin tersimpan", opened[1].teksLatin)
        assertEquals(source.forSurah(surah.nomor)[2], opened[2].teksLatin)
        assertTrue(opened.all { it.teksLatin.isNotBlank() })
    }
}
