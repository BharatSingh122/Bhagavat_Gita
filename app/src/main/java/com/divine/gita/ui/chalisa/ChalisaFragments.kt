package com.divine.gita.ui.chalisa

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.divine.gita.R
import com.divine.gita.databinding.FragmentListBinding
import com.divine.gita.databinding.ItemChalisaBinding
import com.divine.gita.databinding.ItemChalisaVerseBinding
import com.divine.gita.domain.model.Chalisa
import com.divine.gita.domain.repository.ChalisaRepository
import com.divine.gita.ui.common.IntentUtils
import com.divine.gita.ui.common.ShareFormatter
import com.divine.gita.ui.common.appViewModels
import com.divine.gita.ui.common.collectWhileStarted
import com.divine.gita.ui.common.copyToClipboard
import com.divine.gita.ui.common.setupToolbar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// ---------------- List ----------------

class ChalisaListViewModel(repository: ChalisaRepository) : ViewModel() {
    val items: StateFlow<List<Chalisa>?> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class ChalisaListFragment : Fragment(R.layout.fragment_list) {
    private val viewModel by appViewModels { c, _ -> ChalisaListViewModel(c.chalisaRepository) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentListBinding.bind(view)
        setupToolbar(b.toolbar, "Chalisa Sangrah", "भक्ति")
        val adapter = ChalisaAdapter { findNavController().navigate(R.id.chalisaDetailFragment, bundleOf("chalisaId" to it.id)) }
        b.recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        b.recycler.adapter = adapter
        collectWhileStarted(viewModel.items) { list ->
            b.progress.isVisible = list == null
            adapter.submit(list.orEmpty())
        }
    }
}

private val TITLE_PREFIXES = setOf("श्री", "माँ", "भगवान", "काल")

private class ChalisaAdapter(private val onClick: (Chalisa) -> Unit) : RecyclerView.Adapter<ChalisaAdapter.VH>() {
    class VH(val b: ItemChalisaBinding) : RecyclerView.ViewHolder(b.root)
    private var items: List<Chalisa> = emptyList()

    fun submit(list: List<Chalisa>) {
        if (list == items) return
        items = list
        @Suppress("NotifyDataSetChanged") notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemChalisaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        with(holder.b) {
            // First Devanagari letter of the deity's name, e.g. "ह" for श्री हनुमान
            val words = c.deity.hindi.split(' ')
            val name = if (words.size > 1 && words[0] in TITLE_PREFIXES) words[1] else words[0]
            initial.text = name.take(1)
            deityHindi.text = c.deity.hindi
            deityName.text = c.deity.key
            root.setOnClickListener { onClick(c) }
        }
    }
}

// ---------------- Detail (reading screen) ----------------

class ChalisaDetailViewModel(handle: SavedStateHandle, repository: ChalisaRepository) : ViewModel() {
    private val id: Int = checkNotNull(handle["chalisaId"])
    val chalisa: StateFlow<Result<Chalisa?>?> = repository.observe(id)
        .map { Result.success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class ChalisaDetailFragment : Fragment(R.layout.fragment_list) {
    private val viewModel by appViewModels { c, h -> ChalisaDetailViewModel(h, c.chalisaRepository) }
    private var textScale = 1f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentListBinding.bind(view)
        textScale = savedInstanceState?.getFloat(KEY_SCALE) ?: 1f
        setupToolbar(b.toolbar, "Chalisa")
        b.toolbar.inflateMenu(R.menu.menu_chalisa)
        val adapter = VerseAdapter()
        adapter.textScale = textScale
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        var current: Chalisa? = null
        b.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_text_smaller -> { textScale = (textScale - 0.1f).coerceAtLeast(0.8f); adapter.textScale = textScale }
                R.id.action_text_larger -> { textScale = (textScale + 0.1f).coerceAtMost(1.6f); adapter.textScale = textScale }
                R.id.action_copy -> current?.let { requireContext().copyToClipboard(ShareFormatter.chalisa(it), b.root) }
                R.id.action_share -> current?.let { IntentUtils.shareText(requireContext(), ShareFormatter.chalisa(it)) }
            }
            true
        }

        collectWhileStarted(viewModel.chalisa) { result ->
            b.progress.isVisible = result == null
            val chalisa = result?.getOrNull()
            current = chalisa
            if (chalisa != null) {
                b.toolbar.title = chalisa.title
                b.toolbar.subtitle = chalisa.deity.hindi
            }
            val empty = result != null && (chalisa == null || chalisa.verses.isEmpty())
            b.emptyText.isVisible = empty
            b.emptyText.setText(R.string.chalisa_empty)
            adapter.verses = chalisa?.verses.orEmpty()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat(KEY_SCALE, textScale)
    }

    private companion object { const val KEY_SCALE = "text_scale" }
}

private class VerseAdapter : RecyclerView.Adapter<VerseAdapter.VH>() {
    class VH(val b: ItemChalisaVerseBinding) : RecyclerView.ViewHolder(b.root)

    var verses: List<String> = emptyList()
        set(value) { if (field != value) { field = value; @Suppress("NotifyDataSetChanged") notifyDataSetChanged() } }
    var textScale = 1f
        set(value) { field = value; notifyItemRangeChanged(0, itemCount) }

    override fun getItemCount() = verses.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemChalisaVerseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.b.index.text = (position + 1).toString()
        holder.b.verse.text = verses[position].replace("। ", "।\n")
        holder.b.verse.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f * textScale)
    }
}
