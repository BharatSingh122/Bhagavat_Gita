package com.divine.gita.ui.settings

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divine.gita.BuildConfig
import com.divine.gita.R
import com.divine.gita.databinding.FragmentSettingsBinding
import com.divine.gita.databinding.ViewSettingsRowBinding
import com.divine.gita.domain.model.TranslationLanguage
import com.divine.gita.domain.repository.GitaRepository
import com.divine.gita.domain.repository.PreferencesRepository
import com.divine.gita.domain.repository.QuoteRepository
import com.divine.gita.ui.common.IntentUtils
import com.divine.gita.ui.common.appViewModels
import com.divine.gita.ui.common.collectWhileStarted
import com.divine.gita.ui.common.setupToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(val language: TranslationLanguage = TranslationLanguage.HINDI, val favoriteCount: Int = 0)

class SettingsViewModel(
    private val preferences: PreferencesRepository,
    gitaRepository: GitaRepository,
    quoteRepository: QuoteRepository,
) : ViewModel() {
    val state: StateFlow<SettingsState> = combine(
        preferences.translationLanguage,
        gitaRepository.observeFavoriteSlokas(),
        quoteRepository.observeFavoriteQuotes(),
    ) { lang, slokas, quotes -> SettingsState(lang, slokas.size + quotes.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setLanguage(language: TranslationLanguage) {
        viewModelScope.launch { preferences.setTranslationLanguage(language) }
    }
}

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val viewModel by appViewModels { c, _ ->
        SettingsViewModel(c.preferences, c.gitaRepository, c.quoteRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentSettingsBinding.bind(view)
        setupToolbar(b.toolbar, "Settings", "Sadhana & App")
        b.version.text = "Version ${BuildConfig.VERSION_NAME}"

        b.rowLanguage.setup(title = "Language", subtitle = "Translation shown with each shlok", iconText = "अ") {
            showLanguageDialog()
        }
        b.rowFavorites.setup(title = "Favourites", subtitle = "Your saved slokas and vichar", icon = R.drawable.ic_favorite) {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.favoritesFragment
        }
        b.rowRate.setup(title = "Rate Us", subtitle = "Rate us on Google Play Store", icon = R.drawable.ic_star) {
            IntentUtils.rateApp(requireContext())
        }
        b.rowShare.setup(title = "Share App", subtitle = "Spread divine knowledge with friends & family", icon = R.drawable.ic_share) {
            IntentUtils.shareApp(requireContext())
        }
        b.rowPrivacy.setup(title = "Privacy Policy", subtitle = "Read our privacy policy & data safety", icon = R.drawable.ic_shield) {
            IntentUtils.openUrl(requireContext(), getString(R.string.privacy_policy_url))
        }

        collectWhileStarted(viewModel.state) { s ->
            b.rowLanguage.badge.isVisible = true
            b.rowLanguage.badge.text = s.language.label
            b.rowFavorites.badge.isVisible = true
            b.rowFavorites.badge.text = "${s.favoriteCount} saved"
        }
    }

    private fun showLanguageDialog() {
        val options = TranslationLanguage.entries
        val current = options.indexOf(viewModel.state.value.language)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Translation language")
            .setSingleChoiceItems(options.map { it.label }.toTypedArray(), current) { dialog, which ->
                viewModel.setLanguage(options[which])
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun ViewSettingsRowBinding.setup(
        title: String,
        subtitle: String,
        @DrawableRes icon: Int? = null,
        iconText: String? = null,
        onClick: () -> Unit,
    ) {
        this.title.text = title
        this.subtitle.text = subtitle
        if (icon != null) this.icon.setImageResource(icon)
        this.icon.isVisible = icon != null
        this.iconText.isVisible = iconText != null
        this.iconText.text = iconText
        root.setOnClickListener { onClick() }
    }
}
