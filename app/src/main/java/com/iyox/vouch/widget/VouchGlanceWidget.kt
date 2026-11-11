package com.iyox.vouch.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

internal enum class WidgetKind { BALANCE, PERSON, SHORTCUTS }

internal data class WidgetData(
    val kind: WidgetKind,
    val balance: WidgetBalance = WidgetBalance(),
    val personName: String? = null,
    val currencyCode: String = "EUR"
)

internal class VouchGlanceWidget(private val kind: WidgetKind) : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition = null

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val updater = context.widgetUpdater()
        val initialRevision = updater.revision.value
        val initial = updater.load(kind, widgetId)
        provideContent {
            // Glance can keep a composition alive across update() calls. Reload its Room snapshot
            // inside that composition too, rather than capturing stale data in provideGlance.
            val revision by updater.revision.collectAsState()
            val data by produceState(initial, revision) {
                if (revision != initialRevision) value = updater.load(kind, widgetId)
            }
            key(revision) { WidgetContent(data, widgetId) }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val sample = WidgetData(kind, WidgetBalance(17500, 5000, 2),
            if (kind == WidgetKind.PERSON) context.getString(com.iyox.vouch.R.string.widget_preview_person) else null)
        provideContent { WidgetContent(sample, 0) }
    }
}
