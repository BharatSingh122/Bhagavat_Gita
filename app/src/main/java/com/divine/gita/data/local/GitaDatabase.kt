package com.divine.gita.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 1,
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
    }
}
