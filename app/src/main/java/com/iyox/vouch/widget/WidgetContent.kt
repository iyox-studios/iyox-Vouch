package com.iyox.vouch.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.icu.util.Currency
import android.os.Build
import android.text.TextPaint
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import com.iyox.vouch.MainActivity
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.navigation.VouchShortcut

@Composable
internal fun WidgetContent(data: WidgetData, widgetId: Int) {
    val context = widgetContext(LocalContext.current)
    CompositionLocalProvider(LocalContext provides context) {
        val appearance = WidgetAppearance(context)
        val size = LocalSize.current
        val shortcuts = data.kind == WidgetKind.SHORTCUTS
        val radius = if (shortcuts) WidgetCorners.shortcut(size.width.value, size.height.value)
            else WidgetCorners.outer(size.width.value, size.height.value)
        val shape = if (shortcuts) R.drawable.widget_shortcut_round else R.drawable.widget_round
        val card = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(ImageProvider(shape), colorFilter = ColorFilter.tint(appearance.color { surfaceContainer }))
            .cornerRadius(radius.dp)
        if (shortcuts) {
            Box(card) { ShortcutContent(appearance, widgetId) }
        } else {
            val missing = data.kind == WidgetKind.PERSON && data.personName == null
            val open = if (missing) actionStartActivity(
                Intent(context, PersonWidgetConfigurationActivity::class.java)
                    .setData("vouch-widget://$widgetId/configure".toUri())
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            ) else openWidget(context, widgetId,
                if (data.kind == WidgetKind.PERSON) VouchShortcut.VIEW_PERSON else null, data.personName)
            Box(card.clickable(open, rippleOverride = R.drawable.widget_feedback)) {
                if (Build.VERSION.SDK_INT >= 31) {
                    val decoration = RemoteViews(context.packageName, R.layout.widget_decoration).apply {
                        setViewVisibility(R.id.widget_decoration, if (data.kind == WidgetKind.PERSON) View.GONE else View.VISIBLE)
                        setViewVisibility(R.id.widget_person_decoration, if (data.kind == WidgetKind.PERSON) View.VISIBLE else View.GONE)
                    }
                    appearance.applyColor(decoration, R.id.widget_decoration, "setColorFilter") { primary }
                    appearance.applyColor(decoration, R.id.widget_person_decoration, "setColorFilter") { primary }
                    AndroidRemoteViews(decoration, GlanceModifier.fillMaxSize())
                }
                BalanceContent(data, appearance, widgetId)
            }
        }
    }
}

