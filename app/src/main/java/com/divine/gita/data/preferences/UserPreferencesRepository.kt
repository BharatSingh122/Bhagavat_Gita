package com.divine.gita.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.divine.gita.domain.model.TranslationLanguage
import com.divine.gita.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(
    private val context: Context,
) : PreferencesRepository {

    private val prefs: Flow<Preferences> = context.dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val onboardingCompleted: Flow<Boolean> =
        prefs.map { it[KEY_ONBOARDING] ?: false }.distinctUntilChanged()

    override val translationLanguage: Flow<TranslationLanguage> = prefs.map { p ->
        p[KEY_LANGUAGE]?.let { stored -> TranslationLanguage.entries.firstOrNull { it.name == stored } }
            ?: TranslationLanguage.HINDI
    }.distinctUntilChanged()

    override suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[KEY_ONBOARDING] = true }
    }

    override suspend fun setTranslationLanguage(language: TranslationLanguage) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language.name }
    }

    private companion object {
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val KEY_LANGUAGE = stringPreferencesKey("translation_language")
    }
}
