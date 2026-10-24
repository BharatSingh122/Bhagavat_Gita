package com.divine.gita.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.divine.gita.data.local.entity.ChalisaEntity
import com.divine.gita.data.local.entity.ChapterEntity
import com.divine.gita.data.local.entity.MoodEntity
import com.divine.gita.data.local.entity.MoodSlokaCrossRef
import com.divine.gita.data.local.entity.MoodWithCount
import com.divine.gita.data.local.entity.QuoteEntity
import com.divine.gita.data.local.entity.SlokaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChapterEntity>)

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun count(): Int

    @Query("SELECT * FROM chapters ORDER BY number")
    fun observeAll(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE number = :number")
    fun observe(number: Int): Flow<ChapterEntity?>
}

@Dao
interface SlokaDao {
    /** IGNORE keeps existing rows (and their is_favorite flag) if the seeder ever re-runs. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<SlokaEntity>)

    @Query("SELECT COUNT(*) FROM slokas")
    suspend fun count(): Int

    @Query("SELECT * FROM slokas WHERE chapter_number = :chapter ORDER BY verse_number")
    fun observeByChapter(chapter: Int): Flow<List<SlokaEntity>>

    @Query("SELECT * FROM slokas WHERE id = :id")
    fun observe(id: Int): Flow<SlokaEntity?>

    /** Sloka of the Day: row at a 0-based position in canonical chapter/verse order. */
    @Query("SELECT * FROM slokas ORDER BY chapter_number, verse_number LIMIT 1 OFFSET :index")
    fun observeAtIndex(index: Int): Flow<SlokaEntity?>

    @Query("SELECT * FROM slokas WHERE is_favorite = 1 ORDER BY chapter_number, verse_number")
    fun observeFavorites(): Flow<List<SlokaEntity>>

    @Query("UPDATE slokas SET is_favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Int, favorite: Boolean)

    @Query(
        """SELECT id FROM slokas WHERE (chapter_number * 1000 + verse_number) <
           (SELECT chapter_number * 1000 + verse_number FROM slokas WHERE id = :id)
           ORDER BY chapter_number DESC, verse_number DESC LIMIT 1"""
    )
    suspend fun previousId(id: Int): Int?

    @Query(
        """SELECT id FROM slokas WHERE (chapter_number * 1000 + verse_number) >
           (SELECT chapter_number * 1000 + verse_number FROM slokas WHERE id = :id)
           ORDER BY chapter_number, verse_number LIMIT 1"""
    )
    suspend fun nextId(id: Int): Int?
}

@Dao
interface QuoteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<QuoteEntity>)

    @Query("SELECT COUNT(*) FROM quotes")
    suspend fun count(): Int

    @Query("SELECT category FROM quotes GROUP BY category ORDER BY MIN(category_order)")
    fun observeCategories(): Flow<List<String>>

    @Query("SELECT * FROM quotes WHERE category = :category ORDER BY source_id")
    fun observeByCategory(category: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE is_favorite = 1 ORDER BY category_order, source_id")
    fun observeFavorites(): Flow<List<QuoteEntity>>

    @Query("UPDATE quotes SET is_favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)
}

@Dao
interface ChalisaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChalisaEntity>)

    @Query("SELECT COUNT(*) FROM chalisa")
    suspend fun count(): Int

    @Query("SELECT * FROM chalisa ORDER BY id")
    fun observeAll(): Flow<List<ChalisaEntity>>

    @Query("SELECT * FROM chalisa WHERE id = :id")
    fun observe(id: Int): Flow<ChalisaEntity?>
}

@Dao
interface MoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoods(items: List<MoodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(items: List<MoodSlokaCrossRef>)

    @Query("SELECT COUNT(*) FROM moods")
    suspend fun count(): Int

    @Query(
        """SELECT m.key, m.title, m.subtitle, COUNT(x.sloka_id) AS verse_count
           FROM moods m LEFT JOIN mood_sloka_cross_ref x ON x.mood_key = m.key
           GROUP BY m.key ORDER BY m.sort_order"""
    )
    fun observeMoods(): Flow<List<MoodWithCount>>

    @Query(
        """SELECT m.key, m.title, m.subtitle, COUNT(x.sloka_id) AS verse_count
           FROM moods m LEFT JOIN mood_sloka_cross_ref x ON x.mood_key = m.key
           WHERE m.key = :key GROUP BY m.key"""
    )
    fun observeMood(key: String): Flow<MoodWithCount?>

    @Query(
        """SELECT s.* FROM slokas s INNER JOIN mood_sloka_cross_ref x ON x.sloka_id = s.id
           WHERE x.mood_key = :key ORDER BY x.position"""
    )
    fun observeSlokasForMood(key: String): Flow<List<SlokaEntity>>
}
