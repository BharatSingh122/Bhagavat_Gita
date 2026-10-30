package com.divine.gita.ui.favorites

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.divine.gita.R
import com.divine.gita.databinding.FragmentTabbedListBinding
import com.divine.gita.domain.model.Quote
import com.divine.gita.domain.model.Sloka
import com.divine.gita.domain.model.TranslationLanguage
import com.divine.gita.domain.repository.GitaRepository
import com.divine.gita.domain.repository.PreferencesRepository
import com.divine.gita.domain.repository.QuoteRepository
import com.divine.gita.ui.common.IntentUtils
import com.divine.gita.ui.common.QuoteAdapter
import com.divine.gita.ui.common.ShareFormatter
import com.divine.gita.ui.common.SlokaAdapter
import com.divine.gita.ui.common.animateItemsOnFirstLoad
import com.divine.gita.ui.common.appViewModels
import com.divine.gita.ui.common.collectWhileStarted
import com.divine.gita.ui.common.navigateTo
import com.divine.gita.ui.common.copyToClipboard
import com.divine.gita.ui.common.setupToolbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesState(
    val tab: Int = 0,
    val slokas: List<Sloka> = emptyList(),
    val quotes: List<Quote> = emptyList(),
    val language: TranslationLanguage = TranslationLanguage.HINDI,
)

class FavoritesViewModel(
    private val handle: SavedStateHandle,
    private val gitaRepository: GitaRepository,
    private val quoteRepository: QuoteRepository,
    preferences: PreferencesRepository,
) : ViewModel() {
    val state: StateFlow<FavoritesState> = combine(
        handle.getStateFlow(KEY_TAB, 0),
        gitaRepository.observeFavoriteSlokas(),
        quoteRepository.observeFavoriteQuotes(),
        preferences.translationLanguage,
    ) { tab, slokas, quotes, lang -> FavoritesState(tab, slokas, quotes, lang) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesState(tab = handle[KEY_TAB] ?: 0))

    fun selectTab(tab: Int) { handle[KEY_TAB] = tab }
    fun removeSloka(sloka: Sloka) { viewModelScope.launch { gitaRepository.setSlokaFavorite(sloka.id, false) } }
    fun removeQuote(quote: Quote) { viewModelScope.launch { quoteRepository.setQuoteFavorite(quote.id, false) } }

    private companion object { const val KEY_TAB = "tab" }
}

/** Saved slokas and quotes from Room (is_favorite = 1). */
class FavoritesFragment : Fragment(R.layout.fragment_tabbed_list) {
    private val viewModel by appViewModels { c, h ->
        FavoritesViewModel(h, c.gitaRepository, c.quoteRepository, c.preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentTabbedListBinding.bind(view)
        setupToolbar(b.toolbar, "Favourites", "Saved with devotion", showBack = false)

        val slokaAdapter = SlokaAdapter(
            onClick = { navigateTo(R.id.slokaDetailFragment, bundleOf("slokaId" to it.id)) },
            onToggleFavorite = viewModel::removeSloka,
        )
        val quoteAdapter = QuoteAdapter(
            showCategory = true,
            onToggleFavorite = viewModel::removeQuote,
            onCopy = { requireContext().copyToClipboard(ShareFormatter.quote(it), b.root) },
            onShare = { IntentUtils.shareText(requireContext(), ShareFormatter.quote(it)) },
        )
        b.recycler.animateItemsOnFirstLoad()
        b.recycler.layoutManager = LinearLayoutManager(requireContext())

        b.tabs.tabMode = TabLayout.MODE_FIXED
        b.tabs.addTab(b.tabs.newTab().setText("Slokas"), false)
        b.tabs.addTab(b.tabs.newTab().setText("Vichar"), false)
        b.tabs.getTabAt(viewModel.state.value.tab)?.select()
        b.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = viewModel.selectTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        collectWhileStarted(viewModel.state) { s ->
            b.tabs.getTabAt(0)?.text = "Slokas (${s.slokas.size})"
            b.tabs.getTabAt(1)?.text = "Vichar (${s.quotes.size})"
            slokaAdapter.language = s.language
            slokaAdapter.submitList(s.slokas)
            quoteAdapter.submitList(s.quotes)
            val showingSlokas = s.tab == 0
            val wanted = if (showingSlokas) slokaAdapter else quoteAdapter
            if (b.recycler.adapter !== wanted) b.recycler.adapter = wanted
            val empty = if (showingSlokas) s.slokas.isEmpty() else s.quotes.isEmpty()
            b.emptyText.isVisible = empty
            b.emptyText.setText(if (showingSlokas) R.string.empty_fav_slokas else R.string.empty_fav_quotes)
        }
    }
}
