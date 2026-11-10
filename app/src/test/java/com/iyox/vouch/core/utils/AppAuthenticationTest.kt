package com.iyox.vouch.core.utils

import android.app.Application
import android.content.Context
import android.hardware.biometrics.BiometricManager
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBiometricManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppAuthenticationTest {
    @Test
    fun disabledLockAllowsPrivateContent() {
        Robolectric.buildActivity(FragmentActivity::class.java).setup().use { controller ->
            var opened = 0
            controller.get().authenticateForApp { opened++ }
            assertEquals(1, opened)
        }
    }

    @Test
    fun enabledLockWaitsForAuthenticationBeforeOpeningPrivateContent() {
        Robolectric.buildActivity(FragmentActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, Context.MODE_PRIVATE).edit {
                putBoolean(PreferenceKeys.KEY_BIOMETRIC_ENABLED, true)
            }
            Shadow.extract<ShadowBiometricManager>(activity.getSystemService(BiometricManager::class.java))
                .setCanAuthenticate(true)
            var opened = 0
            activity.authenticateForApp { opened++ }
            assertEquals(0, opened)
        }
    }

    @Test
    fun unavailableBiometricsDoNotBypassAnEnabledLock() {
        Robolectric.buildActivity(FragmentActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, Context.MODE_PRIVATE).edit {
                putBoolean(PreferenceKeys.KEY_BIOMETRIC_ENABLED, true)
            }
            Shadow.extract<ShadowBiometricManager>(activity.getSystemService(BiometricManager::class.java))
                .setCanAuthenticate(false)
            var opened = 0
            activity.authenticateForApp { opened++ }
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(0, opened)
        }
    }
}
