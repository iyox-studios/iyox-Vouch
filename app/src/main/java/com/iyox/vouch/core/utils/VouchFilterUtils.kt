package com.iyox.vouch.core.utils

import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.model.VouchSortOrder
import com.iyox.vouch.core.model.VouchTypeFilter
import com.iyox.vouch.core.model.VouchWithPerson
import kotlin.math.abs

/**
 * Filter, search, and sort utilities for Vouch and Person collections.
 */
object VouchFilterUtils {

    /**
     * Filters vouches by given/received status using [VouchTypeFilter].
     */
    fun List<VouchWithPerson>.filterByGiven(filter: VouchTypeFilter): List<VouchWithPerson> {
        return when (filter) {
            VouchTypeFilter.GIVEN -> filter { it.vouch.given }
            VouchTypeFilter.RECEIVED -> filter { !it.vouch.given }
            VouchTypeFilter.ALL -> this
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
     * Sorts vouches by date, amount, or person name in descending order using [VouchSortOrder].
     */
    fun List<VouchWithPerson>.sortVouches(sortOrder: VouchSortOrder): List<VouchWithPerson> {
        return when (sortOrder) {
            VouchSortOrder.DATE -> sortedByDescending { it.vouch.timestamp }
            VouchSortOrder.AMOUNT -> sortedByDescending { it.vouch.amount }
            VouchSortOrder.PERSON -> sortedByDescending { if (it.persons.size == 1) it.persons.first().person.name else "" }
        }
    }

    /**
     * Applies given/received filter, search query filter, and sorting in one cohesive pipeline.
     */
    fun List<VouchWithPerson>.filterSearchAndSort(
        typeFilter: VouchTypeFilter,
        searchQuery: String,
        sortOrder: VouchSortOrder
    ): List<VouchWithPerson> {
        return this
            .filterByGiven(typeFilter)
            .filterBySearchQuery(searchQuery)
            .sortVouches(sortOrder)
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
                    CurrencyManager.formatAmountWithSymbol(pt.amount).lowercase().contains(q) ||
                    CurrencyManager.formatAmount(abs(pt.amount)).contains(q) ||
                    CurrencyManager.formatAmountWithSymbol(abs(pt.amount)).lowercase().contains(q)
        }
    }
}
