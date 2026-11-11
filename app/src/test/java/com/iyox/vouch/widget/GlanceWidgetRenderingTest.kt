package com.iyox.vouch.widget

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Path
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import com.iyox.vouch.R
import com.iyox.vouch.navigation.VouchShortcut
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class GlanceWidgetRenderingTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val referenceBounds by lazy {
        org.json.JSONObject(javaClass.getResourceAsStream("/widget-goldens/bounds.json")!!
            .bufferedReader().use { it.readText() })
    }

    @Test
    fun matchesReferenceRenders() = runBlocking {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            .putString("color_theme", "purple").commit()
        for (night in listOf(false, true)) {
            val localized = localized(night)
            for (kind in WidgetKind.entries) {
                val sizes = if (kind == WidgetKind.SHORTCUTS) listOf(132 to 72, 360 to 72)
                    else listOf(110 to 110, 180 to 180, 280 to 260, 110 to 260)
                for ((width, height) in sizes) {
                    val name = "${kind.name.lowercase()}-$width-$height-${if (night) "dark" else "light"}"
                    val view = render(localized, sample(kind), width, height)
                    val bounds = org.json.JSONArray()
                    fun record(node: View) {
                        if (node.visibility != View.VISIBLE) return
                        if (node is TextView && node.text.isNotEmpty()) {
                            val rect = android.graphics.Rect()
                            node.getDrawingRect(rect)
                            (view as ViewGroup).offsetDescendantRectToMyCoords(node, rect)
                            bounds.put(org.json.JSONObject().apply {
                                put("text", node.text.toString()); put("left", rect.left); put("top", rect.top)
                                put("width", rect.width()); put("height", rect.height()); put("size", node.textSize)
                            })
                        }
                        if (node is ViewGroup) for (index in 0 until node.childCount) record(node.getChildAt(index))
                    }
                    record(view)
                    val expectedBounds = referenceBounds.getJSONArray(name)
                    assertEquals("Text count: $name", expectedBounds.length(), bounds.length())
                    for (index in 0 until bounds.length()) {
                        val expected = expectedBounds.getJSONObject(index)
                        val actual = bounds.getJSONObject(index)
                        for (key in listOf("text", "left", "top", "width", "height", "size")) {
                            if (key == "text") assertEquals("$name text[$index]", expected.getString(key), actual.getString(key))
                            else assertEquals("$name text[$index].$key", expected.getDouble(key), actual.getDouble(key), 0.01)
                        }
                    }
                    val bitmap = draw(view, kind)
                    val output = File("build/reports/glance-widgets/$name.png")
                    output.parentFile!!.mkdirs()
                    output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    val reference = BitmapFactory.decodeStream(javaClass.getResourceAsStream("/widget-goldens/$name.png"))
                    assertNotNull("Missing reference $name", reference)
                    var changed = 0
                    for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                        val actual = bitmap.getPixel(x, y)
                        val expected = reference.getPixel(x, y)
                        val difference = listOf(0, 8, 16, 24).maxOf { shift ->
                            kotlin.math.abs((actual ushr shift and 255) - (expected ushr shift and 255))
                        }
                        if (difference > 2) changed++
                    }
                    assertTrue("Visual regression in $name: $changed pixels changed", changed <= 4)
                    bitmap.recycle()
                    reference.recycle()
                }
            }
        }
    }

    @Test
    fun personActionsKeepDistinctIntentsAndTheSelectedPerson() = runBlocking {
        val view = render(localized(false), sample(WidgetKind.PERSON), 180, 180)
        val actions = listOf(
            R.string.action_add_vouch to VouchShortcut.ADD_VOUCH,
            R.string.share_vouch_summary_title to VouchShortcut.SHARE_PERSON,
            R.string.widget_settle_all to VouchShortcut.SETTLE_PERSON
        )
        val uris = mutableSetOf<String>()
        for ((label, shortcut) in actions) {
            val button = actionView(view, context.getString(label))
            assertTrue(button.performClick())
            val intent = shadowOf(context as Application).nextStartedActivity
            assertEquals(shortcut.action, intent.action)
            assertEquals("Alex", intent.getStringExtra(VouchShortcut.EXTRA_PERSON_NAME))
            assertTrue(uris.add(intent.data.toString()))
        }
    }

    @Test
    fun missingPersonOpensConfigurationForTheSameWidget() = runBlocking {
        val view = render(localized(false), sample(WidgetKind.PERSON).copy(personName = null), 110, 110)
        assertTrue(view.findViewById<View>(android.R.id.background).performClick())
        val intent = shadowOf(context as Application).nextStartedActivity
        assertEquals(PersonWidgetConfigurationActivity::class.java.name, intent.component!!.className)
        assertEquals(123, intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
    }

    @Test
    fun shortcutActionsRetainSeparateDestinations() = runBlocking {
        val view = render(localized(false), sample(WidgetKind.SHORTCUTS), 132, 72)
        for ((label, shortcut) in listOf(R.string.action_add_vouch to VouchShortcut.ADD_VOUCH,
            R.string.action_add_group_vouch to VouchShortcut.ADD_GROUP_VOUCH)) {
            assertTrue(actionView(view, context.getString(label)).performClick())
            assertEquals(shortcut.action, shadowOf(context as Application).nextStartedActivity.action)
        }
    }

    @Test
    fun textFitsAcrossSizesAndFontScales() = runBlocking {
        for (locale in listOf(Locale.ENGLISH, Locale.GERMAN)) {
            for (scale in listOf(1f, 1.15f, 1.5f, 2f)) {
                val localized = localized(false, scale, locale)
                for (kind in WidgetKind.entries) {
                    val sizes = if (kind == WidgetKind.SHORTCUTS) listOf(132 to 72, 180 to 72, 360 to 72)
                        else listOf(110 to 110, 110 to 180, 180 to 160, 220 to 192, 110 to 260, 360 to 350)
                    for ((width, height) in sizes) {
                        val data = sample(kind).copy(balance = WidgetBalance(123456789, 76543210, 1234),
                            personName = if (kind == WidgetKind.PERSON) "Alexandria with a very long name" else null)
                        val view = render(localized, data, width, height)
                        assertTextFits(view, "$kind ${width}x$height font=$scale")
                    }
                }
            }
        }
    }

    @Test
    @Config(sdk = [25])
    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    fun legacyLaunchersInflateWithoutUnsupportedRemoteCalls() = runBlocking {
        for (scale in listOf(1f, 1.5f, 2f)) {
            for (kind in WidgetKind.entries) {
                val width = if (kind == WidgetKind.SHORTCUTS) 132 else 110
                val height = if (kind == WidgetKind.SHORTCUTS) 72 else 180
                val view = render(localized(false, scale), sample(kind), width, height)
                // Legacy Robolectric graphics use synthetic font metrics. Check remote API
                // compatibility here; native graphics above verify actual text fitting.
                val texts = descendants(view).filterIsInstance<TextView>().filter { it.text.isNotEmpty() }
                if (kind != WidgetKind.SHORTCUTS) {
                    assertTrue(texts.isNotEmpty())
                    assertTrue(texts.all { it.layout.lineCount == 1 })
                }
                assertNull(view.findViewById<View>(R.id.widget_decoration))
            }
        }
    }

    private fun sample(kind: WidgetKind) = WidgetData(kind, WidgetBalance(17500, 5000, 2),
        if (kind == WidgetKind.PERSON) "Alex" else null)

    private fun descendants(view: View): List<View> = buildList {
        add(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) addAll(descendants(view.getChildAt(index)))
    }

    private fun actionView(root: View, label: String): View {
        var container: View? = descendants(root).first { it.contentDescription == label }
        // Glance puts its ripple/click overlay beside the semantic content in a wrapper.
        while (container != null) {
            descendants(container).firstOrNull { it.hasOnClickListeners() }?.let { return it }
            container = container.parent as? View
        }
        error("Missing click target for $label")
    }

    private fun localized(night: Boolean, scale: Float = 1f, locale: Locale = Locale.ENGLISH): Context =
        context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            uiMode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            fontScale = scale
            setLocale(locale)
        })

    private suspend fun render(context: Context, data: WidgetData, width: Int, height: Int): View {
        val remote = GlanceRemoteViews().compose(context, DpSize(width.dp, height.dp)) { WidgetContent(data, 123) }.remoteViews
        return remote.apply(context, null).apply {
            val density = context.resources.displayMetrics.density
            measure(View.MeasureSpec.makeMeasureSpec((width * density).toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((height * density).toInt(), View.MeasureSpec.EXACTLY))
            layout(0, 0, measuredWidth, measuredHeight)
        }
    }

    private fun draw(view: View, kind: WidgetKind): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = (if (kind == WidgetKind.SHORTCUTS) 22 else 20) * context.resources.displayMetrics.density
        canvas.clipPath(Path().apply {
            addRoundRect(0f, 0f, view.width.toFloat(), view.height.toFloat(), radius, radius, Path.Direction.CW)
        })
        view.draw(canvas)
        return bitmap
    }

    private fun assertTextFits(view: View, case: String) {
        if (view.visibility != View.VISIBLE) return
        if (view is TextView && view.text.isNotEmpty()) {
            assertTrue("Clipped ${view.text}: $case layout=${view.layout.height} height=${view.height} width=${view.width} size=${view.textSize} lines=${view.layout.lineCount}",
                view.layout.height <= view.height - view.paddingTop - view.paddingBottom)
            assertTrue("No width ${view.text}: $case", view.width > 0)
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) assertTextFits(view.getChildAt(index), case)
    }
}
