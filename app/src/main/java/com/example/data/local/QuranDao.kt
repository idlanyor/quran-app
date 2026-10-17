package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    // Surahs
    @Query("SELECT * FROM surahs ORDER BY nomor ASC")
    fun getAllSurahsFlow(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs ORDER BY nomor ASC")
    suspend fun getAllSurahsOnce(): List<SurahEntity>

    @Query("SELECT * FROM surahs WHERE nomor = :nomor LIMIT 1")
    suspend fun getSurahByNomor(nomor: Int): SurahEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurah(surah: SurahEntity)

    @Query("UPDATE surahs SET isCachedOffline = :cached WHERE nomor = :nomor")
    suspend fun updateSurahOfflineStatus(nomor: Int, cached: Boolean)

    // Ayats
    @Query("SELECT * FROM ayats WHERE surahNomor = :surahNomor ORDER BY nomorAyat ASC")
    fun getAyatsBySurahFlow(surahNomor: Int): Flow<List<AyatEntity>>

    @Query("SELECT * FROM ayats WHERE surahNomor = :surahNomor ORDER BY nomorAyat ASC")
    suspend fun getAyatsBySurahOnce(surahNomor: Int): List<AyatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyats(ayats: List<AyatEntity>)

    @Query("SELECT * FROM ayats WHERE teksIndonesia LIKE '%' || :query || '%' OR teksLatin LIKE '%' || :query || '%' ORDER BY surahNomor ASC, nomorAyat ASC LIMIT :limit OFFSET :offset")
    suspend fun searchCachedAyats(query: String, limit: Int, offset: Int): List<AyatEntity>

    // Tafsirs
    @Query("SELECT * FROM tafsirs WHERE surahNomor = :surahNomor ORDER BY nomorAyat ASC")
    suspend fun getTafsirsBySurahOnce(surahNomor: Int): List<TafsirEntity>

    @Query("SELECT * FROM tafsirs WHERE surahNomor = :surahNomor AND nomorAyat = :nomorAyat LIMIT 1")
    suspend fun getTafsirForAyat(surahNomor: Int, nomorAyat: Int): TafsirEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTafsirs(tafsirs: List<TafsirEntity>)

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarksFlow(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE surahNomor = :surahNomor AND nomorAyat = :nomorAyat")
    suspend fun deleteBookmark(surahNomor: Int, nomorAyat: Int)

    // Khatam Logs
    @Query("SELECT * FROM khatam_logs ORDER BY dateKey DESC LIMIT 30")
    fun getRecentKhatamLogsFlow(): Flow<List<KhatamLogEntity>>

    @Query("SELECT * FROM khatam_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getKhatamLogForDate(dateKey: String): KhatamLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhatamLog(log: KhatamLogEntity)
}
