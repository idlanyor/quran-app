package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nurquran_prefs")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class KhatamTargetMode(val label: String, val unitLabel: String) {
    VERSES("Target Ayat", "Ayat"),
    CHAPTERS("Target Surah", "Surah")
}

data class LastReadState(
    val surahNomor: Int = 1,
    val surahNamaLatin: String = "Al-Fatihah",
    val surahNamaArab: String = "الفاتحة",
    val surahArti: String = "Pembukaan",
    val ayatNomor: Int = 1,
    val totalAyatSurah: Int = 7,
    val updatedAt: Long = 0L
)

data class ReaderSettingsState(
    val arabicFontSizeSp: Int = 30,
    val translationFontSizeSp: Int = 15,
    val showLatin: Boolean = true,
    val showTranslation: Boolean = true,
    val autoNextAudio: Boolean = true,
    val autoScrollSpeedLevel: Int = 3,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val khatamTargetMode: KhatamTargetMode = KhatamTargetMode.VERSES,
    val dailyKhatamTargetAyat: Int = 20,
    val dailyKhatamTargetSurah: Int = 1,
    val totalKhatamAyatCompleted: Int = 0,
    val totalKhatamSurahCompleted: Int = 0
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LAST_SURAH_NOMOR = intPreferencesKey("last_surah_nomor")
        val LAST_SURAH_LATIN = stringPreferencesKey("last_surah_latin")
        val LAST_SURAH_ARAB = stringPreferencesKey("last_surah_arab")
        val LAST_SURAH_ARTI = stringPreferencesKey("last_surah_arti")
        val LAST_AYAT_NOMOR = intPreferencesKey("last_ayat_nomor")
        val LAST_SURAH_TOTAL_AYAT = intPreferencesKey("last_surah_total_ayat")
        val LAST_UPDATED_AT = longPreferencesKey("last_updated_at")

        val ARABIC_FONT_SIZE = intPreferencesKey("arabic_font_size")
        val TRANSLATION_FONT_SIZE = intPreferencesKey("translation_font_size")
        val SHOW_LATIN = booleanPreferencesKey("show_latin")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val AUTO_NEXT_AUDIO = booleanPreferencesKey("auto_next_audio")
        val AUTO_SCROLL_SPEED = intPreferencesKey("auto_scroll_speed")
        val THEME_MODE = stringPreferencesKey("theme_mode")

        val KHATAM_TARGET_MODE = stringPreferencesKey("khatam_target_mode")
        val DAILY_TARGET_AYAT = intPreferencesKey("daily_target_ayat")
        val DAILY_TARGET_SURAH = intPreferencesKey("daily_target_surah")
        val TOTAL_KHATAM_COMPLETED = intPreferencesKey("total_khatam_completed")
        val TOTAL_KHATAM_SURAH_COMPLETED = intPreferencesKey("total_khatam_surah_completed")
    }

    val lastReadFlow: Flow<LastReadState> = context.dataStore.data.map { prefs ->
        LastReadState(
            surahNomor = prefs[Keys.LAST_SURAH_NOMOR] ?: 1,
            surahNamaLatin = prefs[Keys.LAST_SURAH_LATIN] ?: "Al-Fatihah",
            surahNamaArab = prefs[Keys.LAST_SURAH_ARAB] ?: "الفاتحة",
            surahArti = prefs[Keys.LAST_SURAH_ARTI] ?: "Pembukaan",
            ayatNomor = prefs[Keys.LAST_AYAT_NOMOR] ?: 1,
            totalAyatSurah = prefs[Keys.LAST_SURAH_TOTAL_AYAT] ?: 7,
            updatedAt = prefs[Keys.LAST_UPDATED_AT] ?: 0L
        )
    }

    val readerSettingsFlow: Flow<ReaderSettingsState> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)
        val targetModeStr = prefs[Keys.KHATAM_TARGET_MODE] ?: KhatamTargetMode.VERSES.name
        val targetMode = runCatching { KhatamTargetMode.valueOf(targetModeStr) }
            .getOrDefault(KhatamTargetMode.VERSES)

        ReaderSettingsState(
            arabicFontSizeSp = prefs[Keys.ARABIC_FONT_SIZE] ?: 30,
            translationFontSizeSp = prefs[Keys.TRANSLATION_FONT_SIZE] ?: 15,
            showLatin = prefs[Keys.SHOW_LATIN] ?: true,
            showTranslation = prefs[Keys.SHOW_TRANSLATION] ?: true,
            autoNextAudio = prefs[Keys.AUTO_NEXT_AUDIO] ?: true,
            autoScrollSpeedLevel = prefs[Keys.AUTO_SCROLL_SPEED] ?: 3,
            themeMode = themeMode,
            khatamTargetMode = targetMode,
            dailyKhatamTargetAyat = prefs[Keys.DAILY_TARGET_AYAT] ?: 20,
            dailyKhatamTargetSurah = prefs[Keys.DAILY_TARGET_SURAH] ?: 1,
            totalKhatamAyatCompleted = prefs[Keys.TOTAL_KHATAM_COMPLETED] ?: 0,
            totalKhatamSurahCompleted = prefs[Keys.TOTAL_KHATAM_SURAH_COMPLETED] ?: 0
        )
    }

    suspend fun saveLastRead(
        surahNomor: Int,
        surahNamaLatin: String,
        surahNamaArab: String,
        surahArti: String,
        ayatNomor: Int,
        totalAyatSurah: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SURAH_NOMOR] = surahNomor
            prefs[Keys.LAST_SURAH_LATIN] = surahNamaLatin
            prefs[Keys.LAST_SURAH_ARAB] = surahNamaArab
            prefs[Keys.LAST_SURAH_ARTI] = surahArti
            prefs[Keys.LAST_AYAT_NOMOR] = ayatNomor
            prefs[Keys.LAST_SURAH_TOTAL_AYAT] = totalAyatSurah
            prefs[Keys.LAST_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updateArabicFontSize(sizeSp: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ARABIC_FONT_SIZE] = sizeSp.coerceIn(22, 44)
        }
    }

    suspend fun updateTranslationFontSize(sizeSp: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TRANSLATION_FONT_SIZE] = sizeSp.coerceIn(12, 22)
        }
    }

    suspend fun updateShowLatin(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_LATIN] = show
        }
    }

    suspend fun updateShowTranslation(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_TRANSLATION] = show
        }
    }

    suspend fun updateAutoNextAudio(autoNext: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_NEXT_AUDIO] = autoNext
        }
    }

    suspend fun updateAutoScrollSpeed(speedLevel: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SCROLL_SPEED] = speedLevel.coerceIn(1, 10)
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun updateKhatamTargetMode(mode: KhatamTargetMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.KHATAM_TARGET_MODE] = mode.name
        }
    }

    suspend fun updateDailyKhatamTarget(targetAyat: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_TARGET_AYAT] = targetAyat.coerceIn(1, 600)
        }
    }

    suspend fun updateDailyKhatamTargetSurah(targetSurah: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_TARGET_SURAH] = targetSurah.coerceIn(1, 30)
        }
    }

    suspend fun incrementKhatamProgress(addedAyat: Int, addedSurah: Int = 0) {
        context.dataStore.edit { prefs ->
            if (addedAyat > 0) {
                val currentAyat = prefs[Keys.TOTAL_KHATAM_COMPLETED] ?: 0
                prefs[Keys.TOTAL_KHATAM_COMPLETED] = (currentAyat + addedAyat).coerceIn(0, 6236)
            }
            if (addedSurah > 0) {
                val currentSurah = prefs[Keys.TOTAL_KHATAM_SURAH_COMPLETED] ?: 0
                prefs[Keys.TOTAL_KHATAM_SURAH_COMPLETED] = (currentSurah + addedSurah).coerceIn(0, 114)
            }
        }
    }

    suspend fun resetKhatamProgress() {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOTAL_KHATAM_COMPLETED] = 0
            prefs[Keys.TOTAL_KHATAM_SURAH_COMPLETED] = 0
        }
    }
}
