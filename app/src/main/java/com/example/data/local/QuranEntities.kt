package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val nomor: Int,
    val namaArab: String,
    val namaLatin: String,
    val arti: String,
    val jumlahAyat: Int,
    val tempatTurun: String,
    val deskripsi: String = "",
    val audioFullUrl: String = "",
    val isCachedOffline: Boolean = false
)

@Entity(tableName = "ayats", primaryKeys = ["surahNomor", "nomorAyat"])
data class AyatEntity(
    val surahNomor: Int,
    val nomorAyat: Int,
    val teksArab: String,
    val teksLatin: String,
    val teksIndonesia: String,
    val audioUrl: String = ""
)

@Entity(tableName = "tafsirs", primaryKeys = ["surahNomor", "nomorAyat"])
data class TafsirEntity(
    val surahNomor: Int,
    val nomorAyat: Int,
    val teksTafsir: String
)

@Entity(tableName = "bookmarks", primaryKeys = ["surahNomor", "nomorAyat"])
data class BookmarkEntity(
    val surahNomor: Int,
    val surahNamaLatin: String,
    val surahNamaArab: String,
    val nomorAyat: Int,
    val teksArab: String,
    val teksIndonesia: String,
    val catatan: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "khatam_logs")
data class KhatamLogEntity(
    @PrimaryKey val dateKey: String, // YYYY-MM-DD
    val ayatCountRead: Int,
    val surahCountRead: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class SearchAyatItem(
    val surahNomor: Int,
    val surahNamaLatin: String,
    val surahNamaArab: String,
    val nomorAyat: Int,
    val teksArab: String,
    val teksLatin: String,
    val teksIndonesia: String,
    val audioUrl: String = ""
)

data class PaginatedSearchResult(
    val items: List<SearchAyatItem>,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val hasMore: Boolean
)