@Composable
private fun BalanceContent(data: WidgetData, appearance: WidgetAppearance, widgetId: Int) {
    val context = appearance.context
    val width = LocalSize.current.width.value
    val height = LocalSize.current.height.value
    val fontScale = context.resources.configuration.fontScale.coerceAtLeast(1f)
    val layout = WidgetLayout.forSize(width, height, fontScale)
    val person = data.kind == WidgetKind.PERSON
    val balance = data.balance
    fun amount(value: Long) = CurrencyManager.formatAmountWithSymbol(value,
        Currency.getInstance(data.currencyCode), context.resources.configuration.locales[0])
    val total = amount(balance.total)
    val title = if (person) data.personName ?: context.getString(R.string.widget_person_missing)
        else context.getString(R.string.label_total_balance)
    val count = context.resources.getQuantityString(R.plurals.vouch_count, balance.count, balance.count)
    val badgePaint = TextPaint().apply {
        textSize = appearance.spToDp(12f) * context.resources.displayMetrics.density
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        letterSpacing = 0.0416667f
    }
    val badgeWidth = badgePaint.measureText(count) / context.resources.displayMetrics.density + 20f
    val amountPaint = TextPaint().apply {
        textSize = appearance.spToDp(32f) * context.resources.displayMetrics.density
        typeface = Typeface.DEFAULT_BOLD
        textSize *= (40f * context.resources.displayMetrics.density / (fontMetrics.descent - fontMetrics.ascent)).coerceAtMost(1f)
    }
    val beside = appearance.spToDp(16f) + 8f <= 40f && width - 24f >=
        amountPaint.measureText(total) / context.resources.displayMetrics.density + 8f + badgeWidth
    val actions = person && data.personName != null && height >= 152f + 12f * (fontScale - 1f)
    val countHeight = if (actions) 152f + 28f * fontScale else 100f + 24f * fontScale
    val countBelow = if (person) height >= countHeight else layout.count
    val amountWidth = width - 24f - (if (beside) badgeWidth + 8f else 0f)
    val description = "$title, $total, " + context.resources.getQuantityString(
        R.plurals.widget_unsettled_count, balance.count, balance.count) +
        ", ${context.getString(R.string.label_given)} ${amount(balance.given)}, " +
        "${context.getString(R.string.label_received)} ${amount(balance.received)}"
    Column(GlanceModifier.fillMaxSize().padding(12.dp).semantics { contentDescription = description }) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(GlanceModifier.defaultWeight()) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val icon = when {
                        person && data.personName == null -> R.drawable.ic_settings
                        person -> R.drawable.ic_person
                        balance.total > 0 -> R.drawable.ic_arrow_outward
                        balance.total < 0 -> R.drawable.ic_south_west
                        else -> R.drawable.ic_check_circle
                    }
                    WidgetIcon(icon, appearance, GlanceModifier.size(20.dp)) { if (balance.total < 0) error else primary }
                    Spacer(GlanceModifier.width(8.dp))
                    WidgetText(title, appearance, GlanceModifier.defaultWeight(),
                        style = if (person) WidgetTextStyle.PERSON_TITLE else WidgetTextStyle.TITLE,
                        color = { onSurfaceVariant })
                }
                Spacer(GlanceModifier.height(4.dp))
                WidgetText(if (person && data.personName == null) context.getString(R.string.widget_amount_placeholder) else total,
                    appearance, GlanceModifier.fillMaxWidth().height(40.dp),
                    style = WidgetTextStyle.AMOUNT,
                    size = appearance.amountSize(total, amountWidth, 40f, 36, 12), fixedHeight = 40f,
                    color = { if (balance.total < 0) error else onSurface })
                if (!beside && countBelow) {
                    Spacer(GlanceModifier.height(4.dp))
                    CountBadge(count, badgeWidth, appearance)
                }
            }
            if (beside) {
                Spacer(GlanceModifier.width(8.dp))
                CountBadge(count, badgeWidth, appearance)
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        if (!person && layout.split) {
            Spacer(GlanceModifier.height(8.dp))
            val labels = !layout.stacked || (width >= 160f && height >= 220f + 36f * fontScale)
            val tileWidth = if (layout.stacked) width - 24f else (width - 30f) / 2f
            if (layout.stacked) {
                SplitTile(amount(balance.given), given = true, stacked = true, labels, tileWidth, appearance)
                Spacer(GlanceModifier.height(6.dp))
                SplitTile(amount(balance.received), given = false, stacked = true, labels, tileWidth, appearance)
            } else {
                Row(GlanceModifier.fillMaxWidth()) {
                    SplitTile(amount(balance.given), given = true, stacked = false, labels = true, tileWidth, appearance)
                    Spacer(GlanceModifier.width(6.dp))
                    SplitTile(amount(balance.received), given = false, stacked = false, labels = true, tileWidth, appearance)
                }
            }
        }
        if (actions) {
            Spacer(GlanceModifier.height(8.dp))
            Row(GlanceModifier.fillMaxWidth().height(48.dp)) {
                val buttons = listOf(
                    Triple(VouchShortcut.ADD_VOUCH, R.drawable.ic_add, R.string.action_add_vouch),
                    Triple(VouchShortcut.SHARE_PERSON, R.drawable.ic_share, R.string.share_vouch_summary_title),
                    Triple(VouchShortcut.SETTLE_PERSON, R.drawable.ic_done_all, R.string.widget_settle_all)
                )
                buttons.forEachIndexed { index, (shortcut, icon, label) ->
                    if (index > 0) Spacer(GlanceModifier.width(6.dp))
                    ActionTile(icon, context.getString(label), appearance, GlanceModifier.defaultWeight().height(48.dp),
                        openWidget(context, widgetId, shortcut, data.personName),
                        personIcon = true)
                }
            }
        }
    }
}

