package com.iyox.vouch.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.UiModeManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.icu.util.Currency
import android.os.Build
import android.os.Bundle
import android.text.TextPaint
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.iyox.vouch.MainActivity
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.navigation.VouchShortcut
import com.iyox.vouch.ui.theme.AppThemeSettings
import com.iyox.vouch.ui.theme.resolveAppColorScheme
import java.util.Locale

internal class VouchWidgetRenderer(baseContext: Context, private val currency: Currency) {
    // Both app languages are bundled: app/build.gradle.kts disables language splitting.
    @SuppressLint("AppBundleLocaleChanges")
    private val context = if (Build.VERSION.SDK_INT < 33 && !AppCompatDelegate.getApplicationLocales().isEmpty) {
        val configuration = Configuration(baseContext.resources.configuration)
        configuration.setLocales(android.os.LocaleList.forLanguageTags(AppCompatDelegate.getApplicationLocales().toLanguageTags()))
        baseContext.createConfigurationContext(configuration)
    } else baseContext
    private val settings = AppThemeSettings.read(context)
    private val theme = settings.nightMode
    private val dark = settings.isDark(context.resources.configuration)
    private val lightPalette = resolveAppColorScheme(context, false, settings.colorTheme)
    private val darkPalette = resolveAppColorScheme(context, true, settings.colorTheme)
    private val palette = if (dark) darkPalette else lightPalette
    private val locale: Locale = context.resources.configuration.locales[0]

