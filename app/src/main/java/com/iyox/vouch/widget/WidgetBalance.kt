package com.iyox.vouch.widget

import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.isSettled

/** Amounts use the same unsettled, non-self shares as the in-app balance hero. */
internal data class WidgetBalance(val given: Long = 0, val received: Long = 0, val count: Int = 0) {
    val total: Long get() = given - received

    companion object {
        fun from(vouches: List<VouchWithPerson>, personId: Long? = null): WidgetBalance {
            var given = 0L
            var received = 0L
            var count = 0
            for (entry in vouches) {
                val shares = entry.persons.filter {
                    !it.settled && !it.isSelf && (personId == null || it.person.id == personId)
                }
                if (if (personId == null) !entry.isSettled() else shares.isNotEmpty()) count++
                val amount = shares.sumOf { it.amount }
                if (entry.vouch.given) given += amount else received += amount
            }
            return WidgetBalance(given, received, count)
        }
    }
}

/** Breakpoints describe usable content dp, after the launcher's own widget padding. */
internal data class WidgetLayout(val split: Boolean, val stacked: Boolean, val count: Boolean) {
    companion object {
        fun forSize(width: Float, height: Float, fontScale: Float): WidgetLayout {
            val scale = fontScale.coerceAtLeast(1f)
            // Padding, icons and the amount's auto-sized line have fixed dp bounds;
            // only the labels grow with font scale. Scaling the entire card would
            // hide the split unnecessarily at common settings such as 115% text.
            val wide = width >= 120f + 100f * scale
            val stacked = !wide && height >= 160f + 20f * scale
            return WidgetLayout(
                split = (wide && height >= 144f + 48f * scale) || stacked,
                stacked = stacked,
                count = height >= (if (stacked) 214f else 100f) + 24f * scale
            )
        }
    }
}
