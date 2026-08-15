package com.iyox.vouch

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.iyox.vouch.core.currency.CurrencyManager

class VouchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CurrencyManager.init(this)
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

