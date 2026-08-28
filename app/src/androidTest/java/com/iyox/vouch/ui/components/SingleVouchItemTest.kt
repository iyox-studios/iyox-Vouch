package com.iyox.vouch.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.ui.components.vouches.SingleVouchItem
import com.iyox.vouch.ui.theme.IyoxVouchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.sql.Timestamp

@RunWith(AndroidJUnit4::class)
class SingleVouchItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSingleVouchItem_displaysInfoAndHandlesClicks() {
        CurrencyManager.setCurrency("EUR")

        val vouch = VouchWithPerson(
            vouch = Vouch(
                id = 42,
                amount = 2550L,
                description = "Coffee & Bakery",
                given = true,
                timestamp = Timestamp(1700000000000L)
            ),
            persons = listOf(PersonWithAmount(Person(id = 1, name = "Alice"), 2550L))
        )

        var edited = false
        var clickedPerson: String? = null

        composeTestRule.setContent {
            IyoxVouchTheme {
                SingleVouchItem(
                    vouch = vouch,
                    canSettle = true,
                    onSettle = {},
                    onDelete = {},
                    onEdit = { edited = true },
                    onPersonClick = { clickedPerson = it }
                )
            }
        }

        val expectedAmount = CurrencyManager.formatAmountWithSymbol(2550L)
        composeTestRule.onNodeWithText(expectedAmount).assertIsDisplayed()
        composeTestRule.onNodeWithText("Coffee & Bakery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()

        // Clicking person chip invokes onPersonClick
        composeTestRule.onNodeWithText("Alice").performClick()
        assertEquals("Alice", clickedPerson)

        // Clicking card invokes onEdit
        composeTestRule.onNodeWithText(expectedAmount).performClick()
        assertEquals(true, edited)
    }

    @Test
    fun testSingleVouchItem_noDescriptionPlaceholder() {
        val vouch = VouchWithPerson(
            vouch = Vouch(id = 1, amount = 1000L, description = ""),
            persons = listOf(PersonWithAmount(Person(name = "Bob"), 1000L))
        )

        composeTestRule.setContent {
            IyoxVouchTheme {
                SingleVouchItem(
                    vouch = vouch,
                    onSettle = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }
}
