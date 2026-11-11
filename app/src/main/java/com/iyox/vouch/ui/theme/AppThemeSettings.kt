package com.iyox.vouch.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import com.iyox.vouch.core.utils.PreferenceKeys

internal data class AppThemeSettings(val nightMode: Int, val colorTheme: AppColorTheme) {
    fun isDark(configuration: Configuration): Boolean = when (nightMode) {
        UiModeManager.MODE_NIGHT_YES -> true
        UiModeManager.MODE_NIGHT_NO -> false
        else -> configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    companion object {
        fun read(context: Context): AppThemeSettings {
            val settings = context.getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, Context.MODE_PRIVATE)
            val colorKey = settings.getString(PreferenceKeys.KEY_COLOR_THEME, null)
                ?: if (settings.getBoolean(PreferenceKeys.KEY_DYNAMIC_COLORS, true)) {
                    AppColorTheme.DYNAMIC.key
                } else {
                    AppColorTheme.PURPLE.key
                }
            return AppThemeSettings(
                nightMode = settings.getInt(PreferenceKeys.KEY_THEME, UiModeManager.MODE_NIGHT_AUTO),
                colorTheme = AppColorTheme.fromKey(colorKey)
            )
        }
    }
}
