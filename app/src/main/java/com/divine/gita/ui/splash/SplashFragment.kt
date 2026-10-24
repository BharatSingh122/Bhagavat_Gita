package com.divine.gita.ui.splash

import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.divine.gita.R
import com.divine.gita.databinding.FragmentSplashBinding
import com.divine.gita.domain.repository.DataSeeder
import com.divine.gita.domain.repository.PreferencesRepository
import com.divine.gita.ui.common.appViewModels
import com.divine.gita.ui.common.collectWhileStarted
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashState {
    data object Preparing : SplashState
    data class Ready(val onboardingCompleted: Boolean) : SplashState
    data class Failed(val message: String) : SplashState
}

class SplashViewModel(
    private val seeder: DataSeeder,
    private val preferences: PreferencesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<SplashState>(SplashState.Preparing)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init { prepare() }

    fun prepare() {
        _state.value = SplashState.Preparing
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            runCatching { seeder.seedIfNeeded() }
                .onSuccess {
                    // Keep the divine intro visible briefly even when seeding is instant.
                    val elapsed = System.currentTimeMillis() - started
                    if (elapsed < MIN_SPLASH_MS) delay(MIN_SPLASH_MS - elapsed)
                    _state.value = SplashState.Ready(preferences.onboardingCompleted.first())
                }
                .onFailure { _state.value = SplashState.Failed(it.message ?: "Could not prepare the sacred library") }
        }
    }

    private companion object { const val MIN_SPLASH_MS = 1400L }
}

/** Divine intro shown while Room is seeded from assets on first launch. */
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val viewModel by appViewModels { c, _ -> SplashViewModel(c.seeder, c.preferences) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentSplashBinding.bind(view)
        b.om.scaleX = 0.8f; b.om.scaleY = 0.8f; b.om.alpha = 0f
        b.om.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(900).setInterpolator(DecelerateInterpolator()).start()
        b.retryButton.setOnClickListener { viewModel.prepare() }

        collectWhileStarted(viewModel.state) { state ->
            b.progress.isVisible = state is SplashState.Preparing
            b.errorText.isVisible = state is SplashState.Failed
            b.retryButton.isVisible = state is SplashState.Failed
            when (state) {
                is SplashState.Failed -> b.errorText.text = state.message
                is SplashState.Ready -> {
                    val nav = findNavController()
                    if (nav.currentDestination?.id == R.id.splashFragment) {
                        nav.navigate(if (state.onboardingCompleted) R.id.action_splash_to_home else R.id.action_splash_to_onboarding)
                    }
                }
                SplashState.Preparing -> Unit
            }
        }
    }
}
