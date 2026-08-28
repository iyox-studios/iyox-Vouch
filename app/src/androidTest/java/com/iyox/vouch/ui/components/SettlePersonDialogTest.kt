package com.iyox.vouch.ui.components

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
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.feature.person.SettlePersonDialog
import com.iyox.vouch.ui.theme.IyoxVouchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettlePersonDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testSettlePersonDialog_confirmFullySettle() {
        CurrencyManager.setCurrency("EUR")

        val personData = PersonTotal(
            person = Person(id = 1, name = "Alice"),
            amount = 2500L,
            vouchCount = 2
        )

        var confirmedWithFullySettle: Boolean? = null
        var dismissed = false

        composeTestRule.setContent {
            IyoxVouchTheme {
                SettlePersonDialog(
                    personName = "Alice",
                    personData = personData,
                    onDismiss = { dismissed = true },
                    onConfirm = { confirmedWithFullySettle = it }
                )
            }
        }

        val settleAction = context.getString(R.string.action_settle)

        composeTestRule.onNodeWithText("Alice", substring = true).assertIsDisplayed()

        // Click Settle button (use exact match with hasClickAction to avoid matching dialog title)
        composeTestRule.onNode(hasText(settleAction, substring = false) and hasClickAction()).performClick()
        assertEquals(true, confirmedWithFullySettle)
    }

    @Test
    fun testSettlePersonDialog_uncheckDifferencePaidAndSettle() {
        CurrencyManager.setCurrency("EUR")

        val personData = PersonTotal(
            person = Person(id = 1, name = "Bob"),
            amount = -1500L,
            vouchCount = 1
        )

        var confirmedWithFullySettle: Boolean? = null

        composeTestRule.setContent {
            IyoxVouchTheme {
                SettlePersonDialog(
                    personName = "Bob",
                    personData = personData,
                    onDismiss = {},
                    onConfirm = { confirmedWithFullySettle = it }
                )
            }
        }

        val diffLabel = context.getString(R.string.label_difference_paid)
        val settleAction = context.getString(R.string.action_settle)

        // Toggle checkbox by clicking row text
        composeTestRule.onNodeWithText(diffLabel, substring = true, ignoreCase = true).performClick()

        // Click Settle button (use exact match with hasClickAction)
        composeTestRule.onNode(hasText(settleAction, substring = false) and hasClickAction()).performClick()
        assertEquals(false, confirmedWithFullySettle)
    }

    @Test
    fun testSettlePersonDialog_cancel() {
        var dismissed = false

        composeTestRule.setContent {
            IyoxVouchTheme {
                SettlePersonDialog(
                    personName = "Charlie",
                    personData = null,
                    onDismiss = { dismissed = true },
                    onConfirm = {}
                )
            }
        }

        val cancelAction = context.getString(R.string.action_cancel)
        composeTestRule.onNode(hasText(cancelAction, substring = false) and hasClickAction()).performClick()
        assertEquals(true, dismissed)
    }
}
