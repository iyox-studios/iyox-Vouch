package com.iyox.vouch.widget

import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBalanceTest {
    @Test fun balancesUseUnsettledSharesAndCountGroupOnlyOnce() {
        val alice = Person(1, "Alice")
        val bob = Person(2, "Bob")
        val self = Person(3, "Me")
        val vouches = listOf(
            VouchWithPerson(Vouch(1, 9000), listOf(
                PersonWithAmount(alice, 3000), PersonWithAmount(bob, 3000, settled = true), PersonWithAmount(self, 3000, isSelf = true)
            )),
            VouchWithPerson(Vouch(2, 1200, given = false), listOf(PersonWithAmount(alice, 1200))),
            VouchWithPerson(Vouch(3, 1000), listOf(PersonWithAmount(bob, 1000, settled = true)))
        )
        assertEquals(WidgetBalance(3000, 1200, 2), WidgetBalance.from(vouches))
        assertEquals(1800L, WidgetBalance.from(vouches).total)
        assertEquals(WidgetBalance(3000, 1200, 2), WidgetBalance.from(vouches, alice.id))
        assertEquals(WidgetBalance(), WidgetBalance.from(vouches, bob.id))
        assertEquals(WidgetBalance(), WidgetBalance.from(vouches, 999))
    }

    @Test fun splitRequiresEnoughRoomInBothDimensions() {
        assertFalse(WidgetLayout.forSize(110f, 110f, 1f).split)
        assertFalse(WidgetLayout.forSize(360f, 110f, 1f).split)
        assertTrue(WidgetLayout.forSize(110f, 180f, 1f).split)
        assertFalse(WidgetLayout.forSize(110f, 179f, 1f).split)
        assertTrue(WidgetLayout.forSize(220f, 192f, 1f).split)
        assertTrue(WidgetLayout.forSize(110f, 240f, 1f).stacked)
        assertTrue(WidgetLayout.forSize(110f, 239f, 1f).split)
        assertFalse(WidgetLayout.forSize(220f, 180f, 1.5f).split)
        assertFalse(WidgetLayout.forSize(110f, 110f, 1.5f).count)
    }

    @Test fun buttonCornersMatchTheAppShapeAtNormalAndCompactSizes() {
        assertEquals(20f, WidgetCorners.outer(110f, 180f), 0f)
        for (width in listOf(100f, 48f, 24.6667f, 10f)) {
            val shape = com.iyox.vouch.ui.theme.AppShapes.medium.createOutline(
                androidx.compose.ui.geometry.Size(width, 48f),
                androidx.compose.ui.unit.LayoutDirection.Ltr,
                androidx.compose.ui.unit.Density(1f)
            ) as androidx.compose.ui.graphics.Outline.Rounded
            assertEquals(shape.roundRect.topLeftCornerRadius.x, WidgetCorners.action(width, 48f), 0.001f)
        }
        assertEquals(12f, WidgetCorners.action(48f, 48f), 0f)
        assertEquals(5f, WidgetCorners.action(10f, 48f), 0f)
        assertEquals(14f, WidgetCorners.split(86f, 36f), 0f)
    }
}
