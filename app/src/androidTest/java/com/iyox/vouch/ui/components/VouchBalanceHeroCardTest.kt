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
class VouchBalanceHeroCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testPositiveBalance_displaysGiven() {
        CurrencyManager.setCurrency("EUR")

        composeTestRule.setContent {
            IyoxVouchTheme {
                VouchBalanceHeroCard(
                    amountGiven = 5000L,
                    amountOwed = 2000L,
                    total = 3000L
                )
            }
        }

        val expectedTotal = CurrencyManager.formatAmountWithSymbol(3000L)
        val expectedGiven = CurrencyManager.formatAmountWithSymbol(5000L)
        val expectedOwed = CurrencyManager.formatAmountWithSymbol(2000L)
        val heroLabel = context.getString(R.string.label_total_balance)

        composeTestRule.onNodeWithText(expectedTotal).assertIsDisplayed()
        composeTestRule.onNodeWithText(heroLabel, substring = true, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedGiven, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedOwed, substring = true).assertIsDisplayed()
    }

    @Test
    fun testNegativeBalance_displaysReceived() {
        CurrencyManager.setCurrency("EUR")

        composeTestRule.setContent {
            IyoxVouchTheme {
                VouchBalanceHeroCard(
                    amountGiven = 1000L,
                    amountOwed = 4000L,
                    total = -3000L
                )
            }
        }

        val expectedTotal = CurrencyManager.formatAmountWithSymbol(-3000L)
        composeTestRule.onNodeWithText(expectedTotal).assertIsDisplayed()
    }

    @Test
    fun testZeroBalance_displaysSettled() {
        CurrencyManager.setCurrency("EUR")

        composeTestRule.setContent {
            IyoxVouchTheme {
                VouchBalanceHeroCard(
                    amountGiven = 0L,
                    amountOwed = 0L,
                    total = 0L
                )
            }
        }

        val expectedTotal = CurrencyManager.formatAmountWithSymbol(0L)
        composeTestRule.onNodeWithText(expectedTotal).assertIsDisplayed()
    }

    @Test
    fun testVouchCountBadge_displayedWhenProvided() {
        CurrencyManager.setCurrency("EUR")

        composeTestRule.setContent {
            IyoxVouchTheme {
                VouchBalanceHeroCard(
                    amountGiven = 5000L,
                    amountOwed = 2000L,
                    total = 3000L,
                    vouchCount = 5
                )
            }
        }

        val expectedCount = context.resources.getQuantityString(R.plurals.vouch_count, 5, 5)
        composeTestRule.onNodeWithText(expectedCount).assertIsDisplayed()
    }
}
