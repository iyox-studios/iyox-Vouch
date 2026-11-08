package com.iyox.vouch.feature.settings

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.R
import com.iyox.vouch.core.backup.BackupPreview
import com.iyox.vouch.core.backup.BackupRepository
import com.iyox.vouch.core.backup.InvalidSignatureException
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.utils.PreferenceKeys
import com.iyox.vouch.ui.theme.AppColorTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ToastEvent {
    data class Info(@field:StringRes val messageRes: Int) : ToastEvent
    data class Error(@field:StringRes val messageRes: Int) : ToastEvent
}

data class SettingsUiState(
    val theme: Int,
    val colorTheme: AppColorTheme,
    val languageTag: String,
    val currencyCode: String,
    val biometricEnabled: Boolean,
    val onboardingCompleted: Boolean = false,
    val hapticsEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val currencyManager: CurrencyManager
) : ViewModel() {

    private val _toastEvents = MutableSharedFlow<ToastEvent>()
    val toastEvents: SharedFlow<ToastEvent> = _toastEvents.asSharedFlow()

    private val _pendingImportPreview = MutableStateFlow<BackupPreview?>(null)
    val pendingImportPreview: StateFlow<BackupPreview?> = _pendingImportPreview.asStateFlow()

    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, Context.MODE_PRIVATE)

    private fun currentLanguageTag(): String {
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return current.ifBlank { "system" }
    }

    fun refreshLanguage() {
        _languageTag.value = currentLanguageTag()
    }

    private fun resolveInitialTheme(): Int {
        val storedTheme = prefs.getInt(PreferenceKeys.KEY_THEME, Int.MIN_VALUE)
        if (storedTheme != Int.MIN_VALUE) return storedTheme
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            UiModeManager.MODE_NIGHT_AUTO
        } else {
            val delegateMode = AppCompatDelegate.getDefaultNightMode()
            if (delegateMode != AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
                delegateMode
            } else {
                UiModeManager.MODE_NIGHT_AUTO
            }
        }
    }

    private fun resolveInitialColorTheme(): AppColorTheme {
        val stored = prefs.getString(PreferenceKeys.KEY_COLOR_THEME, null)
        if (stored != null) {
            return AppColorTheme.fromKey(stored)
        }
        if (prefs.contains(PreferenceKeys.KEY_DYNAMIC_COLORS)) {
            val isDynamic = prefs.getBoolean(PreferenceKeys.KEY_DYNAMIC_COLORS, true)
            return if (isDynamic) AppColorTheme.DYNAMIC else AppColorTheme.PURPLE
        }
        return AppColorTheme.DYNAMIC
    }

    private val _theme = MutableStateFlow(resolveInitialTheme())
    private val _colorTheme = MutableStateFlow(resolveInitialColorTheme())
    private val _languageTag = MutableStateFlow(currentLanguageTag())
    private val _biometricEnabled = MutableStateFlow(prefs.getBoolean(PreferenceKeys.KEY_BIOMETRIC_ENABLED, false))
    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(PreferenceKeys.KEY_HAPTICS_ENABLED, true))
    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean(PreferenceKeys.KEY_ONBOARDING_COMPLETED, false))

    val uiState: StateFlow<SettingsUiState> = combine(
        _theme, _colorTheme, _languageTag, currencyManager.currencyFlow, _biometricEnabled
    ) { theme, colorTheme, languageTag, currency, biometricEnabled ->
        SettingsUiState(
            theme = theme,
            colorTheme = colorTheme,
            languageTag = languageTag,
            currencyCode = currency.currencyCode,
            biometricEnabled = biometricEnabled,
            onboardingCompleted = _onboardingCompleted.value
        )
    }.combine(_onboardingCompleted) { state, onboardingCompleted ->
        state.copy(onboardingCompleted = onboardingCompleted)
    }.combine(_hapticsEnabled) { state, hapticsEnabled ->
        state.copy(hapticsEnabled = hapticsEnabled)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState(
            theme = _theme.value,
            colorTheme = _colorTheme.value,
            languageTag = _languageTag.value,
            currencyCode = currencyManager.getCurrency().currencyCode,
            biometricEnabled = _biometricEnabled.value,
            onboardingCompleted = _onboardingCompleted.value,
            hapticsEnabled = _hapticsEnabled.value
        )
    )

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(PreferenceKeys.KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean = true) {
        _onboardingCompleted.value = completed
        prefs.edit { putBoolean(PreferenceKeys.KEY_ONBOARDING_COMPLETED, completed) }
    }

    fun setTheme(theme: Int) {
        _theme.value = theme
        prefs.edit { putInt(PreferenceKeys.KEY_THEME, theme) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            uiModeManager.setApplicationNightMode(theme)
        } else {
            AppCompatDelegate.setDefaultNightMode(theme)
        }
    }

    fun setColorTheme(colorTheme: AppColorTheme) {
        _colorTheme.value = colorTheme
        prefs.edit {
            putString(PreferenceKeys.KEY_COLOR_THEME, colorTheme.key)
            putBoolean(PreferenceKeys.KEY_DYNAMIC_COLORS, colorTheme == AppColorTheme.DYNAMIC)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _biometricEnabled.value = enabled
        prefs.edit { putBoolean(PreferenceKeys.KEY_BIOMETRIC_ENABLED, enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        prefs.edit { putBoolean(PreferenceKeys.KEY_HAPTICS_ENABLED, enabled) }
    }

    fun showBiometricError(@StringRes messageRes: Int) {
        viewModelScope.launch {
            _toastEvents.emit(ToastEvent.Error(messageRes))
        }
    }

    fun setLanguage(tag: String) {
        _languageTag.value = tag

        val appLocale = if (tag == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }

        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun setCurrency(currencyCode: String) {
        currencyManager.setCurrency(currencyCode)
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.exportData(uri)
            if (result.isSuccess) {
                _toastEvents.emit(ToastEvent.Info(R.string.toast_export_success))
            } else {
                result.exceptionOrNull()?.printStackTrace()
                _toastEvents.emit(ToastEvent.Error(R.string.toast_export_failed))
            }
        }
    }

    fun requestImport(uri: Uri) {
        viewModelScope.launch {
            try {
                if (uri.scheme == "content") {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            } catch (_: Exception) {
                // Ignored if persistable permission is not available for this URI
            }

            val result = backupRepository.readPreview(uri)
            if (result.isSuccess) {
                _pendingImportPreview.value = result.getOrNull()
            } else {
                val error = result.exceptionOrNull()
                error?.printStackTrace()
                Log.e("SettingsViewModel", "Failed to read backup preview for $uri", error)
                if (error is InvalidSignatureException) {
                    _toastEvents.emit(ToastEvent.Error(R.string.toast_import_invalid_file))
                } else {
                    _toastEvents.emit(ToastEvent.Error(R.string.toast_import_failed))
                }
            }
        }
    }

    fun confirmImport(preview: BackupPreview) {
        _pendingImportPreview.value = null
        viewModelScope.launch {
            val result = backupRepository.importFromPreview(preview)
            if (result.isSuccess) {
                _toastEvents.emit(ToastEvent.Info(R.string.toast_import_success))
            } else {
                val error = result.exceptionOrNull()
                error?.printStackTrace()
                Log.e("SettingsViewModel", "Failed to confirm import", error)
                if (error is InvalidSignatureException) {
                    _toastEvents.emit(ToastEvent.Error(R.string.toast_import_invalid_file))
                } else {
                    _toastEvents.emit(ToastEvent.Error(R.string.toast_import_failed))
                }
            }
        }
    }

    fun dismissImportDialog() {
        _pendingImportPreview.value = null
    }

    val appVersion: String
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: context.getString(R.string.label_unknown)
        } catch (_: Exception) {
            context.getString(R.string.label_unknown)
        }
}
