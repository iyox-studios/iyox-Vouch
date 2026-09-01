package com.iyox.vouch.core.utils

import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.model.VouchWithPerson

/**
 * Filter, search, and sort utilities for Vouch and Person collections.
 */
object VouchFilterUtils {

    /**
     * Filters vouches by given/received status.
     * @param showOnlyGiven 0 = All, 1 = Given, 2 = Received
     */
    fun List<VouchWithPerson>.filterByGiven(showOnlyGiven: Int): List<VouchWithPerson> {
        return when (showOnlyGiven) {
            1 -> filter { it.vouch.given }
            2 -> filter { !it.vouch.given }
            else -> this
        }
    }

    /**
     * Filters vouches matching a search query across description, person names, and formatted amounts.
     */
    @JvmName("filterVouchesBySearchQuery")
    fun List<VouchWithPerson>.filterBySearchQuery(query: String): List<VouchWithPerson> {
        if (query.isBlank()) return this
        val q = query.trim().lowercase()
        return filter { v ->
            v.vouch.description.lowercase().contains(q) ||
                    v.persons.any { it.person.name.lowercase().contains(q) } ||
                    CurrencyManager.formatAmount(v.vouch.amount).contains(q) ||
                    CurrencyManager.formatAmountWithSymbol(v.vouch.amount).lowercase().contains(q)
        }
    }

    /**
     * Sorts vouches by date, amount, or person name in descending order.
     * @param sortedBy 0 = Date (newest first), 1 = Amount (highest first), 2 = Person name
     */
    fun List<VouchWithPerson>.sortVouches(sortedBy: Int): List<VouchWithPerson> {
        return when (sortedBy) {
            0 -> sortedBy { it.vouch.timestamp }
            1 -> sortedBy { it.vouch.amount }
            2 -> sortedBy { if (it.persons.size == 1) it.persons.first().person.name else "" }
            else -> sortedBy { it.vouch.timestamp }
        }.reversed()
    }

    /**
     * Applies given/received filter, search query filter, and sorting in one cohesive pipeline.
     */
    fun List<VouchWithPerson>.filterSearchAndSort(
        showOnlyGiven: Int,
        searchQuery: String,
        sortedBy: Int
    ): List<VouchWithPerson> {
        return this
            .filterByGiven(showOnlyGiven)
            .filterBySearchQuery(searchQuery)
            .sortVouches(sortedBy)
    }

    /**
     * Filters PersonTotal items matching a search query across person name and formatted amounts.
     */
    @JvmName("filterPersonsBySearchQuery")
    fun List<PersonTotal>.filterBySearchQuery(query: String): List<PersonTotal> {
        if (query.isBlank()) return this
        val q = query.trim().lowercase()
        return filter { pt ->
            pt.person.name.lowercase().contains(q) ||
                    CurrencyManager.formatAmount(pt.amount).contains(q) ||
                    CurrencyManager.formatAmountWithSymbol(pt.amount).lowercase().contains(q)
        }
    }
}
