package com.iyox.vouch.ui.theme

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.iyox.vouch.core.utils.PreferenceKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppThemeSettingsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences = context.getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, Context.MODE_PRIVATE)

    @Test
    fun explicitPaletteOverridesLegacyDynamicPreference() {
        preferences.edit {
            putBoolean(PreferenceKeys.KEY_DYNAMIC_COLORS, false)
        }
        assertEquals(AppColorTheme.PURPLE, AppThemeSettings.read(context).colorTheme)
        preferences.edit { putString(PreferenceKeys.KEY_COLOR_THEME, AppColorTheme.BLUE.key) }
        assertEquals(AppColorTheme.BLUE, AppThemeSettings.read(context).colorTheme)
    }

    @Test
    fun systemThemeFollowsConfigurationUnlessAnExplicitModeIsStored() {
        val dark = Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_YES }
        val light = Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_NO }
        val automatic = AppThemeSettings.read(context)
        assertEquals(AppColorTheme.DYNAMIC, automatic.colorTheme)
        assertTrue(automatic.isDark(dark))
        assertFalse(automatic.isDark(light))
        preferences.edit { putInt(PreferenceKeys.KEY_THEME, UiModeManager.MODE_NIGHT_NO) }
        assertFalse(AppThemeSettings.read(context).isDark(dark))
        preferences.edit { putInt(PreferenceKeys.KEY_THEME, UiModeManager.MODE_NIGHT_YES) }
        assertTrue(AppThemeSettings.read(context).isDark(light))
    }
}
