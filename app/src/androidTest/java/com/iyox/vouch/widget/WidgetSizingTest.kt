package com.iyox.vouch.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iyox.vouch.R
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetSizingTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @OptIn(androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi::class)
    @Test fun glanceLayoutsFitAtMinimumAndBreakpointSizesWithLargeFonts() = runBlocking {
        for (fontScale in listOf(1f, 1.15f, 1.5f, 2f)) {
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                this.fontScale = fontScale
            })
            for (kind in WidgetKind.entries) {
                val sizes = if (kind == WidgetKind.SHORTCUTS) listOf(132 to 72, 180 to 72, 360 to 72)
                    else listOf(110 to 110, 180 to 160, 220 to 180, 280 to 190, 110 to 260, 360 to 350)
                for ((width, height) in sizes) {
                    val data = WidgetData(kind, WidgetBalance(123456789, 76543210, 1234),
                        if (kind == WidgetKind.PERSON) "Alexandria with a very long name" else null)
                    val views = androidx.glance.appwidget.GlanceRemoteViews().compose(localized,
                        androidx.compose.ui.unit.DpSize(width.dp, height.dp)) { WidgetContent(data, 123) }.remoteViews
                    instrumentation.runOnMainSync {
                        val view = views.apply(localized, null)
                        measure(view, width, height)
                        assertVisibleTextFits(view, "$kind "+width+"x"+height+" font="+fontScale)
                    }
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
                        val texts = it.widgetView.widgetTexts().map { text -> text.text.toString() }
                        val split = context.getString(R.string.label_given) in texts
                        matches = texts.isNotEmpty() && split == WidgetLayout.forSize(width, height, context.resources.configuration.fontScale).split
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
