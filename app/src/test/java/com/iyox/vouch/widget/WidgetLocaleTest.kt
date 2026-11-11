package com.iyox.vouch.widget

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [25], application = Application::class)
class WidgetLocaleTest {
    @Test
    fun widgetOnlyColdStartRestoresThePersistedAppLanguage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Fixture matching AppCompat's persisted state, before any Activity initializes it.
        context.openFileOutput("androidx.appcompat.app.AppCompatDelegate.application_locales_record_file", Context.MODE_PRIVATE)
            .use { it.write("<locales application_locales=\"de\" />".toByteArray()) }
        assertTrue(AppCompatDelegate.getApplicationLocales().isEmpty)
        val english = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.ENGLISH)
        })
        assertEquals("de", widgetContext(english).resources.configuration.locales[0].language)
    }
}
