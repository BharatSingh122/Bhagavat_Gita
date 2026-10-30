package com.divine.gita.ui.mood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.divine.gita.R
import com.divine.gita.databinding.FragmentListBinding
import com.divine.gita.databinding.ItemMoodBinding
import com.divine.gita.databinding.ItemMoodHeaderBinding
import com.divine.gita.domain.model.Mood
import com.divine.gita.domain.model.Sloka
import com.divine.gita.domain.model.TranslationLanguage
import com.divine.gita.domain.repository.GitaRepository
import com.divine.gita.domain.repository.MoodRepository
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

/** Visual style per mood key (keys match assets/moods.json). */
private data class MoodStyle(@ColorRes val tint: Int, @ColorRes val accent: Int, val symbol: String)

private fun styleFor(key: String) = when (key) {
    "anxious" -> MoodStyle(R.color.mood_anxious_tint, R.color.mood_anxious, "☁")
    "confused" -> MoodStyle(R.color.mood_confused_tint, R.color.mood_confused, "?")
    "sad" -> MoodStyle(R.color.mood_sad_tint, R.color.mood_sad, "☂")
    "angry" -> MoodStyle(R.color.mood_angry_tint, R.color.mood_angry, "🔥")
    "stressed" -> MoodStyle(R.color.mood_stressed_tint, R.color.mood_stressed, "⚡")
    "unmotivated" -> MoodStyle(R.color.mood_unmotivated_tint, R.color.mood_unmotivated, "⏳")
    "purpose" -> MoodStyle(R.color.mood_purpose_tint, R.color.mood_purpose, "✦")
    "grateful" -> MoodStyle(R.color.mood_grateful_tint, R.color.mood_grateful, "🙏")
    else -> MoodStyle(R.color.card_soft, R.color.saffron_deep, "ॐ")
}

// ---------------- Mood grid ----------------

class MoodsViewModel(repository: MoodRepository) : ViewModel() {
    val moods: StateFlow<List<Mood>?> = repository.observeMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class MoodFragment : Fragment(R.layout.fragment_list) {
    private val viewModel by appViewModels { c, _ -> MoodsViewModel(c.moodRepository) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentListBinding.bind(view)
        setupToolbar(b.toolbar, "Gita for Your Mood", "Manah Prasada", showBack = false)
        val adapter = MoodAdapter { navigateTo(R.id.moodVersesFragment, bundleOf("moodKey" to it.key)) }
        b.recycler.animateItemsOnFirstLoad()
        b.recycler.layoutManager = GridLayoutManager(requireContext(), 2).apply {
            // Header spans both columns
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) = if (position == 0) 2 else 1
            }
        }
        b.recycler.adapter = adapter
        collectWhileStarted(viewModel.moods) { list ->
            b.progress.isVisible = list == null
            adapter.submit(list.orEmpty())
        }
    }
}

private class MoodAdapter(private val onClick: (Mood) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class HeaderVH(b: ItemMoodHeaderBinding) : RecyclerView.ViewHolder(b.root)
    class MoodVH(val b: ItemMoodBinding) : RecyclerView.ViewHolder(b.root)

    private var moods: List<Mood> = emptyList()

    fun submit(list: List<Mood>) {
        if (list == moods) return
        moods = list
        @Suppress("NotifyDataSetChanged") notifyDataSetChanged()
    }

    override fun getItemCount() = moods.size + 1
    override fun getItemViewType(position: Int) = if (position == 0) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) HeaderVH(ItemMoodHeaderBinding.inflate(inflater, parent, false))
        else MoodVH(ItemMoodBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is MoodVH) return
        val mood = moods[position - 1]
        val style = styleFor(mood.key)
        val ctx = holder.itemView.context
        with(holder.b) {
            tint.setBackgroundColor(ContextCompat.getColor(ctx, style.tint))
            symbol.text = style.symbol
            symbol.setTextColor(ContextCompat.getColor(ctx, style.accent))
            title.text = mood.title
            subtitle.text = mood.subtitle
            count.text = ctx.getString(R.string.holy_shlokas, mood.verseCount)
            count.setTextColor(ContextCompat.getColor(ctx, style.accent))
            root.setOnClickListener { onClick(mood) }
        }
    }
}

// ---------------- Verses for one mood ----------------

data class MoodVersesState(
    val mood: Mood? = null,
    val slokas: List<Sloka> = emptyList(),
    val language: TranslationLanguage = TranslationLanguage.HINDI,
    val loading: Boolean = true,
)

class MoodVersesViewModel(
    handle: SavedStateHandle,
    moodRepository: MoodRepository,
    private val gitaRepository: GitaRepository,
    preferences: PreferencesRepository,
) : ViewModel() {
    private val key: String = checkNotNull(handle["moodKey"])

    val state: StateFlow<MoodVersesState> = combine(
        moodRepository.observeMood(key),
        moodRepository.observeSlokasForMood(key),
        preferences.translationLanguage,
    ) { mood, slokas, lang -> MoodVersesState(mood, slokas, lang, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoodVersesState())

    fun toggleFavorite(sloka: Sloka) {
        viewModelScope.launch { gitaRepository.setSlokaFavorite(sloka.id, !sloka.isFavorite) }
    }
}

class MoodVersesFragment : Fragment(R.layout.fragment_list) {
    private val viewModel by appViewModels { c, h ->
        MoodVersesViewModel(h, c.moodRepository, c.gitaRepository, c.preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentListBinding.bind(view)
        setupToolbar(b.toolbar, "Mood", "Krishna's guidance")
        val adapter = SlokaAdapter(
            onClick = { navigateTo(R.id.slokaDetailFragment, bundleOf("slokaId" to it.id)) },
            onToggleFavorite = viewModel::toggleFavorite,
        )
        b.recycler.animateItemsOnFirstLoad()
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter
        collectWhileStarted(viewModel.state) { s ->
            b.progress.isVisible = s.loading
            s.mood?.let { b.toolbar.title = "${it.title} ${it.subtitle}" }
            adapter.language = s.language
            adapter.submitList(s.slokas)
        }
    }
}
