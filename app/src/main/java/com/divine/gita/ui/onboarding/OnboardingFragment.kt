package com.divine.gita.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.divine.gita.R
import com.divine.gita.databinding.FragmentOnboardingBinding
import com.divine.gita.databinding.ItemOnboardingPageBinding
import com.divine.gita.domain.repository.PreferencesRepository
import com.divine.gita.ui.common.appViewModels
import kotlinx.coroutines.launch

class OnboardingViewModel(private val preferences: PreferencesRepository) : ViewModel() {
    suspend fun complete() = preferences.setOnboardingCompleted()
}

private data class Page(val overline: String, val title: String, val body: String, val highlights: String)

private val pages = listOf(
    Page(
        "ॐ श्रीमद्भगवद्गीता", "Srimad Bhagavad Gita",
        "Timeless Vedic wisdom for inner calm, clear decisions and righteous living through life's battles.",
        "📖  18 chapters • 700 shlokas with Hindi & English meaning",
    ),
    Page(
        "एक श्लोक प्रतिदिन", "A Sloka Every Day",
        "Each morning a new verse awaits you on the home screen, so your sadhana grows one shloka at a time.",
        "🧘  Gita for your mood: guidance for anxiety, anger and sorrow\n💬  Anmol Vichar: 150 thoughts on karma, dharma and bhakti",
    ),
    Page(
        "भक्ति • श्रद्धा", "Chalisa Sangrah",
        "Read the chalisas of 15 deities, from Shri Ganesh to Suryadev, and keep your favourite verses close.",
        "🛕  Ganesh, Hanuman, Shiv, Lakshmi, Krishna, Ram and more",
    ),
)

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private val viewModel by appViewModels { c, _ -> OnboardingViewModel(c.preferences) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentOnboardingBinding.bind(view)
        b.pager.adapter = PageAdapter()

        // Page indicator dots
        val dotMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
        val dots = pages.indices.map {
            View(requireContext()).apply {
                setBackgroundResource(R.drawable.indicator_dot)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { marginStart = dotMargin; marginEnd = dotMargin }
            }.also(b.indicators::addView)
        }

        fun finish() {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.complete()
                findNavController().navigate(R.id.action_onboarding_to_home)
            }
        }

        b.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.forEachIndexed { i, dot -> dot.isSelected = i == position }
                val last = position == pages.lastIndex
                b.nextButton.setText(if (last) R.string.start_journey else R.string.next)
                b.skipButton.isInvisible = last
            }
        })
        b.nextButton.setOnClickListener {
            if (b.pager.currentItem == pages.lastIndex) finish() else b.pager.currentItem += 1
        }
        b.skipButton.setOnClickListener { finish() }
    }

    private class PageAdapter : RecyclerView.Adapter<PageAdapter.VH>() {
        class VH(val b: ItemOnboardingPageBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = pages.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = pages[position]
            holder.b.overline.text = p.overline
            holder.b.title.text = p.title
            holder.b.body.text = p.body
            holder.b.highlights.text = p.highlights
        }
    }
}
