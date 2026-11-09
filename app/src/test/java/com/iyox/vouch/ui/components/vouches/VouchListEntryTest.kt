package com.iyox.vouch.ui.components.vouches

import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.VouchSortOrder
import com.iyox.vouch.core.utils.VouchFilterUtils.sortVouches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class VouchListEntryTest {
    private val utc = TimeZone.getTimeZone("UTC")

    private fun vouch(id: Long, year: Int, month: Int, day: Int = 1): VouchWithPerson {
        val timestamp = Calendar.getInstance(utc).apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis
        return VouchWithPerson(
            Vouch(id = id, amount = id * 100, timestamp = timestamp),
            listOf(PersonWithAmount(Person(id = id, name = "Person $id"), amount = id * 100))
        )
    }

    @Test
    fun monthsAreNewestFirstAndKeepInputOrderWithinEachMonth() {
        val novemberEarly = vouch(3, 2026, 11)
        val october = vouch(2, 2026, 10)
        val novemberLate = vouch(1, 2026, 11, 20)
        val entries = buildVouchListEntries(
            listOf(novemberEarly, october, novemberLate), true, utc
        )

        assertEquals(
            listOf("month:${2026 * 12 + 10}", "vouch:3", "vouch:1", "month:${2026 * 12 + 9}", "vouch:2"),
            entries.map { it.key }
        )
        assertEquals(listOf(400L, 200L),
            entries.filterIsInstance<VouchListEntry.Month>().map { it.totalAmount })
    }

    @Test
    fun sameMonthInDifferentYearsHasSeparateSections() {
        val entries = buildVouchListEntries(
            listOf(vouch(1, 2025, 1), vouch(2, 2026, 1)), true, utc
        )

        assertEquals(listOf(2026 * 12, 2025 * 12),
            entries.filterIsInstance<VouchListEntry.Month>().map { it.monthIndex })
    }

    @Test
    fun monthBoundaryUsesProvidedTimeZone() {
        val midnightUtc = vouch(1, 2026, 1)
        val entries = buildVouchListEntries(
            listOf(midnightUtc), true, TimeZone.getTimeZone("America/Los_Angeles")
        )

        assertEquals(2025 * 12 + 11, (entries.first() as VouchListEntry.Month).monthIndex)
    }

    @Test
    fun removingLastVouchInMonthRemovesItsHeader() {
        val october = vouch(1, 2026, 10)
        val november = vouch(2, 2026, 11)
        val remaining = listOf(october, november).filterNot { it == november }
        val entries = buildVouchListEntries(remaining, true, utc)

        assertEquals(listOf("month:${2026 * 12 + 9}", "vouch:1"), entries.map { it.key })
        assertEquals(100L, (entries.first() as VouchListEntry.Month).totalAmount)
        assertTrue(buildVouchListEntries(emptyList(), true, utc).isEmpty())
    }

    @Test
    fun monthlyBalanceSubtractsReceivedAndCanBeNegative() {
        val given = vouch(1, 2026, 11)
        val received = vouch(3, 2026, 11).let { it.copy(vouch = it.vouch.copy(given = false)) }
        val entries = buildVouchListEntries(listOf(given, received), true, utc)

        assertEquals(-200L, (entries.first() as VouchListEntry.Month).totalAmount)
    }

    @Test
    fun monthlyBalanceExcludesSettledAndSelfShares() {
        val group = vouch(1, 2026, 11).copy(persons = listOf(
            PersonWithAmount(Person(id = 1, name = "Open"), amount = 250L),
            PersonWithAmount(Person(id = 2, name = "Settled"), amount = 300L, settled = true),
            PersonWithAmount(Person(id = 3, name = "Me"), amount = 450L, isSelf = true)
        ))
        val entries = buildVouchListEntries(listOf(group), true, utc)

        assertEquals(250L, (entries.first() as VouchListEntry.Month).totalAmount)
    }

    @Test
    fun monthlyBalanceCanBeZero() {
        val given = vouch(1, 2026, 11)
        val received = given.copy(vouch = given.vouch.copy(id = 2, given = false))
        val entries = buildVouchListEntries(listOf(given, received), true, utc)

        assertEquals(0L, (entries.first() as VouchListEntry.Month).totalAmount)
    }

    @Test
    fun amountSortingIsGlobalWithoutMonthHeaders() {
        val newerSmall = vouch(1, 2026, 11)
        val olderLarge = vouch(3, 2025, 1)
        val sorted = listOf(newerSmall, olderLarge).sortVouches(VouchSortOrder.AMOUNT)
        val entries = buildVouchListEntries(sorted, false, utc)

        assertEquals(listOf("vouch:3", "vouch:1"), entries.map { it.key })
        assertTrue(entries.none { it is VouchListEntry.Month })
    }

    @Test
    fun ungroupedListKeepsOriginalOrderWithoutHeaders() {
        val entries = buildVouchListEntries(
            listOf(vouch(1, 2025, 1), vouch(2, 2026, 1)), false, utc
        )

        assertEquals(listOf("vouch:1", "vouch:2"), entries.map { it.key })
        assertTrue(entries.none { it is VouchListEntry.Month })
    }
}
