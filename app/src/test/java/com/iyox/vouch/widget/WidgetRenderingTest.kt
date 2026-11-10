package com.iyox.vouch.widget

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.icu.util.Currency
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.iyox.vouch.R
import com.iyox.vouch.navigation.VouchShortcut
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetRenderingTest {
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

    @Test fun totalAndPersonLayoutsFitWithLargeFonts() {
        for (fontScale in listOf(1f, 1.15f, 1.5f, 2f)) {
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                this.fontScale = fontScale
            })
            val renderer = VouchWidgetRenderer(localized, Currency.getInstance("EUR"))
            for ((width, height) in listOf(110 to 110, 110 to 180, 110 to 200, 180 to 152, 180 to 160, 180 to 180,
                220 to 192, 280 to 190, 110 to 260, 180 to 350, 360 to 350)) {
                for (person in listOf(false, true)) {
                    val view = renderer.balance(123, WidgetBalance(123456789, 76543210, 1234),
                        if (person) "Alexandria with a very long name" else null, person,
                        width.toFloat(), height.toFloat()).apply(localized, null)
                    measure(view, width, height)
                    assertFits(view, "${width}x$height font=$fontScale person=$person")
                    assertEquals(if (!person && WidgetLayout.forSize(width.toFloat(), height.toFloat(), fontScale).split)
                        View.VISIBLE else View.GONE, view.findViewById<View>(R.id.widget_split).visibility)
                }
            }
        }
    }

    @Test fun personActionsOpenDistinctDestinationsWithThePersonPrefilled() {
        val renderer = VouchWidgetRenderer(context, Currency.getInstance("EUR"))
        for ((width, height) in listOf(180 to 160, 110 to 180, 110 to 260)) {
            val view = renderer.balance(456, WidgetBalance(5000, 1000, 2), "Alice", true,
                width.toFloat(), height.toFloat()).apply(context, null)
            measure(view, width, height)
            val buttons = listOf(R.id.person_add_action, R.id.person_share_action, R.id.person_settle_action)
            var previousRight = 0
            for ((button, shortcut) in buttons.zip(listOf(VouchShortcut.ADD_VOUCH, VouchShortcut.SHARE_PERSON, VouchShortcut.SETTLE_PERSON))) {
                val action = view.findViewById<View>(button)
                assertEquals(View.VISIBLE, action.visibility)
                assertEquals(View.VISIBLE, (action.parent as View).visibility)
                assertTrue(action.width > 0 && action.height >= dp(48))
                if (width >= 180) assertTrue(action.width >= dp(48))
                assertEquals(0, action.top)
                assertTrue(action.left >= previousRight)
                previousRight = action.right
                action.performClick()
                val intent = shadowOf(context as Application).nextStartedActivity
                assertEquals(shortcut.action, intent.action)
                assertEquals("Alice", intent.getStringExtra(VouchShortcut.EXTRA_PERSON_NAME))
            }
        }
    }

    @Test fun backgroundUsesOneTintedLayerAndShortcutButtonsKeepTheirTouchTargets() {
        val renderer = VouchWidgetRenderer(context, Currency.getInstance("EUR"))
        val view = renderer.shortcuts(789, 132f).apply(context, null)
        measure(view, 132, 72)
        assertNotNull(view.backgroundTintList)
        assertEquals(View.GONE, view.findViewById<ImageView>(R.id.widget_background).visibility)
        for (id in listOf(R.id.add_action, R.id.group_action)) {
            assertTrue(view.findViewById<View>(id).width >= dp(48))
            assertTrue(view.findViewById<View>(id).height >= dp(48))
        }
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

    @Test fun shortcutLabelsFitAcrossWidthsAndFontScales() {
        for (fontScale in listOf(1f, 1.15f, 1.5f, 2f)) {
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                this.fontScale = fontScale
            })
            val renderer = VouchWidgetRenderer(localized, Currency.getInstance("EUR"))
            for (width in listOf(132, 180, 240, 280, 360, 480)) {
                val view = renderer.shortcuts(987, width.toFloat()).apply(localized, null)
                measure(view, width, 72)
                assertFits(view, "shortcuts $width font=$fontScale")
            }
        }
    }

    @Test fun nestedOutlineRadiiMatchTheInsetAndChildBounds() {
        val renderer = VouchWidgetRenderer(context, Currency.getInstance("EUR"))
        val view = renderer.shortcuts(321, 132f).apply(context, null)
        measure(view, 132, 72)
        fun radius(id: Int): Float {
            val target = view.findViewById<View>(id)
            return android.graphics.Outline().also { target.outlineProvider.getOutline(target, it) }.radius
        }
        assertEquals(dp(22).toFloat(), radius(android.R.id.background), 0.1f)
        assertEquals(dp(12).toFloat(), radius(R.id.add_action), 0.1f)
        assertTrue(view.findViewById<View>(R.id.add_action).clipToOutline)
    }

    @Test fun bothBalanceWidgetsUseTheHeroCountBadgeAndPersonNameIsLarger() {
        val renderer = VouchWidgetRenderer(context, Currency.getInstance("EUR"))
        for (person in listOf(false, true)) {
            val view = renderer.balance(333, WidgetBalance(5000, 1000, 3),
                if (person) "Eltern" else null, person, 280f, 260f).apply(context, null)
            measure(view, 280, 260)
            val badge = view.findViewById<View>(R.id.widget_count_badge)
            val count = view.findViewById<TextView>(R.id.widget_count)
            assertEquals(View.VISIBLE, badge.visibility)
            assertEquals(context.resources.getQuantityString(R.plurals.vouch_count, 3, 3), count.text.toString())
            assertEquals(dp(10), count.paddingStart)
            assertEquals(dp(4), count.paddingTop)
            val background = view.findViewById<ImageView>(R.id.widget_count_background)
            assertEquals(badge.width, background.width)
            assertEquals(badge.height, background.height)
            val bitmap = android.graphics.Bitmap.createBitmap(background.width, background.height,
                android.graphics.Bitmap.Config.ARGB_8888)
            background.draw(android.graphics.Canvas(bitmap))
            assertEquals(255, android.graphics.Color.alpha(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)))
            assertEquals(0, android.graphics.Color.alpha(bitmap.getPixel(0, 0)))
            bitmap.recycle()
            assertTrue(badge.width < view.width - dp(24))
            assertFits(view, "hero badge person=$person")
            assertEquals((if (person) 16 else 12) * context.resources.displayMetrics.scaledDensity,
                view.findViewById<TextView>(R.id.widget_title).textSize, 0.1f)
        }
    }

    @Test fun badgeMovesBesideTheAmountOnlyWhenBothFit() {
        val renderer = VouchWidgetRenderer(context, Currency.getInstance("EUR"))
        for (person in listOf(false, true)) {
            for ((width, height, total, beside) in listOf(
                listOf(180, 260, 4000, 0),
                listOf(280, 260, 4000, 1),
                listOf(280, 110, 4000, 1),
                listOf(280, 260, 123456789, 0)
            )) {
                val view = renderer.balance(444, WidgetBalance(total.toLong(), 0, 3),
                    if (person) "Eltern" else null, person, width.toFloat(), height.toFloat()).apply(context, null)
                measure(view, width, height)
                val badge = view.findViewById<View>(R.id.widget_count_badge)
                assertEquals(if (beside == 1) R.id.widget_count_side else R.id.widget_count_below,
                    (badge.parent as View).id)
                assertEquals(View.VISIBLE, (badge.parent as View).visibility)
                if (beside == 1) {
                    val amount = view.findViewById<TextView>(R.id.widget_amount)
                    val side = badge.parent as View
                    assertTrue(side.left >= amount.right + dp(8))
                    val bounds = android.graphics.Rect()
                    badge.getDrawingRect(bounds)
                    (view as ViewGroup).offsetDescendantRectToMyCoords(badge, bounds)
                    assertEquals(dp(12), bounds.top)
                    assertEquals(view.width - dp(12), bounds.right)
                }
                assertFits(view, "badge $width x $height person=$person total=$total")
            }
        }
    }

    @Test fun personDecorationUsesTheHeroBottomStartPosition() {
        val renderer = VouchWidgetRenderer(context, Currency.getInstance("EUR"))
        for (person in listOf(false, true)) {
            val view = renderer.balance(555, WidgetBalance(5000, 1000, 3),
                if (person) "Eltern" else null, person, 280f, 260f).apply(context, null)
            measure(view, 280, 260)
            val pentagon = view.findViewById<ImageView>(R.id.widget_person_decoration)
            val cookie = view.findViewById<ImageView>(R.id.widget_decoration)
            assertEquals(if (person) View.VISIBLE else View.GONE, pentagon.visibility)
            assertEquals(if (person) View.GONE else View.VISIBLE, cookie.visibility)
            if (person) {
                assertEquals(dp(200), pentagon.width)
                assertEquals(dp(200), pentagon.height)
                assertEquals(-dp(65).toFloat(), pentagon.x, 0.1f)
                assertEquals(view.height - dp(200) + dp(55).toFloat(), pentagon.y, 0.1f)
                assertEquals(0.15f, pentagon.alpha, 0.001f)
            }
        }
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
