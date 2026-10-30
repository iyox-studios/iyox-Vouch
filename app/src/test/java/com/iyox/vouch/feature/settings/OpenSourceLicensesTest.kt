package com.iyox.vouch.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpenSourceLicensesTest {

    @Test
    fun testLoadLibrariesFromResources() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val libs = Libs.Builder().withContext(context).build()
        assertTrue("Libraries should not be empty", libs.libraries.isNotEmpty())
    }
}
