package com.iyox.vouch.viewmodel

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val uiModeManager = application.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(uiModeManager.nightMode)
    val theme: StateFlow<Int> = _theme.asStateFlow()

    private val _dynamicColors = MutableStateFlow(prefs.getBoolean("dynamic_colors", true))
    val dynamicColors: StateFlow<Boolean> = _dynamicColors.asStateFlow()

    fun setTheme(theme: Int) {
        _theme.value = theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            uiModeManager.setApplicationNightMode(theme)
        }
        else {
            AppCompatDelegate.setDefaultNightMode(theme)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        _dynamicColors.value = enabled
    }

    val appVersion: String
        get() = try {
            val pInfo = getApplication<Application>().packageManager.getPackageInfo(
                getApplication<Application>().packageName, 0
            )
            pInfo.versionName ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
}