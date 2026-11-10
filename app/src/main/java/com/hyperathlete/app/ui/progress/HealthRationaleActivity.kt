package com.hyperathlete.app.ui.progress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.ProvideAppLanguage
import com.hyperathlete.app.ui.AppViewModel
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.settings.SubScreen
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperTheme
import dagger.hilt.android.AndroidEntryPoint

/** What Hyper Athlete does with Health Connect data; Health Connect opens this from its permission screens. */
@AndroidEntryPoint
class HealthRationaleActivity : ComponentActivity() {
    private val app: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by app.state.collectAsStateWithLifecycle()
            val s = state ?: return@setContent
            ProvideAppLanguage(s.settings.language) {
                HyperTheme(themeMode = s.settings.themeMode, accent = Color(s.settings.accentArgb.toInt()), secondary = Color(s.settings.secondaryArgb.toInt())) {
                    SubScreen(stringResource(R.string.health_title), onBack = { finish() }, bottom = {
                        PrimaryButton(stringResource(R.string.action_done), { finish() })
                    }) {
                        Text(stringResource(R.string.health_rationale), style = Hyper.type.body, color = Hyper.colors.text)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.health_rationale_private), style = Hyper.type.small, color = Hyper.colors.textSecondary)
                    }
                }
            }
        }
    }
}
