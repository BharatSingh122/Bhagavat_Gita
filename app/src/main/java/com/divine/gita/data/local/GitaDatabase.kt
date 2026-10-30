package com.divine.gita.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.divine.gita.data.local.dao.ChalisaDao
import com.divine.gita.data.local.dao.ChapterDao
import com.divine.gita.data.local.dao.MoodDao
import com.divine.gita.data.local.dao.QuoteDao
import com.divine.gita.data.local.dao.SlokaDao
import com.divine.gita.data.local.entity.ChalisaEntity
import com.divine.gita.data.local.entity.ChapterEntity
import com.divine.gita.data.local.entity.MoodEntity
import com.divine.gita.data.local.entity.MoodSlokaCrossRef
import com.divine.gita.data.local.entity.QuoteEntity
import com.divine.gita.data.local.entity.SlokaEntity

@Database(
    entities = [
        ChapterEntity::class,
        SlokaEntity::class,
        QuoteEntity::class,
        ChalisaEntity::class,
        MoodEntity::class,
        MoodSlokaCrossRef::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GitaDatabase : RoomDatabase() {
    abstract fun chapterDao(): ChapterDao
    abstract fun slokaDao(): SlokaDao
    abstract fun quoteDao(): QuoteDao
    abstract fun chalisaDao(): ChalisaDao
    abstract fun moodDao(): MoodDao

    companion object {
        const val NAME = "divine_gita.db"

        /**
         * v2 splits chalisa `verses` into `doha` / `chalisa` / `closing_doha`. The chalisa table only
         * holds bundled content (no favourites), so it is recreated empty and reseeded from assets.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `chalisa`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chalisa` (`id` INTEGER NOT NULL, `deity` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `doha` TEXT NOT NULL, `chalisa` TEXT NOT NULL, " +
                        "`closing_doha` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }
    }
}
