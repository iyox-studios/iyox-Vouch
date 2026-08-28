package com.iyox.vouch.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.R
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
                    sortedBy = 0,
                    showOnlyGiven = 0,
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
}
