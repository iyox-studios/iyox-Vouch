package com.iyox.vouch

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.AppDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VouchApplication : Application() {

    /** Application-scoped coroutine scope — cancelled when the process dies. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject lateinit var currencyManager: CurrencyManager

    override fun onCreate() {
        super.onCreate()
        // Pre-warm the Room database so the first screen open has no cold-start penalty.
        // Uses a supervised applicationScope instead of GlobalScope so it is properly
        // associated with the app's lifetime.
        applicationScope.launch {
            AppDatabase.getDatabase(this@VouchApplication).vouchDao().getAll()
        }
        migrateLegacyLocalesIfNeeded()
    }

    private fun migrateLegacyLocalesIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val prefs = getSharedPreferences(com.iyox.vouch.core.utils.PreferenceKeys.PREFS_SETTINGS, MODE_PRIVATE)
        if (prefs.getBoolean("locales_migrated_33", false)) {
            return
        }

        val localeManager = getSystemService(LocaleManager::class.java)
        if (!localeManager.applicationLocales.isEmpty) {
            prefs.edit { putBoolean("locales_migrated_33", true) }
            return
        }

        val legacyTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (legacyTags.isNotBlank()) {
            localeManager.applicationLocales = LocaleList.forLanguageTags(legacyTags)
        }

        prefs.edit { putBoolean("locales_migrated_33", true) }
    }
}

