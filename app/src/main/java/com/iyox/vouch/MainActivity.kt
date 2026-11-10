package com.iyox.vouch

import android.app.UiModeManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import com.iyox.vouch.core.utils.authenticateForApp
import com.iyox.vouch.core.utils.PreferenceKeys
import dagger.hilt.android.AndroidEntryPoint
import com.iyox.vouch.navigation.VouchShortcut

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var isAuthenticated = false
    private var isIntentConsumed = false
    private val pendingImportUri = mutableStateOf<Uri?>(null)
    private val pendingShortcut = mutableStateOf<VouchShortcut?>(null)
    private val pendingShortcutPersonName = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        applyStoredTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState != null) {
            pendingShortcut.value = VouchShortcut.fromAction(savedInstanceState.getString(KEY_PENDING_SHORTCUT))
            pendingShortcutPersonName.value = savedInstanceState.getString(VouchShortcut.EXTRA_PERSON_NAME)
            isAuthenticated = savedInstanceState.getBoolean(KEY_IS_AUTHENTICATED, false)
            isIntentConsumed = savedInstanceState.getBoolean(KEY_IS_INTENT_CONSUMED, false)
        }

        if (!isIntentConsumed) {
            handleIntentData(intent)
        }

        val prefs = getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, MODE_PRIVATE)
        val biometricEnabled = prefs.getBoolean(PreferenceKeys.KEY_BIOMETRIC_ENABLED, false)

        if (biometricEnabled && !isAuthenticated) {
            authenticateUser()
        } else {
            showApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        isIntentConsumed = false
        val prefs = getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, MODE_PRIVATE)
        val biometricEnabled = prefs.getBoolean(PreferenceKeys.KEY_BIOMETRIC_ENABLED, false)

        if (biometricEnabled && !isAuthenticated) {
            authenticateUser()
        } else {
            handleIntentData(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PENDING_SHORTCUT, pendingShortcut.value?.action)
        outState.putString(VouchShortcut.EXTRA_PERSON_NAME, pendingShortcutPersonName.value)
        outState.putBoolean(KEY_IS_AUTHENTICATED, isAuthenticated)
        outState.putBoolean(KEY_IS_INTENT_CONSUMED, isIntentConsumed)
    }

    private fun extractUriFromIntent(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                } ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri ?: intent.data
            }
            else -> null
        }
    }

    private fun handleIntentData(intent: Intent?) {
        if (isIntentConsumed) return
        VouchShortcut.fromAction(intent?.action)?.let {
            pendingShortcut.value = it
            pendingShortcutPersonName.value = intent?.getStringExtra(VouchShortcut.EXTRA_PERSON_NAME)
            isIntentConsumed = true
            return
        }
        val uri = extractUriFromIntent(intent) ?: return
        if (intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_SEND) {
            pendingImportUri.value = uri
        }
    }

    private fun consumePendingImport() {
        pendingImportUri.value = null
        isIntentConsumed = true
        intent?.action = null
        intent?.data = null
        intent?.removeExtra(Intent.EXTRA_STREAM)
    }

    private fun showApp() {
        setContent {
            VouchApp(
                pendingImportUri = pendingImportUri.value,
                pendingShortcut = pendingShortcut.value,
                pendingShortcutPersonName = pendingShortcutPersonName.value,
                onShortcutConsumed = {
                    pendingShortcut.value = null
                    pendingShortcutPersonName.value = null
                },
                onImportUriConsumed = { consumePendingImport() }
            )
        }
    }

    private fun applyStoredTheme() {
        val prefs = getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, MODE_PRIVATE)
        val theme = prefs.getInt(PreferenceKeys.KEY_THEME, UiModeManager.MODE_NIGHT_AUTO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
            uiModeManager.setApplicationNightMode(theme)
        } else {
            AppCompatDelegate.setDefaultNightMode(theme)
        }
    }

    private fun authenticateUser() {
        authenticateForApp {
            isAuthenticated = true
            handleIntentData(intent)
            showApp()
        }
    }

    companion object {
        private const val KEY_PENDING_SHORTCUT = "pending_shortcut"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
        private const val KEY_IS_INTENT_CONSUMED = "is_intent_consumed"
    }
}
