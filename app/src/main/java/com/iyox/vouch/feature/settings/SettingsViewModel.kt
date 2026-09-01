package com.iyox.vouch.feature.settings

import android.app.LocaleManager
import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.R
import com.iyox.vouch.core.backup.BackupPreview
import com.iyox.vouch.core.backup.BackupRepository
import com.iyox.vouch.core.backup.InvalidSignatureException
import com.iyox.vouch.core.currency.CurrencyManager
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
    val onboardingCompleted: Boolean = false
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
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun currentLanguageTag(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return "system"
        }

        val localeManager = context.getSystemService(LocaleManager::class.java)
        val current = localeManager.applicationLocales.toLanguageTags()
        return current.ifBlank { "system" }
    }

    fun refreshLanguage() {
        _languageTag.value = currentLanguageTag()
    }

    private fun resolveInitialTheme(): Int {
        val storedTheme = prefs.getInt("theme", Int.MIN_VALUE)
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
        val stored = prefs.getString("color_theme", null)
        if (stored != null) {
            return AppColorTheme.fromKey(stored)
        }
        if (prefs.contains("dynamic_colors")) {
            val isDynamic = prefs.getBoolean("dynamic_colors", true)
            return if (isDynamic) AppColorTheme.DYNAMIC else AppColorTheme.PURPLE
        }
        return AppColorTheme.DYNAMIC
    }

    private val _theme = MutableStateFlow(resolveInitialTheme())
    private val _colorTheme = MutableStateFlow(resolveInitialColorTheme())
    private val _languageTag = MutableStateFlow(currentLanguageTag())
    private val _biometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))

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
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState(
            theme = _theme.value,
            colorTheme = _colorTheme.value,
            languageTag = _languageTag.value,
            currencyCode = currencyManager.getCurrency().currencyCode,
            biometricEnabled = _biometricEnabled.value,
            onboardingCompleted = _onboardingCompleted.value
        )
    )

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean = true) {
        _onboardingCompleted.value = completed
        prefs.edit { putBoolean("onboarding_completed", completed) }
    }

    fun setTheme(theme: Int) {
        _theme.value = theme
        prefs.edit { putInt("theme", theme) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            uiModeManager.setApplicationNightMode(theme)
        } else {
            AppCompatDelegate.setDefaultNightMode(theme)
        }
    }

    fun setColorTheme(colorTheme: AppColorTheme) {
        _colorTheme.value = colorTheme
        prefs.edit {
            putString("color_theme", colorTheme.key)
            putBoolean("dynamic_colors", colorTheme == AppColorTheme.DYNAMIC)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _biometricEnabled.value = enabled
        prefs.edit { putBoolean("biometric_enabled", enabled) }
    }

    fun showBiometricError(@StringRes messageRes: Int) {
        viewModelScope.launch {
            _toastEvents.emit(ToastEvent.Error(messageRes))
        }
    }

    fun setLanguage(tag: String) {
        _languageTag.value = tag

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val appLocale = if (tag == "system") {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }

        val localeManager = context.getSystemService(LocaleManager::class.java)
        localeManager.applicationLocales = appLocale
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
            val result = backupRepository.readPreview(uri)
            if (result.isSuccess) {
                _pendingImportPreview.value = result.getOrNull()
            } else {
                val error = result.exceptionOrNull()
                error?.printStackTrace()
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
            val result = backupRepository.importData(preview.uri)
            if (result.isSuccess) {
                _toastEvents.emit(ToastEvent.Info(R.string.toast_import_success))
            } else {
                val error = result.exceptionOrNull()
                error?.printStackTrace()
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

    fun importData(uri: Uri) {
        requestImport(uri)
    }

    fun deleteAllData() {
        viewModelScope.launch {
            val result = backupRepository.deleteAllData()
            if (result.isSuccess) {
                _toastEvents.emit(ToastEvent.Info(R.string.msg_all_data_deleted))
            } else {
                result.exceptionOrNull()?.printStackTrace()
                _toastEvents.emit(ToastEvent.Error(R.string.msg_delete_data_failed))
            }
        }
    }

    val appVersion: String
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: context.getString(R.string.label_unknown)
        } catch (_: Exception) {
            context.getString(R.string.label_unknown)
        }
}