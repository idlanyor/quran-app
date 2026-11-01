package com.irengcloud.quran.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.map

data class ReadingDelta(val ayats: Int = 0, val surahs: Int = 0)

/** Position, completion and daily history share one atomic DataStore edit. */
class ReadingProgressRepository(
    private val store: DataStore<Preferences>,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val surahs = QuranSeedData.getAll114Surahs()
    private val totalAyatKey = intPreferencesKey("total_khatam_completed")
    private val totalSurahKey = intPreferencesKey("total_khatam_surah_completed")
    private val migratedKey = booleanPreferencesKey("reading_history_migrated")
    private fun progressKey(nomor: Int) = intPreferencesKey("reading_surah_$nomor")
    private fun dayAyatKey(day: String) = intPreferencesKey("reading_day_${day}_ayat")
    private fun daySurahKey(day: String) = intPreferencesKey("reading_day_${day}_surah")
    private fun dayTimeKey(day: String) = longPreferencesKey("reading_day_${day}_time")

    val logsFlow = store.data.map { prefs ->
        prefs.asMap().keys.map { it.name }
            .filter { it.startsWith("reading_day_") && it.endsWith("_ayat") }
            .map { it.removePrefix("reading_day_").removeSuffix("_ayat") }
            .sortedDescending().take(30).map { day ->
                KhatamLogEntity(day, prefs[dayAyatKey(day)] ?: 0,
                    prefs[daySurahKey(day)] ?: 0, prefs[dayTimeKey(day)] ?: 0L)
            }
    }

    suspend fun importLegacyHistory(logs: List<KhatamLogEntity>) {
        store.edit { prefs ->
            if (prefs[migratedKey] != true) {
                logs.forEach { log ->
                    prefs[dayAyatKey(log.dateKey)] = (prefs[dayAyatKey(log.dateKey)] ?: 0) + log.ayatCountRead
                    prefs[daySurahKey(log.dateKey)] = (prefs[daySurahKey(log.dateKey)] ?: 0) + log.surahCountRead
                    prefs[dayTimeKey(log.dateKey)] = log.lastUpdated
                }
                prefs[migratedKey] = true
            }
        }
    }

    suspend fun savePosition(surah: SurahEntity, ayat: Int) {
        require(ayat in 1..surah.jumlahAyat)
        store.edit { prefs ->
            if (prefs[intPreferencesKey("last_surah_nomor")] != surah.nomor ||
                prefs[intPreferencesKey("last_ayat_nomor")] != ayat ||
                (prefs[longPreferencesKey("last_updated_at")] ?: 0L) == 0L) {
                savePosition(prefs, surah, ayat)
            }
        }
    }

    suspend fun markReadThrough(surah: SurahEntity, ayat: Int): ReadingDelta {
        require(ayat in 1..surah.jumlahAyat)
        var result = ReadingDelta()
        store.edit { prefs ->
            result = advance(prefs, surah, ayat)
            savePosition(prefs, surah, ayat)
            recordDay(prefs, result)
        }
        return result
    }

    /** Quick logging completes the next unread verses/surahs in Quran order. */
    suspend fun recordNext(ayatCount: Int = 0, surahCount: Int = 0): ReadingDelta {
        require(ayatCount >= 0 && surahCount >= 0)
        var result = ReadingDelta()
        store.edit { prefs ->
            var remainingAyats = ayatCount
            var remainingSurahs = surahCount
            for (surah in surahs) {
                val read = prefs[progressKey(surah.nomor)] ?: 0
                if (read >= surah.jumlahAyat) continue
                if (remainingAyats == 0 && remainingSurahs == 0) break
                val end = if (remainingSurahs > 0) surah.jumlahAyat
                    else read + minOf(remainingAyats, surah.jumlahAyat - read)
                val delta = advance(prefs, surah, end)
                result = ReadingDelta(result.ayats + delta.ayats, result.surahs + delta.surahs)
                if (remainingSurahs > 0) remainingSurahs-- else remainingAyats -= delta.ayats
                savePosition(prefs, surah, end)
            }
            recordDay(prefs, result)
        }
        return result
    }

    suspend fun reset() {
        store.edit { prefs ->
            prefs.asMap().keys.filter {
                it.name.startsWith("reading_surah_") || it.name.startsWith("reading_day_")
            }.forEach { prefs.remove(it) }
            prefs[totalAyatKey] = 0
            prefs[totalSurahKey] = 0
            // Prevent old Room history from reappearing after a reset.
            prefs[migratedKey] = true
        }
    }

    private fun advance(prefs: MutablePreferences, surah: SurahEntity, end: Int): ReadingDelta {
        val before = prefs[progressKey(surah.nomor)] ?: 0
        val after = maxOf(before, end).coerceAtMost(surah.jumlahAyat)
        val delta = ReadingDelta(after - before,
            if (before < surah.jumlahAyat && after == surah.jumlahAyat) 1 else 0)
        prefs[progressKey(surah.nomor)] = after
        prefs[totalAyatKey] = ((prefs[totalAyatKey] ?: 0) + delta.ayats).coerceAtMost(6236)
        prefs[totalSurahKey] = ((prefs[totalSurahKey] ?: 0) + delta.surahs).coerceAtMost(114)
        return delta
    }

    private fun recordDay(prefs: MutablePreferences, delta: ReadingDelta) {
        if (delta.ayats == 0 && delta.surahs == 0) return
        val time = now()
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))
        prefs[dayAyatKey(day)] = (prefs[dayAyatKey(day)] ?: 0) + delta.ayats
        prefs[daySurahKey(day)] = (prefs[daySurahKey(day)] ?: 0) + delta.surahs
        prefs[dayTimeKey(day)] = time
    }

    private fun savePosition(prefs: MutablePreferences, surah: SurahEntity, ayat: Int) {
        prefs[intPreferencesKey("last_surah_nomor")] = surah.nomor
        prefs[stringPreferencesKey("last_surah_latin")] = surah.namaLatin
        prefs[stringPreferencesKey("last_surah_arab")] = surah.namaArab
        prefs[stringPreferencesKey("last_surah_arti")] = surah.arti
        prefs[intPreferencesKey("last_ayat_nomor")] = ayat
        prefs[intPreferencesKey("last_surah_total_ayat")] = surah.jumlahAyat
        prefs[longPreferencesKey("last_updated_at")] = now()
    }
}
