package com.iyox.vouch.navigation

import android.app.Application
import android.content.Intent
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ShortcutRequestTest {
    @Test
    fun personAndActionSurviveActivityRecreationTogether() {
        val intent = Intent(VouchShortcut.SHARE_PERSON.action)
            .putExtra(VouchShortcut.EXTRA_PERSON_NAME, "Alice")
        val request = ShortcutRequest.fromIntent(intent)!!
        val state = Bundle()
        request.saveTo(state)
        assertEquals(ShortcutRequest(VouchShortcut.SHARE_PERSON, "Alice"), ShortcutRequest.restoreFrom(state))
    }

    @Test
    fun unknownActionsDoNotCreateRequests() {
        assertNull(ShortcutRequest.fromIntent(Intent("unknown")))
        assertNull(ShortcutRequest.fromIntent(null))
        assertNull(ShortcutRequest.restoreFrom(Bundle()))
    }
}
