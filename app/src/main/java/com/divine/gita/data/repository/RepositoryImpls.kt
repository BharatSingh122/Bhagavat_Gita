package com.divine.gita.data.repository

import com.divine.gita.data.local.dao.ChalisaDao
import com.divine.gita.data.local.dao.ChapterDao
import com.divine.gita.data.local.dao.MoodDao
import com.divine.gita.data.local.dao.QuoteDao
import com.divine.gita.data.local.dao.SlokaDao
import com.divine.gita.domain.model.Chalisa
import com.divine.gita.domain.model.Chapter
import com.divine.gita.domain.model.Mood
import com.divine.gita.domain.model.Quote
import com.divine.gita.domain.model.QuoteCategory
import com.divine.gita.domain.model.Sloka
import com.divine.gita.domain.repository.ChalisaRepository
import com.divine.gita.domain.repository.GitaRepository
import com.divine.gita.domain.repository.MoodRepository
import com.divine.gita.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GitaRepositoryImpl(
    private val chapterDao: ChapterDao,
    private val slokaDao: SlokaDao,
) : GitaRepository {
    override fun observeChapters(): Flow<List<Chapter>> =
        chapterDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeChapter(number: Int): Flow<Chapter?> =
        chapterDao.observe(number).map { it?.toDomain() }

    override fun observeSlokasForChapter(chapter: Int): Flow<List<Sloka>> =
        slokaDao.observeByChapter(chapter).map { list -> list.map { it.toDomain() } }

    override fun observeSloka(id: Int): Flow<Sloka?> =
        slokaDao.observe(id).map { it?.toDomain() }.distinctUntilChanged()

    override fun observeSlokaAtIndex(index: Int): Flow<Sloka?> =
        slokaDao.observeAtIndex(index).map { it?.toDomain() }.distinctUntilChanged()

    override fun observeFavoriteSlokas(): Flow<List<Sloka>> =
        slokaDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    override suspend fun slokaCount(): Int = slokaDao.count()

    override suspend fun neighbourIds(id: Int): Pair<Int?, Int?> =
        slokaDao.previousId(id) to slokaDao.nextId(id)

    override suspend fun setSlokaFavorite(id: Int, favorite: Boolean) = slokaDao.setFavorite(id, favorite)
}

class QuoteRepositoryImpl(
    private val quoteDao: QuoteDao,
) : QuoteRepository {
    override fun observeCategories(): Flow<List<QuoteCategory>> =
        quoteDao.observeCategories().map { keys -> keys.map(QuoteCategory::fromKey).distinct() }

    override fun observeQuotes(category: QuoteCategory): Flow<List<Quote>> =
        quoteDao.observeByCategory(category.key).map { list -> list.map { it.toDomain() } }

    override fun observeFavoriteQuotes(): Flow<List<Quote>> =
        quoteDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    override suspend fun setQuoteFavorite(id: Long, favorite: Boolean) = quoteDao.setFavorite(id, favorite)
}

class ChalisaRepositoryImpl(
    private val chalisaDao: ChalisaDao,
) : ChalisaRepository {
    override fun observeAll(): Flow<List<Chalisa>> =
        chalisaDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observe(id: Int): Flow<Chalisa?> = chalisaDao.observe(id).map { it?.toDomain() }
}

class MoodRepositoryImpl(
    private val moodDao: MoodDao,
) : MoodRepository {
    override fun observeMoods(): Flow<List<Mood>> =
        moodDao.observeMoods().map { list -> list.map { it.toDomain() } }

    override fun observeMood(key: String): Flow<Mood?> = moodDao.observeMood(key).map { it?.toDomain() }

    override fun observeSlokasForMood(key: String): Flow<List<Sloka>> =
        moodDao.observeSlokasForMood(key).map { list -> list.map { it.toDomain() } }
}
