package com.irengcloud.quran

import com.irengcloud.quran.data.QuranRepository
import com.irengcloud.quran.data.local.AyatEntity
import com.irengcloud.quran.data.local.QuranDao
import com.irengcloud.quran.data.local.SurahEntity
import com.irengcloud.quran.data.remote.QuranApiService
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class OfflineSearchTest {
    private fun repository(matches: Int): QuranRepository {
        val verses = (1..matches).map { AyatEntity(1, it, "arab", "latin", "rahmat") }
        val dao = Proxy.newProxyInstance(
            QuranDao::class.java.classLoader, arrayOf(QuranDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getAllSurahsFlow", "getAllBookmarksFlow", "getRecentKhatamLogsFlow" -> emptyFlow<Any>()
                "getAllSurahsOnce" -> listOf(SurahEntity(1, "arab", "Al-Fatihah", "Pembukaan", 7, "Makkiyah"))
                "countCompleteCachedSurahs" -> 114
                "countCachedSearchMatches" -> matches
                "searchCachedAyats" -> verses.drop(args!![2] as Int).take(args[1] as Int)
                else -> error("Unexpected DAO call: ${method.name}")
            }
        } as QuranDao
        val api = Proxy.newProxyInstance(
            QuranApiService::class.java.classLoader, arrayOf(QuranApiService::class.java)
        ) { _, method, _ -> error("Offline search must not call API: ${method.name}") } as QuranApiService
        return QuranRepository(dao, api)
    }

    @Test
    fun completeCorpusSearchPaginatesWithoutNetwork() = runBlocking {
        val repository = repository(12)
        val first = repository.searchAyat("rahmat", 1, 10).getOrThrow()
        val last = repository.searchAyat("rahmat", 2, 10).getOrThrow()
        assertEquals(12, first.totalItems)
        assertEquals(2, first.totalPages)
        assertTrue(first.hasMore)
        assertEquals((1..10).toList(), first.items.map { it.nomorAyat })
        assertEquals(listOf(11, 12), last.items.map { it.nomorAyat })
        assertFalse(last.hasMore)
        assertEquals("Al-Fatihah", last.items.first().surahNamaLatin)
    }

    @Test
    fun noMatchesDoesNotWaitForNetwork() = runBlocking {
        val result = repository(0).searchAyat("missing").getOrThrow()
        assertTrue(result.items.isEmpty())
        assertEquals(0, result.totalItems)
        assertEquals(1, result.totalPages)
        assertFalse(result.hasMore)
    }
}
