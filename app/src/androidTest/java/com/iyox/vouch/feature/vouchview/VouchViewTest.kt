package com.iyox.vouch.feature.vouchview

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.utils.LocalToastManager
import com.iyox.vouch.core.utils.ToastManager
import com.iyox.vouch.ui.theme.IyoxVouchTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VouchViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: VouchRepository
    private lateinit var sharedActions: SharedVouchActions

    @Before
    fun setUp() {
        CurrencyManager.setCurrency("EUR")
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        database = AppDatabase.getDatabase(context)
        repository = VouchRepository(database, database.vouchDao(), database.personDao())
        sharedActions = SharedVouchActions(repository)
    }

    @Test
    fun testGroupVouchView_rendersCardShortcutBar_andScrollableList() {
        val alice = Person(id = 1, name = "Alice")
        val bob = Person(id = 2, name = "Bob")

        val vouch = VouchWithPerson(
            vouch = Vouch(
                id = 99,
                amount = 6000L,
                description = "Weekend cabin",
                given = true,
                timestamp = 1700000000000L
            ),
            persons = listOf(
                PersonWithAmount(alice, 3000L, settled = true),
                PersonWithAmount(bob, 3000L, settled = false)
            )
        )

        var editedVouchId: Long? = null
        var navigatedPersonName: String? = null
        var navigatedBack = false

        composeTestRule.setContent {
            IyoxVouchTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val toastManager = remember { ToastManager(snackbarHostState, scope) }

                CompositionLocalProvider(LocalToastManager provides toastManager) {
                    VouchViewContent(
                        uiState = VouchViewUiState(vouch = vouch, isLoading = false, isSettled = false),
                        sharedActions = sharedActions,
                        onNavigateBack = { navigatedBack = true },
                        onNavigateToEdit = { id -> editedVouchId = id },
                        onNavigateToPerson = { name -> navigatedPersonName = name }
                    )
                }
            }
        }

        val formattedTotal = CurrencyManager.formatAmountWithSymbol(6000L)
        composeTestRule.onNodeWithText(formattedTotal).assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekend cabin").assertIsDisplayed()

        // Shortcut bar buttons
        composeTestRule.onNodeWithContentDescription("Edit Vouch").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Delete Vouch").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Export Vouch image").assertIsDisplayed()

        // Verify "Beteiligte" text is NOT present
        composeTestRule.onNodeWithText("Beteiligte", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Participants", substring = true).assertDoesNotExist()

        // Associated vouches show Alice and Bob
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()

        // Click Back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(navigatedBack)

        // Tap Edit
        composeTestRule.onNodeWithContentDescription("Edit Vouch").performClick()
        assertEquals(99L, editedVouchId)

        // Tap Alice
        composeTestRule.onNodeWithText("Alice").performClick()
        assertEquals("Alice", navigatedPersonName)
    }

    @Test
    fun testShortcutBar_triggersCallbacks() {
        var settleClicked = false
        var exportClicked = false
        var editClicked = false
        var deleteClicked = false

        composeTestRule.setContent {
            IyoxVouchTheme {
                VouchViewShortcutBar(
                    isSettled = false,
                    onSettleClick = { settleClicked = true },
                    onExportClick = { exportClicked = true },
                    onEditClick = { editClicked = true },
                    onDeleteClick = { deleteClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Edit Vouch").performClick()
        assertTrue(editClicked)

        composeTestRule.onNodeWithContentDescription("Delete Vouch").performClick()
        assertTrue(deleteClicked)

        composeTestRule.onNodeWithContentDescription("Export Vouch image").performClick()
        assertTrue(exportClicked)
    }

    @Test
    fun testDelete_showsConfirmationPopup() {
        val alice = Person(id = 1, name = "Alice")
        val vouch = VouchWithPerson(
            vouch = Vouch(id = 99, amount = 3000L, description = "Test Vouch", timestamp = 1700000000000L),
            persons = listOf(PersonWithAmount(alice, 3000L, settled = false))
        )

        composeTestRule.setContent {
            IyoxVouchTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val toastManager = remember { ToastManager(snackbarHostState, scope) }

                CompositionLocalProvider(LocalToastManager provides toastManager) {
                    VouchViewContent(
                        uiState = VouchViewUiState(vouch = vouch, isLoading = false, isSettled = false),
                        sharedActions = sharedActions,
                        onNavigateBack = {},
                        onNavigateToEdit = {},
                        onNavigateToPerson = {}
                    )
                }
            }
        }

        // Click Delete button in shortcut bar
        composeTestRule.onNodeWithContentDescription("Delete Vouch").performClick()

        // Confirmation dialog should be displayed with "Delete Vouch?" and "Cancel"
        composeTestRule.onNodeWithText("Delete Vouch?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()

        // Click Cancel to dismiss
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Delete Vouch?").assertDoesNotExist()
    }

    @Test
    fun testSettle_showsConfirmationPopup() {
        val alice = Person(id = 1, name = "Alice")
        val vouch = VouchWithPerson(
            vouch = Vouch(id = 99, amount = 3000L, description = "Test Vouch", timestamp = 1700000000000L),
            persons = listOf(PersonWithAmount(alice, 3000L, settled = false))
        )

        composeTestRule.setContent {
            IyoxVouchTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val toastManager = remember { ToastManager(snackbarHostState, scope) }

                CompositionLocalProvider(LocalToastManager provides toastManager) {
                    VouchViewContent(
                        uiState = VouchViewUiState(vouch = vouch, isLoading = false, isSettled = false),
                        sharedActions = sharedActions,
                        onNavigateBack = {},
                        onNavigateToEdit = {},
                        onNavigateToPerson = {}
                    )
                }
            }
        }

        // Click Settle button in shortcut bar
        composeTestRule.onNodeWithText("Settle").performClick()

        // Confirmation dialog should be displayed with "Settle Vouch?" and "Cancel"
        composeTestRule.onNodeWithText("Settle Vouch?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()

        // Click Cancel to dismiss
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Settle Vouch?").assertDoesNotExist()
    }
}
