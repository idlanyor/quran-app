package com.irengcloud.quran.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.irengcloud.quran.data.QiblaLocation

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nurquran_prefs")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, CLASSIC
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
    val themeMode: ThemeMode = ThemeMode.CLASSIC,
    val khatamTargetMode: KhatamTargetMode = KhatamTargetMode.VERSES,
    val dailyKhatamTargetAyat: Int = 20,
    val dailyKhatamTargetSurah: Int = 1,
    val totalKhatamAyatCompleted: Int = 0,
    val totalKhatamSurahCompleted: Int = 0,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 5,
    val dailyReminderMinute: Int = 0,
    val prayerProvince: String = "",
    val prayerCity: String = ""
)

class UserPreferencesRepository(context: Context) {

    internal val store = context.dataStore

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
        val REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val PRAYER_PROVINCE = stringPreferencesKey("prayer_province")
        val PRAYER_CITY = stringPreferencesKey("prayer_city")
        val LOCATION_LATITUDE = doublePreferencesKey("qibla_latitude")
        val LOCATION_LONGITUDE = doublePreferencesKey("qibla_longitude")
        val LOCATION_ALTITUDE = doublePreferencesKey("qibla_altitude")
        val LOCATION_ACCURACY = floatPreferencesKey("qibla_accuracy")
        val LOCATION_TIME = longPreferencesKey("qibla_location_time")
        val LOCATION_REGION_PROVINCE = stringPreferencesKey("qibla_region_province")
        val LOCATION_REGION_CITY = stringPreferencesKey("qibla_region_city")
    }

    val qiblaLocationFlow: Flow<QiblaLocation?> = store.data.map { prefs ->
        val latitude = prefs[Keys.LOCATION_LATITUDE]
        val longitude = prefs[Keys.LOCATION_LONGITUDE]
        if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0) null
        else QiblaLocation(latitude, longitude, prefs[Keys.LOCATION_ALTITUDE] ?: 0.0,
            prefs[Keys.LOCATION_ACCURACY] ?: 0f, prefs[Keys.LOCATION_TIME] ?: 0L,
            prefs[Keys.LOCATION_REGION_PROVINCE].orEmpty(), prefs[Keys.LOCATION_REGION_CITY].orEmpty())
    }

    suspend fun saveQiblaLocation(location: QiblaLocation) {
        store.edit { prefs ->
            prefs[Keys.LOCATION_LATITUDE] = location.latitude
            prefs[Keys.LOCATION_LONGITUDE] = location.longitude
            prefs[Keys.LOCATION_ALTITUDE] = location.altitude
            prefs[Keys.LOCATION_ACCURACY] = location.accuracyMeters
            prefs[Keys.LOCATION_TIME] = location.updatedAt
            prefs[Keys.LOCATION_REGION_PROVINCE] = location.regionProvince
            prefs[Keys.LOCATION_REGION_CITY] = location.regionCity
        }
    }

    val lastReadFlow: Flow<LastReadState> = store.data.map { prefs ->
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

    val readerSettingsFlow: Flow<ReaderSettingsState> = store.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.CLASSIC.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.CLASSIC)
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
            totalKhatamSurahCompleted = prefs[Keys.TOTAL_KHATAM_SURAH_COMPLETED] ?: 0,
            dailyReminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: false,
            dailyReminderHour = (prefs[Keys.REMINDER_HOUR] ?: 5).coerceIn(0, 23),
            dailyReminderMinute = (prefs[Keys.REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
            prayerProvince = prefs[Keys.PRAYER_PROVINCE].orEmpty(),
            prayerCity = prefs[Keys.PRAYER_CITY].orEmpty()
        )
    }

    suspend fun updateDailyReminder(enabled: Boolean? = null, hour: Int? = null, minute: Int? = null) {
        store.edit { prefs ->
            enabled?.let { prefs[Keys.REMINDER_ENABLED] = it }
            hour?.let { prefs[Keys.REMINDER_HOUR] = it.coerceIn(0, 23) }
            minute?.let { prefs[Keys.REMINDER_MINUTE] = it.coerceIn(0, 59) }
        }
    }

    suspend fun updatePrayerLocation(province: String, city: String, clearQiblaLocation: Boolean = false) {
        store.edit { prefs ->
            prefs[Keys.PRAYER_PROVINCE] = province
            prefs[Keys.PRAYER_CITY] = city
            if (clearQiblaLocation) {
                prefs.remove(Keys.LOCATION_LATITUDE)
                prefs.remove(Keys.LOCATION_LONGITUDE)
                prefs.remove(Keys.LOCATION_ALTITUDE)
                prefs.remove(Keys.LOCATION_ACCURACY)
                prefs.remove(Keys.LOCATION_TIME)
                prefs.remove(Keys.LOCATION_REGION_PROVINCE)
                prefs.remove(Keys.LOCATION_REGION_CITY)
            }
        }
    }

    suspend fun updateArabicFontSize(sizeSp: Int) {
        store.edit { prefs ->
            prefs[Keys.ARABIC_FONT_SIZE] = sizeSp.coerceIn(22, 44)
        }
    }

    suspend fun updateTranslationFontSize(sizeSp: Int) {
        store.edit { prefs ->
            prefs[Keys.TRANSLATION_FONT_SIZE] = sizeSp.coerceIn(12, 22)
        }
    }

    suspend fun updateShowLatin(show: Boolean) {
        store.edit { prefs ->
            prefs[Keys.SHOW_LATIN] = show
        }
    }

    suspend fun updateShowTranslation(show: Boolean) {
        store.edit { prefs ->
            prefs[Keys.SHOW_TRANSLATION] = show
        }
    }

    suspend fun updateAutoNextAudio(autoNext: Boolean) {
        store.edit { prefs ->
            prefs[Keys.AUTO_NEXT_AUDIO] = autoNext
        }
    }

    suspend fun updateAutoScrollSpeed(speedLevel: Int) {
        store.edit { prefs ->
            prefs[Keys.AUTO_SCROLL_SPEED] = speedLevel.coerceIn(1, 10)
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        store.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun updateKhatamTargetMode(mode: KhatamTargetMode) {
        store.edit { prefs ->
            prefs[Keys.KHATAM_TARGET_MODE] = mode.name
        }
    }

    suspend fun updateDailyKhatamTarget(targetAyat: Int) {
        store.edit { prefs ->
            prefs[Keys.DAILY_TARGET_AYAT] = targetAyat.coerceIn(1, 600)
        }
    }

    suspend fun updateDailyKhatamTargetSurah(targetSurah: Int) {
        store.edit { prefs ->
            prefs[Keys.DAILY_TARGET_SURAH] = targetSurah.coerceIn(1, 30)
        }
    }

}
