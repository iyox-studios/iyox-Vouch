package com.iyox.vouch.feature.settings

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.core.backup.BackupRepository
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.ui.theme.AppColorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {

    private lateinit var application: Application
    private lateinit var viewModel: SettingsViewModel
    private lateinit var currencyManager: CurrencyManager

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val database = AppDatabase.getDatabase(application)
        val vouchRepository = VouchRepository(database, database.vouchDao(), database.personDao())
        val backupRepository = BackupRepository(application, database, vouchRepository)
        currencyManager = CurrencyManager(application)
        viewModel = SettingsViewModel(application, backupRepository, currencyManager)
    }

    @Test
    fun testOnboardingPreference() {
        assertFalse(viewModel.isOnboardingCompleted())

        viewModel.setOnboardingCompleted(true)
        assertTrue(viewModel.isOnboardingCompleted())

        viewModel.setOnboardingCompleted(false)
        assertFalse(viewModel.isOnboardingCompleted())
    }

    @Test
    fun testThemePreference() {
        viewModel.setTheme(UiModeManager.MODE_NIGHT_YES)
        val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
        assertEquals(UiModeManager.MODE_NIGHT_YES, prefs.getInt("theme", -1))

        viewModel.setTheme(UiModeManager.MODE_NIGHT_NO)
        assertEquals(UiModeManager.MODE_NIGHT_NO, prefs.getInt("theme", -1))
    }

    @Test
    fun testColorThemePreference() {
        viewModel.setColorTheme(AppColorTheme.GREEN)
        val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
        assertEquals("green", prefs.getString("color_theme", null))

        viewModel.setColorTheme(AppColorTheme.ORANGE)
        assertEquals("orange", prefs.getString("color_theme", null))
    }

    @Test
    fun testBiometricPreference() {
        assertFalse(viewModel.uiState.value.biometricEnabled)

        viewModel.setBiometricEnabled(true)
        val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("biometric_enabled", false))

        viewModel.setBiometricEnabled(false)
        assertFalse(prefs.getBoolean("biometric_enabled", true))
    }

    @Test
    fun testCurrencySetting() {
        viewModel.setCurrency("USD")
        assertEquals("USD", currencyManager.getCurrency().currencyCode)

        viewModel.setCurrency("EUR")
        assertEquals("EUR", currencyManager.getCurrency().currencyCode)
    }

    @Test
    fun testDismissImportDialog() {
        viewModel.dismissImportDialog()
        assertNull(viewModel.pendingImportPreview.value)
    }

    @Test
    fun testAppVersion() {
        assertNotNull(viewModel.appVersion)
        assertTrue(viewModel.appVersion.isNotEmpty())
    }
}