    fun render(
        id: Int,
        shortcuts: Boolean,
        balance: WidgetBalance,
        personName: String?,
        isPerson: Boolean,
        options: Bundle
    ): RemoteViews {
        fun at(width: Float, height: Float) = if (shortcuts) shortcuts(id, width, height) else balance(id, balance, personName, isPerson, width, height)
        if (Build.VERSION.SDK_INT >= 31) {
            @Suppress("DEPRECATION")
            val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                ?.filter { it.width > 0 && it.height > 0 }?.distinct()
            if (!sizes.isNullOrEmpty()) {
                // Exact host-reported sizes account for grid, orientation and launcher padding.
                return RemoteViews(sizes.take(16).associateWith { at(it.width, it.height) })
            }
        }
        // Before Android 12 the launcher reports two orientation ranges rather than exact sizes.
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180).toFloat()
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, if (shortcuts) 72 else 110).toFloat()
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth.toInt()).toFloat()
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight.toInt()).toFloat()
        return RemoteViews(at(maxWidth, minHeight), at(minWidth, maxHeight))
    }

    internal fun balance(id: Int, balance: WidgetBalance, personName: String?, isPerson: Boolean, width: Float, height: Float): RemoteViews {
        val layout = WidgetLayout.forSize(width, height, context.resources.configuration.fontScale)
        val views = RemoteViews(context.packageName, if (layout.stacked) R.layout.widget_balance_stacked else R.layout.widget_balance)
        views.cardBackground(width, height)
        views.tint(R.id.widget_decoration) { primary }
        views.tint(R.id.widget_person_decoration) { primary }
        // XML clipToOutline is supported by remote layouts from Android 12.
        // Older hosts keep the rounded card without a decoration crossing its corners.
        views.setViewVisibility(R.id.widget_decoration, if (Build.VERSION.SDK_INT >= 31 && !isPerson) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_person_decoration, if (Build.VERSION.SDK_INT >= 31 && isPerson) View.VISIBLE else View.GONE)
        views.tint(R.id.widget_icon) { if (balance.total < 0) error else primary }
        views.setImageViewResource(R.id.widget_icon, when {
            isPerson && personName == null -> R.drawable.ic_settings
            isPerson -> R.drawable.ic_person
            balance.total > 0 -> R.drawable.ic_arrow_outward
            balance.total < 0 -> R.drawable.ic_south_west
            else -> R.drawable.ic_check_circle
        })
        val title = if (isPerson) personName ?: context.getString(R.string.widget_person_missing) else context.getString(R.string.label_total_balance)
        views.setTextViewText(R.id.widget_title, title)
        views.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, if (isPerson) 16f else 12f)
        views.textColor(R.id.widget_title) { onSurfaceVariant }
        views.setTextViewText(R.id.widget_amount, if (isPerson && personName == null) context.getString(R.string.widget_amount_placeholder) else amount(balance.total))
        views.textColor(R.id.widget_amount) { if (balance.total < 0) error else onSurface }
        val countText = context.resources.getQuantityString(R.plurals.vouch_count, balance.count, balance.count)
        val metrics = context.resources.displayMetrics
        val badgePaint = TextPaint().apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, metrics)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.0416667f
        }
        val badgeWidth = badgePaint.measureText(countText) / metrics.density + 20f
        val amountPaint = TextPaint().apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 32f, metrics)
            typeface = Typeface.DEFAULT_BOLD
            // Match the amount's fixed-height line before testing horizontal space.
            val lineHeight = fontMetrics.descent - fontMetrics.ascent
            textSize *= (40f * metrics.density / lineHeight).coerceAtMost(1f)
        }
        val badgeHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, metrics) / metrics.density + 8f
        val badgeOnRight = badgeHeight <= 40f &&
            width - 24f >= amountPaint.measureText(amount(balance.total)) / metrics.density + 8f + badgeWidth
        views.fitLegacyAmount(R.id.widget_amount, balance.total,
            width - 24f - (if (badgeOnRight) badgeWidth + 8f else 0f), 32f)
        val badge = RemoteViews(context.packageName, R.layout.widget_count_badge).apply {
            setTextViewText(R.id.widget_count, countText)
            textColor(R.id.widget_count) { onSurface }
            tint(R.id.widget_count_background) { surfaceContainerHigh }
        }
        views.removeAllViews(R.id.widget_count_side)
        views.removeAllViews(R.id.widget_count_below)
        views.addView(if (badgeOnRight) R.id.widget_count_side else R.id.widget_count_below, badge)
        val personActions = isPerson && personName != null
        val fontScale = context.resources.configuration.fontScale.coerceAtLeast(1f)
        val showActions = personActions && height >= 152f + 12f * (fontScale - 1f)
        val actionCountHeight = 152f + 28f * fontScale
        val showCount = if (isPerson) {
            height >= if (showActions) actionCountHeight else 100f + 24f * fontScale
        } else layout.count
        views.setViewVisibility(R.id.widget_count_side, if (badgeOnRight) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_count_below, if (showCount && !badgeOnRight) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_split, if (layout.split && !isPerson) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_person_actions, if (showActions) View.VISIBLE else View.GONE)
        views.tint(R.id.given_background) { primaryContainer }
        views.tint(R.id.received_background) { errorContainer }
        val splitWidth = if (layout.stacked) width - 24f else (width - 30f) / 2f
        val splitRadius = WidgetCorners.split(splitWidth, if (layout.stacked) 36f else 54f)
        views.roundTile(R.id.given_tile, R.id.given_background, splitRadius)
        views.roundTile(R.id.received_tile, R.id.received_background, splitRadius)
        views.setTextViewText(R.id.given_label, context.getString(R.string.label_given))
        views.setTextViewText(R.id.received_label, context.getString(R.string.label_received))
        val splitLabels = !layout.stacked || (width >= 160f && height >= 220f + 36f * context.resources.configuration.fontScale)
        listOf(R.id.given_label, R.id.received_label).forEach {
            views.setViewVisibility(it, if (splitLabels) View.VISIBLE else View.GONE)
        }
        views.tint(R.id.given_icon) { onPrimaryContainer }
        views.tint(R.id.received_icon) { onErrorContainer }
        views.setTextViewText(R.id.given_amount, amount(balance.given))
        views.setTextViewText(R.id.received_amount, amount(balance.received))
        val shareWidth = if (layout.stacked) width - 64f else (width - 30f) / 2f - 20f
        views.fitLegacyAmount(R.id.given_amount, balance.given, shareWidth, 18f)
        views.fitLegacyAmount(R.id.received_amount, balance.received, shareWidth, 18f)
        listOf(R.id.given_label, R.id.given_amount).forEach { views.textColor(it) { onPrimaryContainer } }
        listOf(R.id.received_label, R.id.received_amount).forEach { views.textColor(it) { onErrorContainer } }
        views.setContentDescription(android.R.id.background, "$title, ${amount(balance.total)}, " +
            context.resources.getQuantityString(R.plurals.widget_unsettled_count, balance.count, balance.count) +
            ", ${context.getString(R.string.label_given)} ${amount(balance.given)}, ${context.getString(R.string.label_received)} ${amount(balance.received)}")
        val open = when {
            isPerson && personName == null -> configureIntent(id)
            isPerson -> openIntent(id, VouchShortcut.VIEW_PERSON.action, personName)
            else -> openIntent(id)
        }
        views.setOnClickPendingIntent(android.R.id.background, open)
        fun bindAction(actionView: Int, background: Int, icon: Int, shortcut: VouchShortcut) {
            val actionWidth = (width - 36f) / 3f
            views.roundTile(actionView, background, WidgetCorners.action(actionWidth, 48f))
            views.tint(background) { primaryContainer }
            views.tint(icon) { onPrimaryContainer }
            views.setOnClickPendingIntent(actionView, openIntent(id, shortcut.action, personName))
        }
        bindAction(R.id.person_add_action, R.id.person_add_background, R.id.person_add_icon, VouchShortcut.ADD_VOUCH)
        bindAction(R.id.person_share_action, R.id.person_share_background, R.id.person_share_icon, VouchShortcut.SHARE_PERSON)
        bindAction(R.id.person_settle_action, R.id.person_settle_background, R.id.person_settle_icon, VouchShortcut.SETTLE_PERSON)
        return views
    }

    internal fun shortcuts(id: Int, width: Float, height: Float = 72f): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_shortcuts)
        val outerRadius = WidgetCorners.shortcut(width, height)
        views.cardBackground(width, height, outerRadius)
        val radius = WidgetCorners.action((width - 36f) / 2f, height - 24f)
        views.roundTile(R.id.add_action, R.id.add_background, radius)
        views.roundTile(R.id.group_action, R.id.group_background, radius)
        views.tint(R.id.add_background) { primaryContainer }
        views.tint(R.id.group_background) { tertiaryContainer }
        views.tint(R.id.add_icon) { onPrimaryContainer }
        views.tint(R.id.group_icon) { onTertiaryContainer }
        views.textColor(R.id.add_label) { onPrimaryContainer }
        views.textColor(R.id.group_label) { onTertiaryContainer }
        val labelPaint = TextPaint().apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
            typeface = Typeface.DEFAULT_BOLD
        }
        val density = context.resources.displayMetrics.density
        val lineHeight = (labelPaint.fontMetrics.descent - labelPaint.fontMetrics.ascent) / density
        val twoLines = 2f * lineHeight <= height - 40f
        fun labelWidth(text: String): Float {
            val breaks = text.indices.filter { text[it].isWhitespace() }
            if (!twoLines || breaks.isEmpty()) return labelPaint.measureText(text) / density
            return breaks.minOf { index ->
                maxOf(labelPaint.measureText(text.substring(0, index)),
                    labelPaint.measureText(text.substring(index + 1))) / density
            }
        }
        val textWidth = maxOf(labelWidth(context.getString(R.string.widget_new_vouch)),
            labelWidth(context.getString(R.string.widget_new_group)))
        val labels = lineHeight <= height - 40f && width >= maxOf(
            240f * context.resources.configuration.fontScale.coerceAtLeast(1f), 36f + 2f * (52f + textWidth))
        listOf(R.id.add_label, R.id.group_label).forEach { views.setViewVisibility(it, if (labels) View.VISIBLE else View.GONE) }
        views.setOnClickPendingIntent(R.id.add_action, openIntent(id, VouchShortcut.ADD_VOUCH.action))
        views.setOnClickPendingIntent(R.id.group_action, openIntent(id, VouchShortcut.ADD_GROUP_VOUCH.action))
        return views
    }

    private fun amount(value: Long) = CurrencyManager.formatAmountWithSymbol(value, currency, locale)

    private fun RemoteViews.fitLegacyAmount(id: Int, value: Long, widthDp: Float, maxSp: Float) {
        if (Build.VERSION.SDK_INT >= 26) return // Native TextView auto-size handles newer hosts.
        val metrics = context.resources.displayMetrics
        val paint = TextPaint().apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, maxSp, metrics)
            if (id == R.id.widget_amount) textSize = textSize.coerceAtMost(36f * metrics.density)
            typeface = Typeface.DEFAULT_BOLD
        }
        val ratio = (widthDp.coerceAtLeast(1f) * metrics.density / paint.measureText(amount(value))).coerceAtMost(1f)
        setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_PX, paint.textSize * ratio)
    }

    private fun openIntent(id: Int, action: String? = null, personName: String? = null): PendingIntent = PendingIntent.getActivity(
        context, id, Intent(context, MainActivity::class.java).apply {
            this.action = action
            putExtra(VouchShortcut.EXTRA_PERSON_NAME, personName)
            data = "vouch-widget://$id/${action ?: "balance"}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun RemoteViews.cardBackground(width: Float, height: Float, radius: Float = WidgetCorners.outer(width, height)) {
        if (Build.VERSION.SDK_INT >= 31) {
            setViewOutlinePreferredRadius(android.R.id.background, radius, TypedValue.COMPLEX_UNIT_DIP)
            // Tint the actual launch-animation background, without a second antialiased layer.
            setViewVisibility(R.id.widget_background, View.GONE)
            if (theme == UiModeManager.MODE_NIGHT_AUTO) {
                setColorStateList(android.R.id.background, "setBackgroundTintList",
                    ColorStateList.valueOf(lightPalette.surfaceContainer.toArgb()),
                    ColorStateList.valueOf(darkPalette.surfaceContainer.toArgb()))
            } else {
                setColorStateList(android.R.id.background, "setBackgroundTintList", ColorStateList.valueOf(palette.surfaceContainer.toArgb()))
            }
        } else {
            setInt(android.R.id.background, "setBackgroundResource", R.drawable.widget_outline)
            tint(R.id.widget_background) { surfaceContainer }
        }
    }

    private fun RemoteViews.roundTile(container: Int, image: Int, radius: Float) {
        if (Build.VERSION.SDK_INT >= 31) {
            // Fill and clip once, so the background and pressed state share exact corners.
            setImageViewResource(image, R.drawable.widget_fill)
            setViewOutlinePreferredRadius(container, radius, TypedValue.COMPLEX_UNIT_DIP)
            setBoolean(container, "setClipToOutline", true)
        }
    }

    private fun configureIntent(id: Int): PendingIntent = PendingIntent.getActivity(
        context, id, Intent(context, PersonWidgetConfigurationActivity::class.java)
            .setData("vouch-widget://$id/configure".toUri())
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun RemoteViews.tint(id: Int, color: ColorScheme.() -> Color) = applyColor(id, "setColorFilter", color)

    private fun RemoteViews.textColor(id: Int, color: ColorScheme.() -> Color) = applyColor(id, "setTextColor", color)

    private fun RemoteViews.applyColor(id: Int, method: String, color: ColorScheme.() -> Color) {
        if (Build.VERSION.SDK_INT >= 31 && theme == UiModeManager.MODE_NIGHT_AUTO) {
            // The launcher can reapply the correct palette while our process is stopped.
            setColorInt(id, method, lightPalette.color().toArgb(), darkPalette.color().toArgb())
        } else {
            setInt(id, method, palette.color().toArgb())
        }
    }
}
