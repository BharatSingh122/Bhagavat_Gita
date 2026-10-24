package com.divine.gita.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey @ColumnInfo(name = "number") val number: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "name_translation") val nameTranslation: String,
    @ColumnInfo(name = "name_transliterated") val nameTransliterated: String,
    @ColumnInfo(name = "name_meaning") val nameMeaning: String,
    @ColumnInfo(name = "summary_english") val summaryEnglish: String,
    @ColumnInfo(name = "summary_hindi") val summaryHindi: String,
    @ColumnInfo(name = "verses_count") val versesCount: Int,
)

@Entity(
    tableName = "slokas",
    indices = [
        Index(value = ["chapter_number", "verse_number"], unique = true),
        Index(value = ["is_favorite"]),
    ],
)
data class SlokaEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
    @ColumnInfo(name = "verse_number") val verseNumber: Int,
    @ColumnInfo(name = "sanskrit") val sanskrit: String,
    @ColumnInfo(name = "transliteration") val transliteration: String,
    @ColumnInfo(name = "word_meanings") val wordMeanings: String,
    @ColumnInfo(name = "hindi_translation") val hindiTranslation: String,
    @ColumnInfo(name = "english_translation") val englishTranslation: String,
    @ColumnInfo(name = "is_favorite", defaultValue = "0") val isFavorite: Boolean = false,
)

@Entity(
    tableName = "quotes",
    indices = [
        Index(value = ["category", "source_id"], unique = true),
        Index(value = ["is_favorite"]),
    ],
)
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Id inside its category in quotes.json (ids repeat across categories). */
    @ColumnInfo(name = "source_id") val sourceId: Int,
    /** QuoteCategory.key, e.g. "Karm", "Jivan Gyan". */
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "category_order") val categoryOrder: Int,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "is_favorite", defaultValue = "0") val isFavorite: Boolean = false,
)

@Entity(tableName = "chalisa")
data class ChalisaEntity(
    @PrimaryKey val id: Int,
    /** Deity.key, e.g. "Hanuman". */
    @ColumnInfo(name = "deity") val deity: String,
    @ColumnInfo(name = "title") val title: String,
    /** Stored as a JSON array via [com.divine.gita.data.local.Converters]. */
    @ColumnInfo(name = "verses") val verses: List<String>,
)

@Entity(tableName = "moods")
data class MoodEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "subtitle") val subtitle: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

/** Many-to-many: which slokas answer which mood. */
@Entity(
    tableName = "mood_sloka_cross_ref",
    primaryKeys = ["mood_key", "sloka_id"],
    foreignKeys = [
        ForeignKey(entity = MoodEntity::class, parentColumns = ["key"], childColumns = ["mood_key"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SlokaEntity::class, parentColumns = ["id"], childColumns = ["sloka_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sloka_id")],
)
data class MoodSlokaCrossRef(
    @ColumnInfo(name = "mood_key") val moodKey: String,
    @ColumnInfo(name = "sloka_id") val slokaId: Int,
    @ColumnInfo(name = "position") val position: Int,
)

/** Projection used by the mood grid. */
data class MoodWithCount(
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "subtitle") val subtitle: String,
    @ColumnInfo(name = "verse_count") val verseCount: Int,
)
