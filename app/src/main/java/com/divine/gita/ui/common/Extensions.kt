package com.divine.gita.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.RecyclerView
import com.divine.gita.GitaApplication
import com.divine.gita.R
import com.divine.gita.di.AppContainer
import com.divine.gita.domain.model.Sloka
import com.divine.gita.domain.model.TranslationLanguage
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** ViewModel factory that hands the AppContainer and the fragment's SavedStateHandle (nav args) to [create]. */
inline fun <reified VM : ViewModel> Fragment.appViewModels(
    crossinline create: (container: AppContainer, handle: SavedStateHandle) -> VM,
) = viewModels<VM>(factoryProducer = {
    viewModelFactory {
        initializer {
            val app = requireContext().applicationContext as GitaApplication
            create(app.container, createSavedStateHandle())
        }
    }
})

/** Collects [flow] while the fragment's view is at least STARTED. */
fun <T> Fragment.collectWhileStarted(flow: Flow<T>, block: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collectLatest(block) }
    }
}

fun Fragment.setupToolbar(toolbar: MaterialToolbar, title: CharSequence, subtitle: CharSequence? = null, showBack: Boolean = true) {
    toolbar.title = title
    toolbar.subtitle = subtitle
    if (showBack) {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationContentDescription(R.string.back)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    } else {
        toolbar.navigationIcon = null
    }
}

/** Slide + fade used for every forward screen change; back/up plays the reverse automatically. */
val screenNavOptions: NavOptions = navOptions {
    anim {
        enter = R.anim.nav_enter
        exit = R.anim.nav_exit
        popEnter = R.anim.nav_pop_enter
        popExit = R.anim.nav_pop_exit
    }
}

/** Navigates with [screenNavOptions]; ignores repeat taps while a transition is already running. */
fun Fragment.navigateTo(@IdRes destination: Int, args: Bundle? = null) {
    if (!isResumed) return
    findNavController().navigate(destination, args, screenNavOptions)
}

/** Lets the cards rise in once, the first time the list has items (data usually arrives after the first layout). */
fun RecyclerView.animateItemsOnFirstLoad() {
    addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
        override fun onChildViewAttachedToWindow(view: View) {
            removeOnChildAttachStateChangeListener(this)
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_list_enter)
            scheduleLayoutAnimation()
        }

        override fun onChildViewDetachedFromWindow(view: View) = Unit
    })
}

fun MaterialButton.showFavorite(isFavorite: Boolean) {
    setIconResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
    iconTint = ContextCompat.getColorStateList(context, if (isFavorite) R.color.favorite else R.color.text_secondary)
}

fun Context.copyToClipboard(text: String, anchor: View) {
    val cm = getSystemService(ClipboardManager::class.java)
    cm.setPrimaryClip(ClipData.newPlainText("Divine Gita", text))
    Snackbar.make(anchor, R.string.copied, Snackbar.LENGTH_SHORT).show()
}

/** Translation text honouring the user's language preference. */
fun Sloka.translationFor(language: TranslationLanguage): String = when (language) {
    TranslationLanguage.HINDI -> hindiTranslation
    TranslationLanguage.ENGLISH -> englishTranslation
    TranslationLanguage.BOTH -> "$hindiTranslation\n\n$englishTranslation"
}

/** Short preview translation for cards (one language only). */
fun Sloka.previewFor(language: TranslationLanguage): String =
    if (language == TranslationLanguage.ENGLISH) englishTranslation else hindiTranslation
