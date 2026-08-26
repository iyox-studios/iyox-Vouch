package com.iyox.vouch

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.AppDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class VouchApplication : Application() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        CurrencyManager.init(this)
        // Pre-warm the Room database so the first screen open has no cold-start penalty
        GlobalScope.launch { AppDatabase.getDatabase(this@VouchApplication).vouchDataDao().getAll() }
        migrateLegacyLocalesIfNeeded()
    }

    private fun migrateLegacyLocalesIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
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

