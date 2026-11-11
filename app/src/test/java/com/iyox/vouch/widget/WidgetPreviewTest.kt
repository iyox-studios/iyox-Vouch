package com.iyox.vouch.widget

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.iyox.vouch.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetPreviewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
    @Test fun personDecorationMatchesTheHeroPentagon() {
        val cubics = androidx.compose.material3.MaterialShapes.Pentagon.cubics
        val path = buildString {
            append("M${cubics.first().anchor0X},${cubics.first().anchor0Y}")
            for (cubic in cubics) {
                append(" C${cubic.control0X},${cubic.control0Y} ${cubic.control1X},${cubic.control1Y} ${cubic.anchor1X},${cubic.anchor1Y}")
            }
            append(" Z")
        }
        val parser = context.resources.getXml(R.drawable.widget_pentagon)
        while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "path") {
                assertEquals(path, parser.getAttributeValue("http://schemas.android.com/apk/res/android", "pathData"))
                return
            }
        }
        fail("Missing pentagon path")
    }

    @Test fun personPickerPreviewShowsSampleBalanceAndAllThreeActions() {
        val view = android.widget.RemoteViews(context.packageName, R.layout.widget_person_preview).apply(context, null)
        measure(view, 180, 180)
        assertFits(view, "person picker preview")
        assertEquals("Alex", view.findViewById<TextView>(R.id.widget_title).text.toString())
        assertEquals(context.getString(R.string.widget_preview_balance), view.findViewById<TextView>(R.id.widget_amount).text.toString())
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.widget_person_actions).visibility)
        for (id in listOf(R.id.person_add_action, R.id.person_share_action, R.id.person_settle_action)) {
            assertEquals(View.VISIBLE, view.findViewById<View>(id).visibility)
            assertTrue(view.findViewById<View>(id).width >= dp(48))
        }
    }

    @Test fun pickerPreviewsFitTwoByTwoWithCompleteLocalizedSampleAmounts() {
        for (locale in listOf(java.util.Locale.ENGLISH, java.util.Locale.GERMAN)) {
            for (fontScale in listOf(1f, 1.15f)) {
                val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                    setLocale(locale)
                    this.fontScale = fontScale
                    uiMode = Configuration.UI_MODE_NIGHT_YES
                })
                for (width in listOf(150, 180)) {
                    for ((name, layout) in listOf("total" to R.layout.widget_balance_preview,
                        "person" to R.layout.widget_person_preview)) {
                        val view = android.widget.RemoteViews(context.packageName, layout).apply(localized, null)
                        measure(view, width, 180)
                        assertFits(view, "$name preview $locale width=$width font=$fontScale")
                        val texts = if (name == "total") listOf(R.id.widget_title, R.id.widget_amount,
                            R.id.given_label, R.id.given_amount, R.id.received_label, R.id.received_amount)
                            else listOf(R.id.widget_title, R.id.widget_amount)
                        for (id in texts) {
                            val text = view.findViewById<TextView>(id)
                            assertTrue("Missing sample text: $name $locale", text.text.isNotEmpty())
                            for (line in 0 until text.layout.lineCount) {
                                assertEquals("Truncated ${text.text}: $name $locale width=$width font=$fontScale",
                                    0, text.layout.getEllipsisCount(line))
                            }
                        }
                        if (fontScale == 1f && width == 180) savePreview(view, "$name-${locale.language}")
                    }
                }
            }
        }
    }

    private fun savePreview(view: View, name: String) {
        val bitmap = android.graphics.Bitmap.createBitmap(view.width * 3, view.height * 3,
            android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.scale(3f, 3f)
        // Software View.draw does not apply the hardware outline clipping used by launchers.
        val clip = android.graphics.Path().apply {
            addRoundRect(0f, 0f, view.width.toFloat(), view.height.toFloat(), dp(20).toFloat(),
                dp(20).toFloat(), android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(clip)
        view.draw(canvas)
        val output = java.io.File("build/reports/widget-previews/$name.png")
        output.parentFile?.mkdirs()
        output.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    private fun measure(view: View, width: Int, height: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(dp(width), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(dp(height), View.MeasureSpec.EXACTLY))
        view.layout(0, 0, dp(width), dp(height))
    }

    private fun assertFits(view: View, case: String) {
        if (view.visibility != View.VISIBLE) return
        if (view is TextView && view.text.isNotEmpty()) {
            val layout = view.layout ?: error("Missing layout: $case")
            assertTrue("Clipped text: ${view.text}, layout=${layout.height} view=${view.height} lines=${layout.lineCount} width=${view.width} ($case)", layout.height <= view.height - view.paddingTop - view.paddingBottom)
            assertTrue("No text width: $case", view.width > 0)
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            val child = view.getChildAt(index)
            if (child.visibility != View.VISIBLE || child.id == R.id.widget_decoration || child.id == R.id.widget_person_decoration) continue
            val name = if (child.id > 0) view.resources.getResourceName(child.id) else child.javaClass.simpleName
            assertTrue("Child extends below its parent: $name ($case)", child.bottom <= view.height - view.paddingBottom)
            assertFits(child, case)
        }
    }
}
