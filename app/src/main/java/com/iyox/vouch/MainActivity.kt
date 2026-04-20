package com.iyox.vouch

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.iyox.vouch.core.utils.LocalVibrationManager
import com.iyox.vouch.core.utils.VibrationManager

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val vibrationManager = VibrationManager(this)
        setContent {
            CompositionLocalProvider(LocalVibrationManager provides vibrationManager) {
                VouchApp()
            }
        }
    }

    private fun authenticateUser() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    setContent {
                        VouchApp()
                    }
                }

            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle("Biometric Authentication")
            .setSubtitle("Authenticate to access the app").setNegativeButtonText("Cancel").build()
        if (BiometricManager.from(this)
                .canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            setContent {
                VouchApp()
            }
        }

    }
}

