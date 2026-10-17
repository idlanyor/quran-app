package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SurahEntity::class,
        AyatEntity::class,
        TafsirEntity::class,
        BookmarkEntity::class,
        KhatamLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile
        private var INSTANCE: QuranDatabase? = null

        fun getInstance(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "nurquran_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
