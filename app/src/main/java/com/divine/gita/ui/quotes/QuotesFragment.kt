package com.divine.gita.ui.quotes

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.divine.gita.R
import com.divine.gita.databinding.FragmentTabbedListBinding
import com.divine.gita.domain.model.Quote
import com.divine.gita.domain.model.QuoteCategory
import com.divine.gita.domain.repository.QuoteRepository
import com.divine.gita.ui.common.IntentUtils
import com.divine.gita.ui.common.QuoteAdapter
import com.divine.gita.ui.common.ShareFormatter
import com.divine.gita.ui.common.appViewModels
import com.divine.gita.ui.common.collectWhileStarted
import com.divine.gita.ui.common.copyToClipboard
import com.divine.gita.ui.common.setupToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class QuotesViewModel(
    private val handle: SavedStateHandle,
    private val repository: QuoteRepository,
) : ViewModel() {

    val categories = QuoteCategory.entries

    val selected: StateFlow<QuoteCategory> = handle.getStateFlow(KEY, QuoteCategory.KARM.name)
        .map { QuoteCategory.valueOf(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuoteCategory.valueOf(handle[KEY] ?: QuoteCategory.KARM.name))

    val quotes: StateFlow<List<Quote>> = selected.flatMapLatest { repository.observeQuotes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(category: QuoteCategory) { handle[KEY] = category.name }

    fun toggleFavorite(quote: Quote) {
        viewModelScope.launch { repository.setQuoteFavorite(quote.id, !quote.isFavorite) }
    }

    private companion object { const val KEY = "quote_category" }
}

/** Anmol Vichar: scrollable category tabs + quote cards with copy / share / favourite. */
class QuotesFragment : Fragment(R.layout.fragment_tabbed_list) {
    private val viewModel by appViewModels { c, h -> QuotesViewModel(h, c.quoteRepository) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentTabbedListBinding.bind(view)
        setupToolbar(b.toolbar, "Anmol Vichar", "Amrit Vani", showBack = false)

        val adapter = QuoteAdapter(
            showCategory = false,
            onToggleFavorite = { q ->
                viewModel.toggleFavorite(q)
                Snackbar.make(b.root, if (q.isFavorite) R.string.removed_favorite else R.string.added_favorite, Snackbar.LENGTH_SHORT).show()
            },
            onCopy = { requireContext().copyToClipboard(ShareFormatter.quote(it), b.root) },
            onShare = { IntentUtils.shareText(requireContext(), ShareFormatter.quote(it)) },
        )
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        b.tabs.tabMode = TabLayout.MODE_SCROLLABLE
        viewModel.categories.forEach { b.tabs.addTab(b.tabs.newTab().setText("${it.hindi} (${it.english})").setTag(it), false) }
        b.tabs.getTabAt(viewModel.categories.indexOf(viewModel.selected.value))?.select()
        b.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.select(tab.tag as QuoteCategory)
                b.recycler.scrollToPosition(0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) { b.recycler.smoothScrollToPosition(0) }
        })

        collectWhileStarted(viewModel.quotes) { adapter.submitList(it) }
    }
}
