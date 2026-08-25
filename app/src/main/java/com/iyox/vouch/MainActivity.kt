package com.iyox.vouch

import android.app.UiModeManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.iyox.vouch.core.utils.LocalVibrationManager
import com.iyox.vouch.core.utils.VibrationManager

class MainActivity : FragmentActivity() {

    private var isAuthenticated = false
    private val pendingImportUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        applyStoredTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState != null) {
            isAuthenticated = savedInstanceState.getBoolean(KEY_IS_AUTHENTICATED, false)
        }

        handleIntentData(intent)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val biometricEnabled = prefs.getBoolean("biometric_enabled", false)

        if (biometricEnabled && !isAuthenticated) {
            authenticateUser()
        } else {
            showApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val biometricEnabled = prefs.getBoolean("biometric_enabled", false)

        if (biometricEnabled && !isAuthenticated) {
            authenticateUser()
        } else {
            handleIntentData(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_AUTHENTICATED, isAuthenticated)
    }

    private fun handleIntentData(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND) {
            pendingImportUri.value = uri
        }
    }

    private fun showApp() {
        val vibrationManager = VibrationManager(this)
        setContent {
            CompositionLocalProvider(LocalVibrationManager provides vibrationManager) {
                VouchApp(
                    pendingImportUri = pendingImportUri.value,
                    onImportUriConsumed = { pendingImportUri.value = null }
                )
            }
        }
    }

    private fun applyStoredTheme() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val theme = prefs.getInt("theme", UiModeManager.MODE_NIGHT_AUTO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
            uiModeManager.setApplicationNightMode(theme)
        } else {
            AppCompatDelegate.setDefaultNightMode(theme)
        }
    }

    private fun authenticateUser() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(this)
            val biometricPrompt = BiometricPrompt(
                this, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isAuthenticated = true
                        handleIntentData(intent)
                        showApp()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        finish()
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_title))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setNegativeButtonText(getString(R.string.action_cancel))
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            isAuthenticated = true
            handleIntentData(intent)
            showApp()
        }
    }

    companion object {
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
    }
}
