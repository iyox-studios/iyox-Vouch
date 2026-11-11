package com.iyox.vouch.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.FrameLayout

/** Debug-only real widget host for exercising exact sizes on a device/emulator. */
class WidgetHostTestActivity : Activity() {
    lateinit var widgetView: AppWidgetHostView
        private set
    private lateinit var host: AppWidgetHost
    var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        host = AppWidgetHost(this, 83142)
        val manager = AppWidgetManager.getInstance(this)
        widgetId = host.allocateAppWidgetId()
        val provider = when (intent.getStringExtra("kind")) {
            "shortcuts" -> ShortcutsWidgetProvider::class.java
            "person" -> PersonWidgetProvider::class.java
            else -> BalanceWidgetProvider::class.java
        }
        check(manager.bindAppWidgetIdIfAllowed(widgetId, ComponentName(this, provider))) {
            "Grant the debug app widget binding permission with adb shell appwidget grantbind --package com.iyox.vouch --user 0"
        }
        widgetView = host.createView(this, widgetId, manager.getAppWidgetInfo(widgetId))
        val background = FrameLayout(this).apply { setBackgroundColor(Color.rgb(44, 42, 49)) }
        background.addView(widgetView)
        setContentView(background)
        host.startListening()
        resize(intent.getFloatExtra("width", 280f), intent.getFloatExtra("height", 190f))
    }

    fun resize(width: Float, height: Float) {
        val density = resources.displayMetrics.density
        val horizontalPadding = widgetView.paddingLeft + widgetView.paddingRight
        val verticalPadding = widgetView.paddingTop + widgetView.paddingBottom
        val outerWidth = width + horizontalPadding / density
        val outerHeight = height + verticalPadding / density
        widgetView.layoutParams = FrameLayout.LayoutParams(
            (width * density).toInt() + horizontalPadding,
            (height * density).toInt() + verticalPadding
        ).apply {
            gravity = android.view.Gravity.CENTER
        }
        if (Build.VERSION.SDK_INT >= 31) {
            widgetView.updateAppWidgetSize(Bundle(), listOf(SizeF(outerWidth, outerHeight)))
        } else {
            @Suppress("DEPRECATION")
            widgetView.updateAppWidgetSize(Bundle(), outerWidth.toInt(), outerHeight.toInt(), outerWidth.toInt(), outerHeight.toInt())
        }
    }

    override fun onDestroy() {
        if (::host.isInitialized) {
            host.stopListening()
            host.deleteAppWidgetId(widgetId)
        }
        super.onDestroy()
    }
}
