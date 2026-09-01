package com.iyox.vouch.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.R
import com.iyox.vouch.core.model.VouchSortOrder
import com.iyox.vouch.core.model.VouchTypeFilter
import com.iyox.vouch.ui.theme.IyoxVouchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VouchFilterRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testVouchFilterRow_clicks() {
        var sortToggled = 0
        var givenToggled = 0

        composeTestRule.setContent {
            IyoxVouchTheme {
                VouchFilterRow(
                    sortedBy = VouchSortOrder.DATE,
                    showOnlyGiven = VouchTypeFilter.ALL,
                    onToggleSort = { sortToggled++ },
                    onToggleGiven = { givenToggled++ }
                )
            }
        }

        val dateLabel = context.getString(R.string.filter_date)
        val filterLabel = context.getString(R.string.filter_given_received_short)

        // Sort chip (initially dateLabel)
        composeTestRule.onNodeWithText(dateLabel, substring = true, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(dateLabel, substring = true, ignoreCase = true).performClick()
        assertEquals(1, sortToggled)

        // Filter chip (initially filterLabel)
        composeTestRule.onNodeWithText(filterLabel, substring = true, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(filterLabel, substring = true, ignoreCase = true).performClick()
        assertEquals(1, givenToggled)
    }

    @Test
    fun testVouchFilterRow_searchOverlay() {
        var query = ""

        composeTestRule.setContent {
            IyoxVouchTheme {
                VouchFilterRow(
                    sortedBy = VouchSortOrder.DATE,
                    showOnlyGiven = VouchTypeFilter.ALL,
                    onToggleSort = { },
                    onToggleGiven = { },
                    searchQuery = query,
                    onSearchQueryChange = { query = it }
                )
            }
        }

        val searchCd = context.getString(R.string.cd_search)
        val placeholder = context.getString(R.string.search_vouches_placeholder)

        // Click search icon button to open search overlay
        composeTestRule.onNodeWithContentDescription(searchCd).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(searchCd).performClick()

        // Placeholder is displayed in the active search bar
        composeTestRule.onNodeWithText(placeholder).assertIsDisplayed()

        // Close search using the X button
        val cancelCd = context.getString(R.string.action_cancel)
        composeTestRule.onNodeWithContentDescription(cancelCd).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(cancelCd).performClick()

        // Search icon button is back
        composeTestRule.onNodeWithContentDescription(searchCd).assertIsDisplayed()
    }
}
