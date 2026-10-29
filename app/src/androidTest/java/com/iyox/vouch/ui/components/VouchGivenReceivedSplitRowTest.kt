package com.iyox.vouch.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.ui.theme.IyoxVouchTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VouchGivenReceivedSplitRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testGivenAndReceivedAmountsDisplayed() {
        CurrencyManager.setCurrency("EUR")

        composeTestRule.setContent {
            IyoxVouchTheme {
                VouchGivenReceivedSplitRow(
                    amountGiven = 12500L,
                    amountReceived = 4500L
                )
            }
        }

        val expectedGiven = CurrencyManager.formatAmountWithSymbol(12500L)
        val expectedReceived = CurrencyManager.formatAmountWithSymbol(4500L)
        val givenLabel = context.getString(R.string.label_given)
        val receivedLabel = context.getString(R.string.label_received)

        composeTestRule.onNodeWithText(expectedGiven).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedReceived).assertIsDisplayed()
        composeTestRule.onNodeWithText(givenLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(receivedLabel).assertIsDisplayed()
    }
}
