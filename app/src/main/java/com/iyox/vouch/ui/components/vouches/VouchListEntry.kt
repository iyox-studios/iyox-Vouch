package com.iyox.vouch.ui.components.vouches

import com.iyox.vouch.core.model.VouchWithPerson
import java.util.Calendar
import java.util.TimeZone

internal sealed interface VouchListEntry {
    val key: String

    data class Month(
        val monthIndex: Int,
        val timestamp: Long,
        val totalAmount: Long
    ) : VouchListEntry {
        override val key: String = "month:$monthIndex"
    }

    data class VouchItem(val vouch: VouchWithPerson) : VouchListEntry {
        override val key: String = "vouch:${vouch.vouch.id}"
    }
}

internal fun buildVouchListEntries(
    vouches: List<VouchWithPerson>,
    groupByMonth: Boolean,
    timeZone: TimeZone = TimeZone.getDefault()
): List<VouchListEntry> {
    if (!groupByMonth) return vouches.map { VouchListEntry.VouchItem(it) }

    val calendar = Calendar.getInstance(timeZone)
    val months = vouches.groupBy {
        calendar.timeInMillis = it.vouch.timestamp
        calendar[Calendar.YEAR] * 12 + calendar[Calendar.MONTH]
    }
    return buildList {
        for ((monthIndex, items) in months.toSortedMap(reverseOrder())) {
            add(VouchListEntry.Month(
                monthIndex = monthIndex,
                timestamp = items.first().vouch.timestamp,
                totalAmount = items.sumOf { item ->
                    val outstanding = item.persons
                        .filter { !it.settled && !it.isSelf }
                        .sumOf { it.amount }
                    if (item.vouch.given) outstanding else -outstanding
                }
            ))
            items.forEach { add(VouchListEntry.VouchItem(it)) }
        }
    }
}
