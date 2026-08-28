package com.iyox.vouch.feature.onboarding

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.R
import com.iyox.vouch.ui.theme.IyoxVouchTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testOnboardingScreen_welcomeSlideAndNext() {
        var finished = false
        var selectedCurrency: String? = null

        composeTestRule.setContent {
            IyoxVouchTheme {
                OnboardingScreen(
                    onFinish = { finished = true },
                    currentCurrencyCode = "EUR",
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }

        val welcomeBadge = context.getString(R.string.onboarding_welcome_badge)
        val nextAction = context.getString(R.string.onboarding_next)

        // Welcome slide
        composeTestRule.onNodeWithText(welcomeBadge, substring = true, ignoreCase = true).assertIsDisplayed()

        // Next button click (use exact match with click action to avoid matching hint text)
        composeTestRule.onNode(hasText(nextAction, substring = false) and hasClickAction()).performClick()
    }

    @Test
    fun testOnboardingScreen_skipToCurrencySlideAndFinish() {
        var finished = false
        var selectedCurrency: String? = null

        composeTestRule.setContent {
            IyoxVouchTheme {
                OnboardingScreen(
                    onFinish = { finished = true },
                    currentCurrencyCode = "EUR",
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }

        val skipAction = context.getString(R.string.onboarding_skip)
        val getStartedAction = context.getString(R.string.onboarding_get_started)

        // Skip to end
        composeTestRule.onNode(hasText(skipAction, substring = false) and hasClickAction()).performClick()

        // Wait for page transition
        composeTestRule.waitForIdle()

        // Currency selection slide
        composeTestRule.onNodeWithText("USD", substring = true).performClick()
        assertEquals("USD", selectedCurrency)

        // Click Get Started / Go
        composeTestRule.onNode(hasText(getStartedAction, substring = false) and hasClickAction()).performClick()
        assertTrue(finished)
    }
}
