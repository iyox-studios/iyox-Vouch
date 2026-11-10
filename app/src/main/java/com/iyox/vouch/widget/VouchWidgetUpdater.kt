package com.iyox.vouch.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.di.ApplicationScope
import com.iyox.vouch.core.utils.PreferenceKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VouchWidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val repository: VouchRepository,
    private val currency: CurrencyManager,
    @param:ApplicationScope private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private val settings = context.getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, Context.MODE_PRIVATE)
    // Keep the listener strongly referenced; SharedPreferences holds only a weak reference.
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    fun start() {
        settings.registerOnSharedPreferenceChangeListener(preferenceListener)
        scope.launch {
            database.invalidationTracker.createFlow("Vouch", "Person", "vouch_person_cross_ref")
                .collectLatest { refresh() }
        }
        scope.launch {
            currency.currencyFlow.collectLatest { refresh() }
        }
    }

    private fun refresh() {
        scope.launch {
            try {
                updateAll()
            } catch (error: Exception) {
                Log.e("VouchWidgets", "Unable to refresh widgets", error)
            }
        }
    }

    suspend fun updateAll() = mutex.withLock {
        val manager = AppWidgetManager.getInstance(context)
        val providers = listOf(BalanceWidgetProvider::class.java, PersonWidgetProvider::class.java, ShortcutsWidgetProvider::class.java)
        val ids = providers.associateWith { manager.getAppWidgetIds(ComponentName(context, it)) }
        if (ids.values.all { it.isEmpty() }) return@withLock
        val vouches = repository.getAll()
        val people = database.personDao().getPersonsTotalAll().associate { it.person.id to it.person.name }
        val prefs = context.getSharedPreferences(VouchWidgetProvider.WIDGET_PREFERENCES, Context.MODE_PRIVATE)
        val renderer = VouchWidgetRenderer(context, currency.getCurrency())
        for ((provider, widgetIds) in ids) {
            for (id in widgetIds) {
                val personId = if (provider == PersonWidgetProvider::class.java) {
                    prefs.getLong(VouchWidgetProvider.personKey(id), -1)
                } else null
                manager.updateAppWidget(id, renderer.render(
                    id = id,
                    shortcuts = provider == ShortcutsWidgetProvider::class.java,
                    balance = WidgetBalance.from(vouches, personId),
                    personName = personId?.let { people[it] },
                    isPerson = personId != null,
                    options = manager.getAppWidgetOptions(id)
                ))
            }
        }
    }
}
