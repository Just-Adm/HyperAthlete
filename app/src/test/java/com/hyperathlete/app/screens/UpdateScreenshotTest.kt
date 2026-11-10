package com.hyperathlete.app.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.hyperathlete.app.core.locale.ProvideAppLanguage
import com.hyperathlete.app.core.update.ReleaseNotes
import com.hyperathlete.app.core.update.UpdateManifest
import com.hyperathlete.app.core.update.UpdateResult
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.ThemeMode
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.settings.UpdateDeps
import com.hyperathlete.app.ui.settings.UpdateRows
import com.hyperathlete.app.ui.settings.UpdateUiState
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperTheme
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.glassBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings → updates: the rows in each state, and the glass update dialog (light and dark, Arabic and English). */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h802dp-xhdpi", application = Application::class)
class UpdateScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private val manifest = UpdateManifest(
        versionCode = 17, versionName = "3.4.0",
        apkUrl = "https://github.com/OWNER/HyperAthlete/releases/download/v3.4.0/HyperAthlete-3.4.0.apk",
        sha256 = "0".repeat(64),
        releaseNotes = ReleaseNotes(
            en = "- Body map: tap any muscle\n- Faster workout logging\n- Fixes",
            ar = "- خريطة الجسم: اضغط على أي عضلة\n- تسجيل الجولات أسرع\n- تصليحات",
        ),
    )

    private fun shot(name: String, language: AppLanguage = AppLanguage.MIXED, theme: ThemeMode = ThemeMode.LIGHT, state: UpdateUiState) {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ProvideAppLanguage(language) {
                    HyperTheme(themeMode = theme) { Screen(state) }
                }
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage("screenshots/$name.png")
    }

    @Composable
    private fun Screen(state: UpdateUiState) {
        Box(Modifier.fillMaxSize().glassBackdrop(Hyper.glass)) {
            Column(Modifier.statusBarsPadding().padding(20.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.hyperathlete.app.R.string.settings_about), style = Hyper.type.h1, color = Hyper.colors.text)
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
                HyperCard(padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { UpdateRows(UpdateDeps(state)) }
            }
        }
    }

    @Test fun settings_updates_idle() = shot("settings_updates", state = UpdateUiState())
    @Test fun settings_updates_offline_dark() = shot("settings_updates_offline_dark", AppLanguage.ENGLISH, ThemeMode.DARK, UpdateUiState(result = UpdateResult.Unavailable))
    @Test fun update_dialog() = shot("update_dialog", state = UpdateUiState(result = UpdateResult.Available(manifest), dialog = manifest))
    @Test fun update_dialog_dark() = shot("update_dialog_dark", AppLanguage.ENGLISH, ThemeMode.DARK, UpdateUiState(result = UpdateResult.Available(manifest), dialog = manifest))
}
