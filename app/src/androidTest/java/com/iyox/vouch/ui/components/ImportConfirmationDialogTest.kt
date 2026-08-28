package com.iyox.vouch.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.R
import com.iyox.vouch.core.backup.BackupPreview
import com.iyox.vouch.core.backup.IyoxEnvelope
import com.iyox.vouch.ui.theme.IyoxVouchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportConfirmationDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testImportConfirmationDialog_flow() {
        val preview = BackupPreview(
            uri = Uri.parse("file:///dummy.iyxv"),
            type = IyoxEnvelope.TYPE_BACKUP,
            version = "1.0",
            vouchCount = 5,
            personCount = 3
        )

        var confirmed = false
        var dismissed = false

        composeTestRule.setContent {
            IyoxVouchTheme {
                ImportConfirmationDialog(
                    preview = preview,
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true }
                )
            }
        }

        val title = context.getString(R.string.import_dialog_title)
        val checkboxText = context.getString(R.string.import_dialog_confirm_checkbox)
        val importAction = context.getString(R.string.import_dialog_action_import)

        // Title and preview values
        composeTestRule.onNodeWithText(title, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("v1.0").assertIsDisplayed()

        // Checkbox interaction
        val importButton = composeTestRule.onNodeWithText(importAction, ignoreCase = true)
        // Initially disabled before checking checkbox
        importButton.assertIsNotEnabled()

        // Click checkbox area
        composeTestRule.onNodeWithText(checkboxText, substring = true).performClick()

        // Now import button should be enabled
        importButton.assertIsEnabled()
        importButton.performClick()
        assertEquals(true, confirmed)
    }

    @Test
    fun testImportConfirmationDialog_cancel() {
        val preview = BackupPreview(
            uri = Uri.parse("file:///dummy.iyxv"),
            type = IyoxEnvelope.TYPE_BACKUP,
            version = "1.0",
            vouchCount = 2,
            personCount = 1
        )

        var dismissed = false

        composeTestRule.setContent {
            IyoxVouchTheme {
                ImportConfirmationDialog(
                    preview = preview,
                    onConfirm = {},
                    onDismiss = { dismissed = true }
                )
            }
        }

        val cancelText = context.getString(R.string.action_cancel)
        composeTestRule.onNodeWithText(cancelText, substring = true, ignoreCase = true).performClick()
        assertEquals(true, dismissed)
    }
}
