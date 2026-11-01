package com.irengcloud.quran

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.irengcloud.quran.data.local.KhatamLogEntity
import com.irengcloud.quran.data.local.QuranSeedData
import com.irengcloud.quran.data.local.ReadingDelta
import com.irengcloud.quran.data.local.ReadingProgressRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import okio.Path.Companion.toPath
import okio.FileSystem

class ReadingProgressTest {
    @get:Rule val folder = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store by lazy {
        PreferenceDataStoreFactory.create(
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {
                File(folder.root, "reading.preferences_pb").absolutePath.toPath()
            },
            scope = scope
        )
    }
    private var time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("2026-10-17 12:00")!!.time
    private val progress by lazy { ReadingProgressRepository(store) { time } }
    private val surahs = QuranSeedData.getAll114Surahs()
    private val totalAyats = intPreferencesKey("total_khatam_completed")
    private val totalSurahs = intPreferencesKey("total_khatam_surah_completed")

    @After fun close() { scope.cancel() }

    @Test fun positionDoesNotCountAsCompletedReading() = runBlocking {
        progress.savePosition(surahs[1], 85)
        val prefs = store.data.first()
        assertEquals(2, prefs[intPreferencesKey("last_surah_nomor")])
        assertEquals(85, prefs[intPreferencesKey("last_ayat_nomor")])
        assertEquals(time, prefs[longPreferencesKey("last_updated_at")])
        assertNull(prefs[totalAyats])
        assertTrue(progress.logsFlow.first().isEmpty())
    }

    @Test fun completionCountsUniqueAyatsAndSurahExactlyOnce() = runBlocking {
        assertEquals(ReadingDelta(5, 0), progress.markReadThrough(surahs[0], 5))
        assertEquals(ReadingDelta(), progress.markReadThrough(surahs[0], 3))
        assertEquals(ReadingDelta(2, 1), progress.markReadThrough(surahs[0], 7))
        assertEquals(ReadingDelta(), progress.markReadThrough(surahs[0], 7))
        assertEquals(7, store.data.first()[totalAyats])
        assertEquals(1, store.data.first()[totalSurahs])
        assertEquals(7, progress.logsFlow.first().single().ayatCountRead)
    }

    @Test fun longSurahIsNotLimitedToFiftyAyats() = runBlocking {
        assertEquals(ReadingDelta(286, 1), progress.markReadThrough(surahs[1], 286))
        assertEquals(286, store.data.first()[totalAyats])
    }

    @Test fun quickLoggingUsesActualLengthsAndCrossesSurahBoundary() = runBlocking {
        assertEquals(ReadingDelta(10, 1), progress.recordNext(ayatCount = 10))
        assertEquals(2, store.data.first()[intPreferencesKey("last_surah_nomor")])
        assertEquals(3, store.data.first()[intPreferencesKey("last_ayat_nomor")])
        assertEquals(ReadingDelta(283, 1), progress.recordNext(surahCount = 1))
        assertEquals(293, store.data.first()[totalAyats])
        assertEquals(2, store.data.first()[totalSurahs])
    }

    @Test fun nextDayStartsNewDailyLogAndKeepsOverallTotals() = runBlocking {
        progress.markReadThrough(surahs[0], 3)
        time += 24 * 60 * 60 * 1000L
        progress.markReadThrough(surahs[0], 7)
        val logs = progress.logsFlow.first()
        assertEquals(listOf("2026-10-18", "2026-10-17"), logs.map { it.dateKey })
        assertEquals(listOf(4, 3), logs.map { it.ayatCountRead })
        assertEquals(7, store.data.first()[totalAyats])
    }

    @Test fun repeatedConcurrentTapsDoNotDoubleCount() = runBlocking {
        (1..20).map { async(Dispatchers.Default) { progress.markReadThrough(surahs[0], 7) } }.awaitAll()
        assertEquals(7, store.data.first()[totalAyats])
        assertEquals(1, store.data.first()[totalSurahs])
        assertEquals(7, progress.logsFlow.first().single().ayatCountRead)
    }

    @Test fun resetClearsDailyAndCompletionButPreservesPosition() = runBlocking {
        progress.markReadThrough(surahs[0], 7)
        progress.reset()
        assertEquals(0, store.data.first()[totalAyats])
        assertEquals(0, store.data.first()[totalSurahs])
        assertEquals(7, store.data.first()[intPreferencesKey("last_ayat_nomor")])
        assertTrue(progress.logsFlow.first().isEmpty())
        assertEquals(ReadingDelta(7, 1), progress.markReadThrough(surahs[0], 7))
    }

    @Test fun repositoryRecreationPreservesCompletion() = runBlocking {
        progress.markReadThrough(surahs[0], 7)
        val reopened = ReadingProgressRepository(store) { time }
        assertEquals(ReadingDelta(), reopened.markReadThrough(surahs[0], 7))
        assertEquals(7, reopened.logsFlow.first().single().ayatCountRead)
    }

    @Test fun legacyImportIsIdempotentAndDoesNotReturnAfterReset() = runBlocking {
        store.edit { it[totalAyats] = 12 }
        val logs = listOf(KhatamLogEntity("2026-10-16", 12, 0))
        progress.importLegacyHistory(logs)
        progress.importLegacyHistory(logs)
        assertEquals(12, progress.logsFlow.first().single().ayatCountRead)
        assertEquals(12, store.data.first()[totalAyats])
        progress.reset()
        progress.importLegacyHistory(logs)
        assertTrue(progress.logsFlow.first().isEmpty())
    }

    @Test fun fullKhatamStopsAtActualQuranTotals() = runBlocking {
        assertEquals(ReadingDelta(6236, 114), progress.recordNext(surahCount = 114))
        assertEquals(ReadingDelta(), progress.recordNext(ayatCount = 20))
        assertEquals(6236, store.data.first()[totalAyats])
        assertEquals(114, store.data.first()[totalSurahs])
    }
}
