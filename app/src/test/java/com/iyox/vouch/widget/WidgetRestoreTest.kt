package com.iyox.vouch.widget

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class WidgetRestoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences = context.getSharedPreferences(VouchWidgetProvider.WIDGET_PREFERENCES, Context.MODE_PRIVATE)

    @Test
    fun unchangedWidgetIdKeepsItsPerson() {
        preferences.edit { putLong(VouchWidgetProvider.personKey(1), 101) }
        PersonWidgetProvider().onRestored(context, intArrayOf(1), intArrayOf(1))
        assertEquals(101L, preferences.getLong(VouchWidgetProvider.personKey(1), -1))
    }

    @Test
    fun overlappingRestoreIdsDoNotOverwriteAnotherSelection() {
        preferences.edit {
            putLong(VouchWidgetProvider.personKey(1), 101)
            putLong(VouchWidgetProvider.personKey(2), 202)
        }
        PersonWidgetProvider().onRestored(context, intArrayOf(1, 2), intArrayOf(2, 3))
        assertFalse(preferences.contains(VouchWidgetProvider.personKey(1)))
        assertEquals(101L, preferences.getLong(VouchWidgetProvider.personKey(2), -1))
        assertEquals(202L, preferences.getLong(VouchWidgetProvider.personKey(3), -1))
    }
}
