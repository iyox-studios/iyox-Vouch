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
import com.iyox.vouch.ui.components.vouches.GroupVouchFullItem
import com.iyox.vouch.ui.components.vouches.GroupVouchSingleItem
import com.iyox.vouch.ui.theme.IyoxVouchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupVouchItemsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testGroupVouchFullItem_rendersAndHandlesClick() {
        CurrencyManager.setCurrency("EUR")

        val vouch = VouchWithPerson(
            vouch = Vouch(
                id = 99,
                amount = 6000L,
                description = "Airbnb weekend",
                given = true,
                timestamp = 1700000000000L
            ),
            persons = listOf(
                PersonWithAmount(Person(name = "Alice"), 3000L, settled = true),
                PersonWithAmount(Person(name = "Bob"), 3000L, settled = false)
            )
        )

        var edited = false

        composeTestRule.setContent {
            IyoxVouchTheme {
                GroupVouchFullItem(
                    vouch = vouch,
                    canSettle = true,
                    onSettle = {},
                    onDelete = {},
                    onEdit = { edited = true }
                )
            }
        }

        val expectedAmount = CurrencyManager.formatAmountWithSymbol(6000L)
        composeTestRule.onNodeWithText(expectedAmount).assertIsDisplayed()
        composeTestRule.onNodeWithText("Airbnb weekend").assertIsDisplayed()

        composeTestRule.onNodeWithText(expectedAmount).performClick()
        assertEquals(true, edited)
    }

    @Test
    fun testGroupVouchSingleItem_rendersAndHandlesClick() {
        CurrencyManager.setCurrency("EUR")

        val alice = Person(id = 1, name = "Alice")
        val vouch = VouchWithPerson(
            vouch = Vouch(
                id = 100,
                amount = 6000L,
                description = "Airbnb weekend",
                given = true,
                timestamp = 1700000000000L
            ),
            persons = listOf(
                PersonWithAmount(alice, 3000L, settled = false),
                PersonWithAmount(Person(id = 2, name = "Bob"), 3000L, settled = false)
            )
        )

        var edited = false

        composeTestRule.setContent {
            IyoxVouchTheme {
                GroupVouchSingleItem(
                    vouch = vouch,
                    person = alice,
                    canSettle = true,
                    onSettle = {},
                    onDelete = {},
                    onEdit = { edited = true }
                )
            }
        }

        val expectedPersonAmount = CurrencyManager.formatAmountWithSymbol(3000L)
        val expectedTotalAmount = CurrencyManager.formatAmountWithSymbol(6000L)

        composeTestRule.onNodeWithText(expectedPersonAmount).assertIsDisplayed()
        composeTestRule.onNodeWithText("/ $expectedTotalAmount").assertIsDisplayed()
        composeTestRule.onNodeWithText("Airbnb weekend").assertIsDisplayed()

        composeTestRule.onNodeWithText(expectedPersonAmount).performClick()
        assertEquals(true, edited)
    }

    @Test
    fun testGroupVouchFullItem_clickBadge_opensParticipantsPopup_andSelectsPerson() {
        CurrencyManager.setCurrency("EUR")

        val vouch = VouchWithPerson(
            vouch = Vouch(
                id = 101,
                amount = 9000L,
                description = "Trip",
                given = true,
                timestamp = 1700000000000L
            ),
            persons = listOf(
                PersonWithAmount(Person(id = 1, name = "Alice"), 3000L, settled = true),
                PersonWithAmount(Person(id = 2, name = "Bob"), 3000L, settled = false),
                PersonWithAmount(Person(id = 3, name = "Charlie"), 3000L, settled = false)
            )
        )

        var clickedPerson: String? = null

        composeTestRule.setContent {
            IyoxVouchTheme {
                GroupVouchFullItem(
                    vouch = vouch,
                    canSettle = true,
                    onSettle = {},
                    onDelete = {},
                    onEdit = {},
                    onPersonClick = { clickedPerson = it }
                )
            }
        }

        // Click the "3 persons" badge
        composeTestRule.onNodeWithText("3 persons").performClick()

        // Popup should be visible showing participants
        composeTestRule.onNodeWithText("Participants").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()

        // Click Bob
        composeTestRule.onNodeWithText("Bob").performClick()
        assertEquals("Bob", clickedPerson)
    }

    @Test
    fun testGroupVouchSingleItem_clickBadge_opensParticipantsPopup_andSelectsPerson() {
        CurrencyManager.setCurrency("EUR")

        val alice = Person(id = 1, name = "Alice")
        val vouch = VouchWithPerson(
            vouch = Vouch(
                id = 102,
                amount = 6000L,
                description = "Dinner",
                given = true,
                timestamp = 1700000000000L
            ),
            persons = listOf(
                PersonWithAmount(alice, 3000L, settled = false),
                PersonWithAmount(Person(id = 2, name = "Bob"), 3000L, settled = false)
            )
        )

        var clickedPerson: String? = null

        composeTestRule.setContent {
            IyoxVouchTheme {
                GroupVouchSingleItem(
                    vouch = vouch,
                    person = alice,
                    canSettle = true,
                    onSettle = {},
                    onDelete = {},
                    onEdit = {},
                    onPersonClick = { clickedPerson = it }
                )
            }
        }

        // Click the "2 persons" badge
        composeTestRule.onNodeWithText("2 persons").performClick()

        // Popup should be visible showing participants
        composeTestRule.onNodeWithText("Participants").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()

        // Click Alice
        composeTestRule.onNodeWithText("Alice").performClick()
        assertEquals("Alice", clickedPerson)
    }
}
