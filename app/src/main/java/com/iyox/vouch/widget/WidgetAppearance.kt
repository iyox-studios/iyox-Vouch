package com.iyox.vouch.widget

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.LocaleManagerCompat
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.unit.ColorProvider
import com.iyox.vouch.R
import com.iyox.vouch.ui.theme.AppThemeSettings
import com.iyox.vouch.ui.theme.resolveAppColorScheme

/** Match the app locale even when an older launcher starts our process without an Activity. */
@SuppressLint("AppBundleLocaleChanges") // Both languages are bundled; language splitting is disabled.
internal fun widgetContext(context: Context): Context {
    if (Build.VERSION.SDK_INT >= 33) return context
    val locales = AppCompatDelegate.getApplicationLocales().takeUnless { it.isEmpty }
        ?: LocaleManagerCompat.getApplicationLocales(context)
    if (locales.isEmpty) return context
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()))
    return context.createConfigurationContext(configuration)
}

internal class WidgetAppearance(val context: Context) {
    private val settings = AppThemeSettings.read(context)
    private val light = resolveAppColorScheme(context, false, settings.colorTheme)
    private val dark = resolveAppColorScheme(context, true, settings.colorTheme)
    private val automatic = settings.nightMode == UiModeManager.MODE_NIGHT_AUTO
    private val palette = if (settings.isDark(context.resources.configuration)) dark else light

    fun color(value: ColorScheme.() -> Color): ColorProvider = DayNightColorProvider(
        day = (if (automatic) light else palette).value(),
        night = (if (automatic) dark else palette).value()
    )

    fun textColor(views: RemoteViews, value: ColorScheme.() -> Color) =
        applyColor(views, R.id.widget_text, "setTextColor", value)

    fun applyColor(views: RemoteViews, id: Int, method: String, value: ColorScheme.() -> Color) {
        if (automatic && Build.VERSION.SDK_INT >= 31) {
            views.setColorInt(id, method, light.value().toArgb(), dark.value().toArgb())
        } else {
            views.setInt(id, method, palette.value().toArgb())
        }
    }

    fun spToDp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, value, context.resources.displayMetrics
    ) / context.resources.displayMetrics.density
}
