package com.divine.gita.di

import android.content.Context
import androidx.room.Room
import com.divine.gita.data.local.GitaDatabase
import com.divine.gita.data.preferences.UserPreferencesRepository
import com.divine.gita.data.repository.ChalisaRepositoryImpl
import com.divine.gita.data.repository.GitaRepositoryImpl
import com.divine.gita.data.repository.MoodRepositoryImpl
import com.divine.gita.data.repository.QuoteRepositoryImpl
import com.divine.gita.data.seed.DatabaseSeeder
import com.divine.gita.domain.repository.ChalisaRepository
import com.divine.gita.domain.repository.DataSeeder
import com.divine.gita.domain.repository.GitaRepository
import com.divine.gita.domain.repository.MoodRepository
import com.divine.gita.domain.repository.PreferencesRepository
import com.divine.gita.domain.repository.QuoteRepository
import com.divine.gita.domain.usecase.CalendarProvider
import com.divine.gita.domain.usecase.GetDailySlokaUseCase
import java.util.Calendar

/** Simple manual dependency container (one per app process), created in GitaApplication. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: GitaDatabase by lazy {
        Room.databaseBuilder(appContext, GitaDatabase::class.java, GitaDatabase.NAME)
            // v1 ships bundled content + favourites. Add real Migrations before bumping the version.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }

    val gitaRepository: GitaRepository by lazy { GitaRepositoryImpl(database.chapterDao(), database.slokaDao()) }
    val quoteRepository: QuoteRepository by lazy { QuoteRepositoryImpl(database.quoteDao()) }
    val chalisaRepository: ChalisaRepository by lazy { ChalisaRepositoryImpl(database.chalisaDao()) }
    val moodRepository: MoodRepository by lazy { MoodRepositoryImpl(database.moodDao()) }
    val preferences: PreferencesRepository by lazy { UserPreferencesRepository(appContext) }
    val seeder: DataSeeder by lazy { DatabaseSeeder(appContext, database) }

    private val calendarProvider = CalendarProvider { Calendar.getInstance() }
    fun getDailySlokaUseCase() = GetDailySlokaUseCase(gitaRepository, calendarProvider)
}
