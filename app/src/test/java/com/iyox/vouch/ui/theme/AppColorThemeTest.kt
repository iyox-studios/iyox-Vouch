package com.iyox.vouch.ui.theme

import com.iyox.vouch.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppColorThemeTest {

    @Test
    fun testFromKey_validKeys() {
        assertEquals(AppColorTheme.DYNAMIC, AppColorTheme.fromKey("dynamic"))
        assertEquals(AppColorTheme.PURPLE, AppColorTheme.fromKey("purple"))
        assertEquals(AppColorTheme.BLUE, AppColorTheme.fromKey("blue"))
        assertEquals(AppColorTheme.GREEN, AppColorTheme.fromKey("green"))
        assertEquals(AppColorTheme.TEAL, AppColorTheme.fromKey("teal"))
        assertEquals(AppColorTheme.ORANGE, AppColorTheme.fromKey("orange"))
        assertEquals(AppColorTheme.ROSE, AppColorTheme.fromKey("rose"))
    }

    @Test
    fun testFromKey_invalidOrEmpty_defaultsToDynamic() {
        assertEquals(AppColorTheme.DYNAMIC, AppColorTheme.fromKey(""))
        assertEquals(AppColorTheme.DYNAMIC, AppColorTheme.fromKey("unknown"))
        assertEquals(AppColorTheme.DYNAMIC, AppColorTheme.fromKey("DARK"))
    }

    @Test
    fun testPreviewColors() {
        assertNull(AppColorTheme.DYNAMIC.previewColor)
        assertNotNull(AppColorTheme.PURPLE.previewColor)
        assertNotNull(AppColorTheme.BLUE.previewColor)
        assertNotNull(AppColorTheme.GREEN.previewColor)
        assertNotNull(AppColorTheme.TEAL.previewColor)
        assertNotNull(AppColorTheme.ORANGE.previewColor)
        assertNotNull(AppColorTheme.ROSE.previewColor)
    }

    @Test
    fun testLabelResources() {
        assertEquals(R.string.color_theme_dynamic, AppColorTheme.DYNAMIC.labelRes)
        assertEquals(R.string.color_theme_purple, AppColorTheme.PURPLE.labelRes)
        assertEquals(R.string.color_theme_blue, AppColorTheme.BLUE.labelRes)
        assertEquals(R.string.color_theme_green, AppColorTheme.GREEN.labelRes)
        assertEquals(R.string.color_theme_teal, AppColorTheme.TEAL.labelRes)
        assertEquals(R.string.color_theme_orange, AppColorTheme.ORANGE.labelRes)
        assertEquals(R.string.color_theme_rose, AppColorTheme.ROSE.labelRes)
    }
}
