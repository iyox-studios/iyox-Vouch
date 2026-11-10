package com.iyox.vouch.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
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
abstract class VouchWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in setOf(
                AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED,
                AppWidgetManager.ACTION_APPWIDGET_RESTORED,
                Intent.ACTION_CONFIGURATION_CHANGED,
                Intent.ACTION_LOCALE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED
            )) {
            val pending = goAsync()
            receiverScope.launch {
                try {
                    context.widgetUpdater().updateAll()
                } catch (error: Exception) {
                    Log.e("VouchWidgets", "Unable to refresh widgets", error)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        context.getSharedPreferences(WIDGET_PREFERENCES, Context.MODE_PRIVATE).edit {
            appWidgetIds.forEach { remove(personKey(it)) }
        }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(WIDGET_PREFERENCES, Context.MODE_PRIVATE)
        prefs.edit {
            oldWidgetIds.zip(newWidgetIds).forEach { (old, new) ->
                if (prefs.contains(personKey(old))) putLong(personKey(new), prefs.getLong(personKey(old), -1))
                remove(personKey(old))
            }
        }
    }

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        internal const val WIDGET_PREFERENCES = "home_widgets"
        internal fun personKey(id: Int) = "person_$id"
    }
}

class BalanceWidgetProvider : VouchWidgetProvider()
class PersonWidgetProvider : VouchWidgetProvider()
class ShortcutsWidgetProvider : VouchWidgetProvider()
