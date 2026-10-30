package com.divine.gita.ui

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.divine.gita.R
import com.divine.gita.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var systemBars = androidx.core.graphics.Insets.NONE

    /** Bottom-bar tabs; their menu ids equal the destination ids in nav_graph.xml. */
    private val topLevel = setOf(
        R.id.homeFragment, R.id.chaptersFragment, R.id.quotesFragment, R.id.moodFragment, R.id.favoritesFragment,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = (supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment).navController

        // Edge-to-edge: status bar padding on content, nav-bar padding on the bottom bar (or content when it's hidden).
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            applyInsets()
            insets
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            if (navController.currentDestination?.id != item.itemId) {
                navController.navigate(item.itemId, null, navOptions {
                    launchSingleTop = true
                    restoreState = true
                    // Material fade-through between tabs
                    anim {
                        enter = R.anim.fade_through_enter
                        exit = R.anim.fade_through_exit
                        popEnter = R.anim.fade_through_enter
                        popExit = R.anim.fade_through_exit
                    }
                    // Home is the root of the back stack once splash/onboarding are gone.
                    popUpTo(R.id.homeFragment) { saveState = true }
                })
            }
            true
        }
        binding.bottomNav.setOnItemReselectedListener { /* stay on the tab */ }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBar = destination.id in topLevel
            binding.bottomNav.visibility = if (showBar) View.VISIBLE else View.GONE
            if (showBar) binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
            applyInsets()
        }
    }

    private fun applyInsets() {
        val barVisible = binding.bottomNav.visibility == View.VISIBLE
        binding.navHost.updatePadding(top = systemBars.top, bottom = if (barVisible) 0 else systemBars.bottom)
        binding.bottomNav.updatePadding(bottom = systemBars.bottom)
    }
}
