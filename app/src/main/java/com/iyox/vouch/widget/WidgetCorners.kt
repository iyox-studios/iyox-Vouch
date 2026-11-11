package com.iyox.vouch.widget

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import com.iyox.vouch.ui.theme.AppShapes

/** Buttons use the app's medium shape; the inset does not reduce their radius. */
internal object WidgetCorners {
    const val CARD = 20f
    const val SHORTCUT_CARD = 22f
    val ACTION = AppShapes.medium.topStart.toPx(Size.Zero, Density(1f))

    fun outer(width: Float, height: Float): Float = fit(CARD, width, height)

    fun shortcut(width: Float, height: Float): Float = fit(SHORTCUT_CARD, width, height)

    // Compose scales equal corners down only when their sum exceeds a side.
    fun action(width: Float, height: Float): Float = fit(ACTION, width, height)

    // VouchGivenReceivedSplitRow uses a 14 dp outer corner on its amount tiles.
    fun split(width: Float, height: Float): Float = fit(14f, width, height)

    private fun fit(radius: Float, width: Float, height: Float): Float =
        minOf(radius, width.coerceAtLeast(0f) / 2f, height.coerceAtLeast(0f) / 2f)
}