@Composable
private fun CountBadge(text: String, width: Float, appearance: WidgetAppearance) {
    Box(GlanceModifier.width(width.dp).wrapContentHeight()
        .background(ImageProvider(R.drawable.widget_badge), colorFilter = ColorFilter.tint(appearance.color { surfaceContainerHigh }))) {
        WidgetText(text, appearance, style = WidgetTextStyle.COUNT)
    }
}

@Composable
private fun SplitTile(
    text: String,
    given: Boolean,
    stacked: Boolean,
    labels: Boolean,
    width: Float,
    appearance: WidgetAppearance
) {
    val color: ColorScheme.() -> Color = { if (given) onPrimaryContainer else onErrorContainer }
    val icon = if (given) R.drawable.ic_arrow_outward else R.drawable.ic_south_west
    val label = appearance.context.getString(if (given) R.string.label_given else R.string.label_received)
    val modifier = GlanceModifier.width(width.dp).background(ImageProvider(R.drawable.widget_split_tile),
        colorFilter = ColorFilter.tint(appearance.color { if (given) primaryContainer else errorContainer }))
        .cornerRadius(WidgetCorners.split(width, if (stacked) 36f else 54f).dp)
    if (stacked) {
        Row(modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            WidgetIcon(icon, appearance, GlanceModifier.size(24.dp), color)
            Spacer(GlanceModifier.width(4.dp))
            Column(GlanceModifier.defaultWeight()) {
                if (labels) WidgetText(label, appearance, style = WidgetTextStyle.SPLIT_LABEL, color = color)
                WidgetText(text, appearance, GlanceModifier.fillMaxWidth().height(24.dp),
                    style = WidgetTextStyle.STACKED_AMOUNT,
                    size = appearance.amountSize(text, width - 40f, 24f, 18, 8), fixedHeight = 24f, color = color)
            }
        }
    } else {
        Column(modifier.padding(10.dp)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WidgetIcon(icon, appearance, GlanceModifier.size(24.dp), color)
                Spacer(GlanceModifier.width(6.dp))
                WidgetText(label, appearance, GlanceModifier.defaultWeight(), style = WidgetTextStyle.SPLIT_LABEL, color = color)
            }
            Spacer(GlanceModifier.height(4.dp))
            WidgetText(text, appearance, GlanceModifier.fillMaxWidth(),
                style = WidgetTextStyle.SPLIT_AMOUNT,
                size = appearance.amountSize(text, width - 20f, appearance.textHeight(17f), 18, 10),
                fixedHeight = appearance.textHeight(17f), color = color)
        }
    }
}

