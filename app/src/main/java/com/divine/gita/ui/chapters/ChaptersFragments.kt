package com.divine.gita.ui.chapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.divine.gita.R
import com.divine.gita.databinding.FragmentListBinding
import com.divine.gita.databinding.ItemChapterBinding
import com.divine.gita.databinding.ItemChapterHeaderBinding
import com.divine.gita.domain.model.Chapter
import com.divine.gita.domain.model.Sloka
import com.divine.gita.domain.model.TranslationLanguage
import com.divine.gita.domain.repository.GitaRepository
import com.divine.gita.domain.repository.PreferencesRepository
import com.divine.gita.ui.common.SlokaAdapter
import com.divine.gita.ui.common.animateItemsOnFirstLoad
import com.divine.gita.ui.common.appViewModels
import com.divine.gita.ui.common.collectWhileStarted
import com.divine.gita.ui.common.navigateTo
import com.divine.gita.ui.common.setupToolbar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------------- Chapter list ----------------

class ChaptersViewModel(repository: GitaRepository) : ViewModel() {
    val chapters: StateFlow<List<Chapter>?> = repository.observeChapters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class ChaptersFragment : Fragment(R.layout.fragment_list) {
    private val viewModel by appViewModels { c, _ -> ChaptersViewModel(c.gitaRepository) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentListBinding.bind(view)
        setupToolbar(b.toolbar, "Bhagavad Gita", "18 Adhyay • 700 Shlok", showBack = false)
        val adapter = ChapterAdapter { navigateTo(R.id.chapterDetailFragment, bundleOf("chapterNumber" to it.number)) }
        b.recycler.animateItemsOnFirstLoad()
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter
        collectWhileStarted(viewModel.chapters) { list ->
            b.progress.isVisible = list == null
            adapter.submitList(list.orEmpty())
        }
    }
}

private class ChapterAdapter(private val onClick: (Chapter) -> Unit) :
    ListAdapter<Chapter, ChapterAdapter.VH>(object : DiffUtil.ItemCallback<Chapter>() {
        override fun areItemsTheSame(a: Chapter, b: Chapter) = a.number == b.number
        override fun areContentsTheSame(a: Chapter, b: Chapter) = a == b
    }) {
    class VH(val b: ItemChapterBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemChapterBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = getItem(position)
        with(holder.b) {
            number.text = ch.number.toString()
            name.text = ch.name
            translation.text = ch.nameTranslation
            meaning.text = ch.nameMeaning
            count.text = root.context.getString(R.string.verses_count, ch.versesCount)
            root.setOnClickListener { onClick(ch) }
        }
    }
}

// ---------------- Chapter detail ----------------

data class ChapterDetailState(
    val chapter: Chapter? = null,
    val slokas: List<Sloka> = emptyList(),
    val language: TranslationLanguage = TranslationLanguage.HINDI,
    val loading: Boolean = true,
)

class ChapterDetailViewModel(
    handle: SavedStateHandle,
    private val repository: GitaRepository,
    preferences: PreferencesRepository,
) : ViewModel() {
    private val number: Int = checkNotNull(handle["chapterNumber"])

    val state: StateFlow<ChapterDetailState> = combine(
        repository.observeChapter(number),
        repository.observeSlokasForChapter(number),
        preferences.translationLanguage,
    ) { chapter, slokas, lang -> ChapterDetailState(chapter, slokas, lang, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChapterDetailState())

    fun toggleFavorite(sloka: Sloka) {
        viewModelScope.launch { repository.setSlokaFavorite(sloka.id, !sloka.isFavorite) }
    }
}

class ChapterDetailFragment : Fragment(R.layout.fragment_list) {
    private val viewModel by appViewModels { c, h -> ChapterDetailViewModel(h, c.gitaRepository, c.preferences) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentListBinding.bind(view)
        setupToolbar(b.toolbar, "Chapter")
        val header = ChapterHeaderAdapter()
        val slokas = SlokaAdapter(
            onClick = { navigateTo(R.id.slokaDetailFragment, bundleOf("slokaId" to it.id)) },
            onToggleFavorite = viewModel::toggleFavorite,
        )
        b.recycler.animateItemsOnFirstLoad()
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = ConcatAdapter(header, slokas)

        collectWhileStarted(viewModel.state) { s ->
            b.progress.isVisible = s.loading
            s.chapter?.let {
                b.toolbar.title = it.nameTranslation
                b.toolbar.subtitle = "Adhyay ${it.number}"
            }
            header.update(s.chapter, s.language)
            slokas.language = s.language
            slokas.submitList(s.slokas)
        }
    }
}

/** Single-item adapter for the expandable chapter summary at the top of the verse list. */
private class ChapterHeaderAdapter : RecyclerView.Adapter<ChapterHeaderAdapter.VH>() {
    class VH(val b: ItemChapterHeaderBinding) : RecyclerView.ViewHolder(b.root)

    private var chapter: Chapter? = null
    private var language = TranslationLanguage.HINDI
    private var expanded = false

    fun update(chapter: Chapter?, language: TranslationLanguage) {
        val hadItem = this.chapter != null
        this.chapter = chapter; this.language = language
        when {
            !hadItem && chapter != null -> notifyItemInserted(0)
            hadItem && chapter != null -> notifyItemChanged(0)
        }
    }

    override fun getItemCount() = if (chapter == null) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemChapterHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = chapter ?: return
        with(holder.b) {
            name.text = ch.name
            transliterated.text = ch.nameTransliterated
            summary.text = if (language == TranslationLanguage.ENGLISH) ch.summaryEnglish else ch.summaryHindi
            summary.maxLines = if (expanded) Int.MAX_VALUE else 4
            toggle.setText(if (expanded) R.string.show_less else R.string.read_summary)
            toggle.setOnClickListener { expanded = !expanded; notifyItemChanged(0) }
        }
    }
}
