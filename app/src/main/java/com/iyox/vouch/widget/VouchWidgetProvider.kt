package com.iyox.vouch.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun widgets(): VouchWidgetUpdater
}

internal fun Context.widgetUpdater(): VouchWidgetUpdater =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java).widgets()

/** A fresh database snapshot is used even when a launcher starts a cold app process. */
abstract class VouchWidgetProvider : GlanceAppWidgetReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(
                Intent.ACTION_LOCALE_CHANGED,
                Intent.ACTION_CONFIGURATION_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED
            )) {
            val pending = goAsync()
            receiverScope.launch {
                try {
                    context.widgetUpdater().updateAll()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("VouchWidgets", "Unable to refresh widgets", error)
                } finally {
                    pending.finish()
                }
            }
        } else super.onReceive(context, intent)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        context.getSharedPreferences(WIDGET_PREFERENCES, Context.MODE_PRIVATE).edit {
            appWidgetIds.forEach { remove(personKey(it)) }
        }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(WIDGET_PREFERENCES, Context.MODE_PRIVATE)
        val selections = oldWidgetIds.zip(newWidgetIds).mapNotNull { (old, new) ->
            if (prefs.contains(personKey(old))) new to prefs.getLong(personKey(old), -1) else null
        }
        prefs.edit {
            oldWidgetIds.forEach { remove(personKey(it)) }
            selections.forEach { (id, person) -> putLong(personKey(id), person) }
        }
    }

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        internal const val WIDGET_PREFERENCES = "home_widgets"
        internal fun personKey(id: Int) = "person_$id"
    }
}

class BalanceWidgetProvider : VouchWidgetProvider() {
    override val glanceAppWidget: GlanceAppWidget = VouchGlanceWidget(WidgetKind.BALANCE)
}
class PersonWidgetProvider : VouchWidgetProvider() {
    override val glanceAppWidget: GlanceAppWidget = VouchGlanceWidget(WidgetKind.PERSON)
}
class ShortcutsWidgetProvider : VouchWidgetProvider() {
    override val glanceAppWidget: GlanceAppWidget = VouchGlanceWidget(WidgetKind.SHORTCUTS)
}