@Composable
private fun ShortcutContent(appearance: WidgetAppearance, widgetId: Int) {
    val width = LocalSize.current.width.value
    val height = LocalSize.current.height.value
    val fontScale = appearance.context.resources.configuration.fontScale.coerceAtLeast(1f)
    val paint = TextPaint().apply {
        textSize = appearance.spToDp(12f) * appearance.context.resources.displayMetrics.density
        typeface = Typeface.DEFAULT_BOLD
    }
    val density = appearance.context.resources.displayMetrics.density
    val lineHeight = (paint.fontMetrics.descent - paint.fontMetrics.ascent) / density
    val twoLines = 2f * lineHeight <= height - 40f
    fun labelWidth(text: String): Float {
        val breaks = text.indices.filter { text[it].isWhitespace() }
        return if (!twoLines || breaks.isEmpty()) paint.measureText(text) / density else breaks.minOf {
            maxOf(paint.measureText(text.substring(0, it)), paint.measureText(text.substring(it + 1))) / density
        }
    }
    val add = appearance.context.getString(R.string.widget_new_vouch)
    val group = appearance.context.getString(R.string.widget_new_group)
    val labels = lineHeight <= height - 40f && width >= maxOf(240f * fontScale, 36f + 2f * (52f + maxOf(labelWidth(add), labelWidth(group))))
    Row(GlanceModifier.fillMaxSize().padding(12.dp)) {
        ActionTile(R.drawable.ic_add, appearance.context.getString(R.string.action_add_vouch), appearance,
            GlanceModifier.fillMaxHeight().defaultWeight(), openWidget(appearance.context, widgetId, VouchShortcut.ADD_VOUCH),
            if (labels) add else null)
        Spacer(GlanceModifier.width(12.dp))
        ActionTile(R.drawable.ic_group_add, appearance.context.getString(R.string.action_add_group_vouch), appearance,
            GlanceModifier.fillMaxHeight().defaultWeight(), openWidget(appearance.context, widgetId, VouchShortcut.ADD_GROUP_VOUCH),
            if (labels) group else null, tertiary = true)
    }
}

@Composable
private fun ActionTile(
    @DrawableRes icon: Int,
    description: String,
    appearance: WidgetAppearance,
    modifier: GlanceModifier,
    action: Action,
    label: String? = null,
    tertiary: Boolean = false,
    personIcon: Boolean = false
) {
    val color: ColorScheme.() -> Color = { if (tertiary) onTertiaryContainer else onPrimaryContainer }
    val size = LocalSize.current
    val actionWidth = (size.width.value - 36f) / if (personIcon) 3f else 2f
    val actionHeight = if (personIcon) 48f else size.height.value - 24f
    Box(modifier.background(ImageProvider(R.drawable.widget_action_tile),
        colorFilter = ColorFilter.tint(appearance.color { if (tertiary) tertiaryContainer else primaryContainer }))
        .cornerRadius(WidgetCorners.action(actionWidth, actionHeight).dp)
        .clickable(action, rippleOverride = R.drawable.widget_action_feedback)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        if (personIcon) {
            val views = RemoteViews(appearance.context.packageName, R.layout.widget_action_icon).apply {
                setImageViewResource(R.id.widget_action_icon, icon)
            }
            appearance.applyColor(views, R.id.widget_action_icon, "setColorFilter", color)
            AndroidRemoteViews(views, GlanceModifier.fillMaxWidth().height(28.dp))
        } else Row(GlanceModifier.fillMaxSize().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
            WidgetIcon(icon, appearance, GlanceModifier.size(28.dp), color)
            if (label != null) {
                Spacer(GlanceModifier.width(8.dp))
                WidgetText(label, appearance, GlanceModifier.defaultWeight(), style = WidgetTextStyle.SHORTCUT_LABEL, color = color)
            }
        }
    }
}

@Composable
private fun WidgetIcon(
    @DrawableRes resource: Int,
    appearance: WidgetAppearance,
    modifier: GlanceModifier = GlanceModifier,
    color: ColorScheme.() -> Color
) {
    Image(ImageProvider(resource), null, modifier, colorFilter = ColorFilter.tint(appearance.color(color)))
}

private fun openWidget(
    context: Context,
    widgetId: Int,
    shortcut: VouchShortcut? = null,
    personName: String? = null
): Action =
    actionStartActivity(Intent(context, MainActivity::class.java).apply {
        action = shortcut?.action
        putExtra(VouchShortcut.EXTRA_PERSON_NAME, personName)
        data = "vouch-widget://$widgetId/${shortcut?.action ?: "balance"}".toUri()
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    })
