package com.iyox.vouch.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.icu.util.Currency
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iyox.vouch.R
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetSizingTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun remoteViewsFitAtMinimumAndBreakpointSizesWithLargeFonts() {
        instrumentation.runOnMainSync {
            for (fontScale in listOf(1f, 1.15f, 1.5f, 2f)) {
                val configuration = Configuration(context.resources.configuration).apply { this.fontScale = fontScale }
                val localized = context.createConfigurationContext(configuration)
                val renderer = VouchWidgetRenderer(localized, Currency.getInstance("EUR"))
                for ((width, height) in listOf(110 to 110, 180 to 160, 220 to 180, 280 to 190, 110 to 240, 180 to 350, 360 to 350)) {
                    val views = renderer.balance(123, WidgetBalance(123456789, 76543210, 1234),
                        "Alexandria with a very long name", true, width.toFloat(), height.toFloat())
                    val view = views.apply(localized, null)
                    measure(view, width, height)
                    assertVisibleTextFits(view, "${width}x$height font=$fontScale")
                    val expected = WidgetLayout.forSize(width.toFloat(), height.toFloat(), fontScale)
                    assertEquals(View.GONE, view.findViewById<View>(R.id.widget_split).visibility)
                    val totalView = renderer.balance(124, WidgetBalance(123456789, 76543210, 1234),
                        null, false, width.toFloat(), height.toFloat()).apply(localized, null)
                    measure(totalView, width, height)
                    assertVisibleTextFits(totalView, "total ${width}x$height font=$fontScale")
                    assertEquals(if (expected.split) View.VISIBLE else View.GONE, totalView.findViewById<View>(R.id.widget_split).visibility)
                }
                for (width in listOf(132, 180, 299, 300, 360)) {
                    val view = renderer.shortcuts(123, width.toFloat()).apply(localized, null)
                    measure(view, width, 72)
                    assertVisibleTextFits(view, "shortcuts $width font=$fontScale")
                    assertTrue(view.findViewById<View>(R.id.add_action).height >= dp(48))
                    assertTrue(view.findViewById<View>(R.id.group_action).width >= dp(48))
                }
            }
        }
    }

    @Test fun actualHostRespondsToResizeAndProviderDeclaresValidConstraints() {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("appwidget grantbind --package ${context.packageName} --user 0")
        ).use { it.readBytes() }
        val providers = AppWidgetManager.getInstance(context).installedProviders.filter { it.provider.packageName == context.packageName }
        assertEquals(3, providers.size)
        for (provider in providers) {
            assertTrue(provider.minResizeWidth > 0)
            if (provider.provider.className == ShortcutsWidgetProvider::class.java.name) {
                assertEquals(android.appwidget.AppWidgetProviderInfo.RESIZE_HORIZONTAL, provider.resizeMode)
            } else {
                assertEquals(android.appwidget.AppWidgetProviderInfo.RESIZE_BOTH, provider.resizeMode)
            }
        }
        ActivityScenario.launch<WidgetHostTestActivity>(Intent(context, WidgetHostTestActivity::class.java)).use { scenario ->
            for ((width, height) in listOf(110f to 110f, 280f to 190f, 110f to 260f, 360f to 110f)) {
                scenario.onActivity { it.resize(width, height) }
                runBlocking { context.widgetUpdater().updateAll() }
                instrumentation.waitForIdleSync()
                val deadline = System.currentTimeMillis() + 5000
                var matches = false
                while (!matches && System.currentTimeMillis() < deadline) {
                    scenario.onActivity {
                        val split = it.widgetView.findViewById<View>(R.id.widget_split)
                        matches = split != null && (split.visibility == View.VISIBLE) == WidgetLayout.forSize(width, height, context.resources.configuration.fontScale).split
                    }
                    if (!matches) Thread.sleep(50)
                }
                assertTrue("Host did not adapt to ${width}x$height", matches)
                scenario.onActivity { assertVisibleTextFits(it.widgetView, "host ${width}x$height") }
            }
        }
    }

    private fun measure(view: View, width: Int, height: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(dp(width), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(height), View.MeasureSpec.EXACTLY))
        view.layout(0, 0, dp(width), dp(height))
    }

    private fun assertVisibleTextFits(view: View, case: String) {
        if (view.visibility != View.VISIBLE) return
        if (view is TextView && view.text.isNotEmpty()) {
            val layout = view.layout ?: error("No text layout: $case")
            assertTrue("Text vertically clipped: ${view.text} ($case)", layout.height <= view.height - view.paddingTop - view.paddingBottom)
            assertTrue("Text view has no width: $case", view.width > 0)
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            val child = view.getChildAt(index)
            if (child.visibility == View.VISIBLE && child is TextView && child.text.isNotEmpty()) {
                assertTrue("Text exceeds parent: ${child.text} ($case)", child.bottom <= view.height - view.paddingBottom)
            }
            assertVisibleTextFits(child, case)
        }
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
