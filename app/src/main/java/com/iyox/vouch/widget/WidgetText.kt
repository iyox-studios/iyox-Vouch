package com.iyox.vouch.widget

import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.AndroidRemoteViews
import com.iyox.vouch.R

internal enum class WidgetTextStyle(
    val size: Float,
    val weight: Int = Typeface.NORMAL,
    val family: String? = null,
    val lines: Int = 1
) {
    TITLE(12f, Typeface.BOLD),
    PERSON_TITLE(16f, Typeface.BOLD),
    AMOUNT(36f, Typeface.BOLD),
    COUNT(12f),
    SPLIT_LABEL(14f, family = "sans-serif-medium"),
    SPLIT_AMOUNT(18f, Typeface.BOLD),
    STACKED_AMOUNT(18f, Typeface.BOLD),
    SHORTCUT_LABEL(12f, Typeface.BOLD, lines = 2)
}

/** Small typography interop: Glance owns placement, while this retains the exact TextView metrics. */
@Composable
internal fun WidgetText(
    text: String,
    appearance: WidgetAppearance,
    modifier: GlanceModifier = GlanceModifier,
    style: WidgetTextStyle = WidgetTextStyle.TITLE,
    size: Float = style.size,
    fixedHeight: Float? = null,
    color: ColorScheme.() -> Color = { onSurface }
) {
    val styled = SpannableString(text).apply {
        if (style.weight != Typeface.NORMAL) setSpan(StyleSpan(style.weight), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        style.family?.let { setSpan(TypefaceSpan(it), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
    }
    val views = RemoteViews(appearance.context.packageName,
        when (style) {
            WidgetTextStyle.COUNT -> R.layout.widget_badge_text
            WidgetTextStyle.STACKED_AMOUNT -> R.layout.widget_centered_text
            else -> R.layout.widget_text
        }).apply {
        setTextViewText(R.id.widget_text, styled)
        setTextViewTextSize(R.id.widget_text, TypedValue.COMPLEX_UNIT_SP, size)
        setBoolean(R.id.widget_text, "setSingleLine", style.lines == 1)
        setInt(R.id.widget_text, "setMaxLines", style.lines)
        fixedHeight?.let {
            setInt(R.id.widget_text, "setHeight", (it * appearance.context.resources.displayMetrics.density).toInt())
        }
    }
    appearance.textColor(views, color)
    AndroidRemoteViews(views, modifier)
}

internal fun WidgetAppearance.textHeight(size: Float): Float {
    val paint = TextPaint().apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = spToDp(size) * context.resources.displayMetrics.density
    }
    return paint.fontMetricsInt.run { descent - ascent } / context.resources.displayMetrics.density
}

/** Same integer-sp fitting bounds as the previous platform TextView auto-size configuration. */
internal fun WidgetAppearance.amountSize(
    text: String,
    width: Float,
    height: Float,
    max: Int,
    min: Int
): Float {
    val metrics = context.resources.displayMetrics
    val paint = TextPaint().apply { typeface = Typeface.DEFAULT_BOLD }
    if (Build.VERSION.SDK_INT < 26) {
        paint.textSize = spToDp(if (max == 36) 32f else max.toFloat())
            .coerceAtMost(if (max == 36) 36f else Float.MAX_VALUE) * metrics.density
        return paint.textSize / spToDp(1f) / metrics.density *
            minOf(1f, width.coerceAtLeast(1f) * metrics.density / paint.measureText(text),
                height * metrics.density / paint.fontMetricsInt.run { descent - ascent })
    }
    for (size in max downTo min) {
        paint.textSize = spToDp(size.toFloat()) * metrics.density
        if (paint.measureText(text) <= width * metrics.density &&
            paint.fontMetricsInt.run { descent - ascent } <= height * metrics.density) return size.toFloat()
    }
    return min.toFloat()
}
