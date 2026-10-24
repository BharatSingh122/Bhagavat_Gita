package com.divine.gita.domain.repository

import com.divine.gita.domain.model.Chalisa
import com.divine.gita.domain.model.Chapter
import com.divine.gita.domain.model.Mood
import com.divine.gita.domain.model.Quote
import com.divine.gita.domain.model.QuoteCategory
import com.divine.gita.domain.model.Sloka
import com.divine.gita.domain.model.TranslationLanguage
import kotlinx.coroutines.flow.Flow

interface GitaRepository {
    fun observeChapters(): Flow<List<Chapter>>
    fun observeChapter(number: Int): Flow<Chapter?>
    fun observeSlokasForChapter(chapter: Int): Flow<List<Sloka>>
    fun observeSloka(id: Int): Flow<Sloka?>
    fun observeSlokaAtIndex(index: Int): Flow<Sloka?>
    fun observeFavoriteSlokas(): Flow<List<Sloka>>
    suspend fun slokaCount(): Int
    suspend fun neighbourIds(id: Int): Pair<Int?, Int?>
    suspend fun setSlokaFavorite(id: Int, favorite: Boolean)
}

interface QuoteRepository {
    fun observeCategories(): Flow<List<QuoteCategory>>
    fun observeQuotes(category: QuoteCategory): Flow<List<Quote>>
    fun observeFavoriteQuotes(): Flow<List<Quote>>
    suspend fun setQuoteFavorite(id: Long, favorite: Boolean)
}

interface ChalisaRepository {
    fun observeAll(): Flow<List<Chalisa>>
    fun observe(id: Int): Flow<Chalisa?>
}

interface MoodRepository {
    fun observeMoods(): Flow<List<Mood>>
    fun observeMood(key: String): Flow<Mood?>
    fun observeSlokasForMood(key: String): Flow<List<Sloka>>
}

interface PreferencesRepository {
    val onboardingCompleted: Flow<Boolean>
    val translationLanguage: Flow<TranslationLanguage>
    suspend fun setOnboardingCompleted()
    suspend fun setTranslationLanguage(language: TranslationLanguage)
}

/** Seeds Room from the JSON files in assets/ the first time the app runs. */
interface DataSeeder {
    suspend fun seedIfNeeded()
}
