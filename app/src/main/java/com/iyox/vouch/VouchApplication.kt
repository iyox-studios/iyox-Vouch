package com.iyox.vouch

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.utils.PreferenceKeys
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

    /**
     * Eagerly injected singletons:
     * - [currencyManager] ensures [CurrencyManager.INSTANCE] is available globally on app startup.
     * - [personRepository] and [vouchRepository] start eager collection of reactive StateFlow caches in RAM
     *   for instant 0ms loads across all screens.
     */
    @Inject lateinit var currencyManager: CurrencyManager
    @Inject lateinit var personRepository: PersonRepository
    @Inject lateinit var vouchRepository: VouchRepository

    override fun onCreate() {
        super.onCreate()
        // Pre-warm the Room database so the first screen open has no cold-start penalty.
        // Uses a supervised applicationScope instead of GlobalScope so it is properly
        // associated with the app's lifetime.
        applicationScope.launch {
            val db = AppDatabase.getDatabase(this@VouchApplication)
            db.vouchDao().getAll()
            db.personDao().getPersonsTotalAll()
        }
        migrateLegacyLocalesIfNeeded()
    }

    private fun migrateLegacyLocalesIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val prefs = getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, MODE_PRIVATE)
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

