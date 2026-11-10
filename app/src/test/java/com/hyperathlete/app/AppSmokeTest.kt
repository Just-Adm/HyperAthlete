package com.hyperathlete.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Boots the real app (Hilt graph, Room, DataStore, navigation, theme, language) on a fresh install.
 * Catches wiring mistakes that compile fine but crash on launch. One flow, because DataStore is a
 * per-process singleton and state would leak between separate tests.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h802dp-xhdpi")
class AppSmokeTest {

    @get:Rule val compose = createAndroidComposeRule<HomeActivity>()

    private fun waitFor(text: String) =
        compose.waitUntil(15_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    @Test fun `fresh install - onboarding, instant language switch, next step`() {
        // Opens in the default mixed mode (Arabic UI).
        waitFor("يلا نبلّش")
        compose.onNodeWithText("Hyper Athlete").assertIsDisplayed()

        // Language flips instantly, same activity instance (no restart).
        val activity = compose.activity
        compose.onNodeWithText("English").performClick()
        waitFor("Let's go")
        compose.onNodeWithText("Step 1 of 6").assertIsDisplayed()
        assertSame(activity, compose.activity)
        compose.onNodeWithText("عربي").performClick()
        waitFor("يلا نبلّش")

        // Moves forward and the step indicator follows.
        compose.onNodeWithText("يلا نبلّش").performClick()
        waitFor("عرّفني عليك")
        compose.onNodeWithText("الخطوة 2 من 6").assertIsDisplayed()
    }
}
