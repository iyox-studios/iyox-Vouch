package com.iyox.vouch.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.di.ApplicationScope
import com.iyox.vouch.core.utils.PreferenceKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class VouchWidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val repository: VouchRepository,
    private val currency: CurrencyManager,
    @param:ApplicationScope private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private val refreshRevision = MutableStateFlow(0L)
    internal val revision = refreshRevision.asStateFlow()
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("VouchWidgets", "Unable to refresh widgets", error)
            }
        }
    }

    internal suspend fun load(kind: WidgetKind, widgetId: Int): WidgetData {
        val personId = if (kind == WidgetKind.PERSON) {
            context.getSharedPreferences(VouchWidgetProvider.WIDGET_PREFERENCES, Context.MODE_PRIVATE)
                .getLong(VouchWidgetProvider.personKey(widgetId), -1)
        } else null
        return WidgetData(
            kind = kind,
            balance = if (kind == WidgetKind.SHORTCUTS) WidgetBalance()
                else WidgetBalance.from(repository.getAll(), personId),
            personName = personId?.let { database.personDao().getById(it)?.name },
            currencyCode = currency.getCurrency().currencyCode
        )
    }

    suspend fun updateAll() = mutex.withLock {
        refreshRevision.update { it + 1 }
        val manager = AppWidgetManager.getInstance(context)
        val glanceManager = GlanceAppWidgetManager(context)
        val providers = listOf(BalanceWidgetProvider(), PersonWidgetProvider(), ShortcutsWidgetProvider())
        for (provider in providers) {
            for (id in manager.getAppWidgetIds(ComponentName(context, provider.javaClass))) {
                provider.glanceAppWidget.update(context, glanceManager.getGlanceIdBy(id))
            }
        }
    }
}
