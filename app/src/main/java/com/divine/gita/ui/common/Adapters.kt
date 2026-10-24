package com.divine.gita.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.divine.gita.R
import com.divine.gita.databinding.ItemQuoteBinding
import com.divine.gita.databinding.ItemSlokaBinding
import com.divine.gita.domain.model.Quote
import com.divine.gita.domain.model.Sloka
import com.divine.gita.domain.model.TranslationLanguage

/** Verse cards used by chapter detail, mood verses and favourites. */
class SlokaAdapter(
    private val onClick: (Sloka) -> Unit,
    private val onToggleFavorite: (Sloka) -> Unit,
) : ListAdapter<Sloka, SlokaAdapter.VH>(DIFF) {

    var language: TranslationLanguage = TranslationLanguage.HINDI
        set(value) {
            if (field != value) { field = value; notifyItemRangeChanged(0, itemCount) }
        }

    inner class VH(val b: ItemSlokaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemSlokaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val sloka = getItem(position)
        with(holder.b) {
            reference.text = root.context.getString(R.string.chapter_verse_short, sloka.chapter, sloka.verse)
            sanskrit.text = sloka.sanskrit
            translation.text = sloka.previewFor(language)
            favorite.showFavorite(sloka.isFavorite)
            favorite.setOnClickListener { onToggleFavorite(sloka) }
            root.setOnClickListener { onClick(sloka) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Sloka>() {
            override fun areItemsTheSame(a: Sloka, b: Sloka) = a.id == b.id
            override fun areContentsTheSame(a: Sloka, b: Sloka) = a == b
        }
    }
}

/** Quote cards used by Anmol Vichar and favourites. */
class QuoteAdapter(
    private val showCategory: Boolean,
    private val onToggleFavorite: (Quote) -> Unit,
    private val onCopy: (Quote) -> Unit,
    private val onShare: (Quote) -> Unit,
) : ListAdapter<Quote, QuoteAdapter.VH>(DIFF) {

    inner class VH(val b: ItemQuoteBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val quote = getItem(position)
        with(holder.b) {
            category.visibility = if (showCategory) android.view.View.VISIBLE else android.view.View.GONE
            category.text = "${quote.category.hindi} (${quote.category.english})"
            text.text = quote.text
            favorite.showFavorite(quote.isFavorite)
            favorite.setOnClickListener { onToggleFavorite(quote) }
            copy.setOnClickListener { onCopy(quote) }
            share.setOnClickListener { onShare(quote) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Quote>() {
            override fun areItemsTheSame(a: Quote, b: Quote) = a.id == b.id
            override fun areContentsTheSame(a: Quote, b: Quote) = a == b
        }
    }
}
