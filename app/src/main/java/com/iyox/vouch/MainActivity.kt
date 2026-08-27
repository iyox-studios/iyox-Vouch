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
    private var isIntentConsumed = false
    private val pendingImportUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        applyStoredTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState != null) {
            isAuthenticated = savedInstanceState.getBoolean(KEY_IS_AUTHENTICATED, false)
            isIntentConsumed = savedInstanceState.getBoolean(KEY_IS_INTENT_CONSUMED, false)
        }

        if (!isIntentConsumed) {
            handleIntentData(intent)
        }

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
        isIntentConsumed = false
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
        val vibrationManager = VibrationManager(this)
        setContent {
            CompositionLocalProvider(LocalVibrationManager provides vibrationManager) {
                VouchApp(
                    pendingImportUri = pendingImportUri.value,
                    onImportUriConsumed = { consumePendingImport() }
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
        private const val KEY_IS_INTENT_CONSUMED = "is_intent_consumed"
    }
}
