package com.hyperathlete.app

import android.graphics.Color
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyperathlete.app.ui.AppViewModel
import com.hyperathlete.app.ui.HyperRoot
import com.hyperathlete.app.ui.theme.isDarkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the system splash until settings are read, so the first frame is already themed and localised.
        splash.setKeepOnScreenCondition { appViewModel.state.value == null }
        if (savedInstanceState == null) appViewModel.open(intent?.getStringExtra(com.hyperathlete.app.core.workout.WorkoutNotifier.EXTRA_OPEN))
        // Display info is only reliable once the window is attached.
        window.decorView.post { runCatching { preferHighestRefreshRate() } }

        setContent {
            val state by appViewModel.state.collectAsStateWithLifecycle()
            val s = state ?: return@setContent
            val dark = isDarkTheme(s.settings.themeMode)
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            HyperRoot(s, appViewModel)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        appViewModel.open(intent.getStringExtra(com.hyperathlete.app.core.workout.WorkoutNotifier.EXTRA_OPEN))
    }

    /** Ask for the panel's top refresh rate (120Hz on the X7e) at the current resolution. */
    private fun preferHighestRefreshRate() {
        val display: Display = window.decorView.display ?: return
        val current = display.mode
        val best = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        if (best.modeId != current.modeId || window.attributes.preferredDisplayModeId != best.modeId) {
            window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
        }
    }
}
