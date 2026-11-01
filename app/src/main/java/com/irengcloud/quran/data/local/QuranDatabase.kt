package com.irengcloud.quran.data.local

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
    exportSchema = true
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile
        private var INSTANCE: QuranDatabase? = null

        /** Add a `Migration(from, to)` here every time [QuranDatabase] `version` is increased. */
        val MIGRATIONS: Array<androidx.room.migration.Migration> = emptyArray()

        fun getInstance(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "nurquran_offline.db"
                )
                    .createFromAsset("database/nurquran_offline.db")
                    // Only the legacy v1 schema (no recorded history) may be wiped. Any future
                    // version bump must ship a Migration, otherwise Room fails loudly instead of
                    // silently deleting the user's bookmarks and khatam progress.
                    .fallbackToDestructiveMigrationFrom(true, 1)
                    .addMigrations(*MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
