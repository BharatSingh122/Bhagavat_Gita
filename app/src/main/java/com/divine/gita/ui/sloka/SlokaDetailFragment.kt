package com.divine.gita.ui.sloka

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divine.gita.R
import com.divine.gita.databinding.FragmentSlokaDetailBinding
import com.divine.gita.domain.model.Sloka
import com.divine.gita.domain.model.TranslationLanguage
import com.divine.gita.domain.repository.GitaRepository
import com.divine.gita.domain.repository.PreferencesRepository
import com.divine.gita.ui.common.IntentUtils
import com.divine.gita.ui.common.ShareFormatter
import com.divine.gita.ui.common.appViewModels
import com.divine.gita.ui.common.collectWhileStarted
import com.divine.gita.ui.common.copyToClipboard
import com.divine.gita.ui.common.setupToolbar
import com.google.android.material.chip.Chip
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SlokaDetailState(
    val sloka: Sloka? = null,
    val previousId: Int? = null,
    val nextId: Int? = null,
    val language: TranslationLanguage = TranslationLanguage.HINDI,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SlokaDetailViewModel(
    private val handle: SavedStateHandle,
    private val repository: GitaRepository,
    preferences: PreferencesRepository,
) : ViewModel() {

    /** Current verse; moves with Previous/Next and survives process death. */
    private val currentId = handle.getStateFlow(KEY_CURRENT, checkNotNull(handle.get<Int>("slokaId")))

    val state: StateFlow<SlokaDetailState> = combine(
        currentId.flatMapLatest { repository.observeSloka(it) },
        currentId.flatMapLatest { id -> flow { emit(repository.neighbourIds(id)) } },
        preferences.translationLanguage,
    ) { sloka, neighbours, lang -> SlokaDetailState(sloka, neighbours.first, neighbours.second, lang) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SlokaDetailState())

    fun goTo(id: Int) { handle[KEY_CURRENT] = id }

    fun toggleFavorite() {
        val sloka = state.value.sloka ?: return
        viewModelScope.launch { repository.setSlokaFavorite(sloka.id, !sloka.isFavorite) }
    }

    private companion object { const val KEY_CURRENT = "current_sloka_id" }
}

class SlokaDetailFragment : Fragment(R.layout.fragment_sloka_detail) {

    private val viewModel by appViewModels { c, h -> SlokaDetailViewModel(h, c.gitaRepository, c.preferences) }
    private var renderedId: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentSlokaDetailBinding.bind(view)
        setupToolbar(b.toolbar, "Shlok", "श्लोक")

        b.toolbar.setOnMenuItemClickListener { item ->
            val s = viewModel.state.value
            val sloka = s.sloka ?: return@setOnMenuItemClickListener false
            when (item.itemId) {
                R.id.action_favorite -> viewModel.toggleFavorite()
                R.id.action_copy -> requireContext().copyToClipboard(ShareFormatter.sloka(sloka, s.language), b.root)
                R.id.action_share -> IntentUtils.shareText(requireContext(), ShareFormatter.sloka(sloka, s.language))
            }
            true
        }

        collectWhileStarted(viewModel.state) { s ->
            val sloka = s.sloka ?: return@collectWhileStarted
            b.toolbar.title = "Chapter ${sloka.chapter} • Verse ${sloka.verse}"
            b.toolbar.menu.findItem(R.id.action_favorite)?.apply {
                setIcon(if (sloka.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
                icon?.setTint(ContextCompat.getColor(requireContext(), if (sloka.isFavorite) R.color.favorite else R.color.text_primary))
            }

            b.reference.text = "॥ ${sloka.reference} ॥"
            b.sanskrit.text = sloka.sanskrit
            b.transliteration.text = sloka.transliteration
            b.hindi.text = sloka.hindiTranslation
            b.english.text = sloka.englishTranslation
            b.hindiCard.isVisible = s.language != TranslationLanguage.ENGLISH
            b.englishCard.isVisible = s.language != TranslationLanguage.HINDI
            b.wordsCard.isVisible = sloka.wordMeanings.isNotBlank()
            if (renderedId != sloka.id) {
                renderWordMeanings(b, sloka.wordMeanings)
                b.scroll.scrollTo(0, 0)
                renderedId = sloka.id
            }

            b.previousButton.isEnabled = s.previousId != null
            b.nextButton.isEnabled = s.nextId != null
            b.previousButton.setOnClickListener { s.previousId?.let(viewModel::goTo) }
            b.nextButton.setOnClickListener { s.nextId?.let(viewModel::goTo) }
        }
    }

    /** "word—meaning; word—meaning" → chips with the Sanskrit word highlighted. */
    private fun renderWordMeanings(b: FragmentSlokaDetailBinding, raw: String) {
        b.wordChips.removeAllViews()
        val saffron = ContextCompat.getColor(requireContext(), R.color.sanskrit)
        raw.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
            val idx = part.indexOf('—')
            val word = if (idx > 0) part.substring(0, idx).trim() else part
            val meaning = if (idx > 0) part.substring(idx + 1).trim() else ""
            val text = SpannableStringBuilder(word).apply {
                setSpan(ForegroundColorSpan(saffron), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (meaning.isNotEmpty()) append("  ").append(meaning)
            }
            b.wordChips.addView(Chip(requireContext()).apply {
                this.text = text
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.card_soft)
                setChipStrokeColorResource(R.color.card_stroke)
                chipStrokeWidth = resources.displayMetrics.density
                setEnsureMinTouchTargetSize(false)
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderedId = null
    }
}
