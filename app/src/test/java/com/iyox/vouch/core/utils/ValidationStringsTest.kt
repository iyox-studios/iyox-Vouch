package com.iyox.vouch.core.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.iyox.vouch.R
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ValidationStringsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testValidationStringsExist() {
        assertNotEquals("", context.getString(R.string.error_amount_required))
        assertNotEquals("", context.getString(R.string.error_person_name_required))
        assertNotEquals("", context.getString(R.string.error_name_empty))
        assertNotEquals("", context.getString(R.string.msg_share_failed))
        assertNotEquals("", context.getString(R.string.msg_no_active_vouches_to_settle))
    }
}
