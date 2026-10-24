package com.divine.gita.ui.home

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.divine.gita.R
import com.divine.gita.databinding.FragmentHomeBinding
import com.divine.gita.databinding.ViewCategoryCardBinding
import com.divine.gita.domain.model.DailySloka
import com.divine.gita.domain.model.TranslationLanguage
import com.divine.gita.domain.repository.GitaRepository
import com.divine.gita.domain.repository.PreferencesRepository
import com.divine.gita.domain.usecase.GetDailySlokaUseCase
import com.divine.gita.ui.common.appViewModels
import com.divine.gita.ui.common.collectWhileStarted
import com.divine.gita.ui.common.previewFor
import com.divine.gita.ui.common.showFavorite
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HomeUiState(
    val daily: DailySloka? = null,
    val language: TranslationLanguage = TranslationLanguage.HINDI,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val getDailySloka: GetDailySlokaUseCase,
    private val gitaRepository: GitaRepository,
    preferences: PreferencesRepository,
) : ViewModel() {

    /** Changes at midnight; re-checked every time the screen resumes. */
    private val dayOfYear = MutableStateFlow(getDailySloka.currentDayOfYear())

    private val daily = dayOfYear.flatMapLatest {
        flow { emitAll(getDailySloka()) }
            .map<DailySloka, DailySloka?> { it }
            .onStart { emit(null) }
    }

    val uiState: StateFlow<HomeUiState> = combine(daily, preferences.translationLanguage) { d, lang -> HomeUiState(d, lang) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onResume() { dayOfYear.value = getDailySloka.currentDayOfYear() }

    fun toggleFavorite() {
        val sloka = uiState.value.daily?.sloka ?: return
        viewModelScope.launch { gitaRepository.setSlokaFavorite(sloka.id, !sloka.isFavorite) }
    }
}

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel by appViewModels { c, _ ->
        HomeViewModel(c.getDailySlokaUseCase(), c.gitaRepository, c.preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHomeBinding.bind(view)
        val nav = findNavController()

        b.settingsButton.setOnClickListener { nav.navigate(R.id.settingsFragment) }
        b.dailyFavorite.setOnClickListener { viewModel.toggleFavorite() }
        b.moodRibbon.setOnClickListener { selectTab(R.id.moodFragment) }

        b.cardGita.bind(
            "Bhagavad Gita", "18 Chapters", "Explore the eternal dialogue and complete divine wisdom of Lord Krishna",
            "View Chapters", R.drawable.ic_book, R.drawable.bg_card_gita, R.color.saffron_deep,
        ) { selectTab(R.id.chaptersFragment) }
        b.cardQuotes.bind(
            "Anmol Vichar", "Quotes", "Inspiring thoughts on karma, dharma and bhakti for everyday clarity",
            "Read Vichar", R.drawable.ic_quote, R.drawable.bg_card_quotes, R.color.mood_grateful,
        ) { selectTab(R.id.quotesFragment) }
        b.cardChalisa.bind(
            "Chalisa Sangrah", "15 Deities", "Sing the glory of the divine: Ganesh, Hanuman, Shiv, Lakshmi and more",
            "Open Chalisa", R.drawable.ic_star, R.drawable.bg_card_chalisa, R.color.saffron_deep,
        ) { nav.navigate(R.id.chalisaListFragment) }
        b.cardMood.bind(
            "Gita for Your Mood", "8 Moods", "Krishna's guidance for anxiety, anger, sorrow, confusion and gratitude",
            "Find Solace", R.drawable.ic_mood, R.drawable.bg_card_mood, R.color.mood_anxious,
        ) { selectTab(R.id.moodFragment) }

        collectWhileStarted(viewModel.uiState) { state ->
            val daily = state.daily
            val date = Date(daily?.dateMillis ?: System.currentTimeMillis())
            b.dateDay.text = SimpleDateFormat("d", Locale.getDefault()).format(date)
            b.dateMonthYear.text = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date)
            b.dateWeekday.text = SimpleDateFormat("EEEE", Locale.getDefault()).format(date)

            b.dailyFavorite.visibility = if (daily == null) View.INVISIBLE else View.VISIBLE
            b.readMoreButton.isEnabled = daily != null
            if (daily == null) {
                b.dailySanskrit.setText(R.string.preparing)
                b.dailyTranslation.text = ""
                b.dailyReference.text = ""
            } else {
                val s = daily.sloka
                b.dailySanskrit.text = s.sanskrit
                b.dailyTranslation.text = s.previewFor(state.language)
                b.dailyReference.text = getString(R.string.chapter_verse, s.chapter, s.verse)
                b.dailyFavorite.showFavorite(s.isFavorite)
                b.readMoreButton.setOnClickListener {
                    nav.navigate(R.id.slokaDetailFragment, bundleOf("slokaId" to s.id))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    /** Switch bottom-bar tab so tab state and back stack stay consistent. */
    private fun selectTab(id: Int) {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = id
    }

    private fun ViewCategoryCardBinding.bind(
        title: String, badge: String, body: String, cta: String,
        @DrawableRes icon: Int, @DrawableRes background: Int, accentColor: Int,
        onClick: () -> Unit,
    ) {
        val accent = ContextCompat.getColor(root.context, accentColor)
        this.title.text = title
        this.badge.text = badge
        this.body.text = body
        this.cta.text = cta
        this.cta.setTextColor(accent)
        this.cta.compoundDrawablesRelative[2]?.setTint(accent)
        this.icon.setImageResource(icon)
        this.icon.setColorFilter(accent)
        this.watermark.setImageResource(icon)
        this.background.setBackgroundResource(background)
        root.setOnClickListener { onClick() }
    }
}
