package com.hyperathlete.app

import android.content.Context
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.domain.model.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real app, onboarding already done: changing settings from the Account tab must show and stick. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h802dp-xhdpi")
class SettingsFlowTest {

    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun waitFor(text: String) =
        compose.waitUntil(15_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    @Test fun `language chosen in Settings applies everywhere and persists`() {
        val repo = SettingsRepository(context)
        runBlocking { repo.setOnboardingDone(true); repo.setLanguage(AppLanguage.MIXED) }
        ActivityScenario.launch(HomeActivity::class.java).use {
            waitFor("الحساب")
            compose.onNodeWithText("الحساب").performClick()
            waitFor("اللغة")
            compose.onNodeWithText("اللغة").performScrollTo().performClick()
            waitFor("English بالكامل")
            compose.onNodeWithText("English بالكامل").performClick()
            // The screen itself must switch language and keep the new selection.
            waitFor("English only")
            waitFor("Language")
            assertEquals(AppLanguage.ENGLISH, runBlocking { repo.current().language })
            compose.onNodeWithContentDescription("Back").performClick()
            waitFor("Account")
            waitFor("Settings")
        }
    }
}
