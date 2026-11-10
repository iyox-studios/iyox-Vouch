package com.iyox.vouch.core.utils

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.iyox.vouch.R

/** Applies the same app lock to every entry point that exposes private data. */
internal fun FragmentActivity.authenticateForApp(onAuthenticated: () -> Unit) {
    val settings = getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, android.content.Context.MODE_PRIVATE)
    if (!settings.getBoolean(PreferenceKeys.KEY_BIOMETRIC_ENABLED, false)) {
        onAuthenticated()
        return
    }

    // Let the prompt report unavailable hardware through onAuthenticationError; an enabled lock
    // must never turn a temporary sensor failure or lockout into access to private data.
    val prompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onAuthenticated()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                finish()
            }
        }
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.action_cancel))
            .build()
    )
}
