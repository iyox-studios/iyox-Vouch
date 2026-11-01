package com.iyox.vouch.feature.vouches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.VouchSortOrder
import com.iyox.vouch.core.model.VouchTypeFilter
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.isSettled
import com.iyox.vouch.core.utils.VouchFilterUtils.filterSearchAndSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class VouchesUiState(
    val vouches: List<VouchWithPerson> = emptyList(),
    val totalVouchesCount: Int = 0,
    val activeVouchesCount: Int = 0,
    val settledCount: Int = 0,
    val showOnlyGiven: VouchTypeFilter = VouchTypeFilter.ALL,
    val sortedBy: VouchSortOrder = VouchSortOrder.DATE,
    val searchQuery: String = "",
    val totalGiven: Long = 0L,
    val totalOwed: Long = 0L,
    val netTotal: Long = 0L,
    val currencyCode: String = ""
)

@HiltViewModel
class VouchesViewModel @Inject constructor(
    repository: VouchRepository,
    currencyManager: CurrencyManager? = null
) : ViewModel() {

    private val currencyFlow = currencyManager?.currencyFlow ?: CurrencyManager.currencyFlow
    val sharedActions = SharedVouchActions(repository)

    private val _showOnlyGiven = MutableStateFlow(VouchTypeFilter.ALL)
    private val _sortedBy = MutableStateFlow(VouchSortOrder.DATE)
    private val _searchQuery = MutableStateFlow("")

    private fun computeState(
        allVouches: List<VouchWithPerson>,
        showOnlyGiven: VouchTypeFilter,
        sortedBy: VouchSortOrder,
        searchQuery: String,
        currencyCode: String = ""
    ): VouchesUiState {
        val activeVouches = allVouches.filter { !it.isSettled() }
        val settledCount = allVouches.count { it.isSettled() }
        val totalVouchesCount = allVouches.size
        val activeVouchesCount = activeVouches.size

        val displayed = activeVouches.filterSearchAndSort(
            typeFilter = showOnlyGiven,
            searchQuery = searchQuery,
            sortOrder = sortedBy
        )

        var totalGiven = 0L
        var totalOwed = 0L
        for ((vouch, persons) in activeVouches) {
            for ((_, amount, settled, _, isSelf) in persons) {
                if (!settled && !isSelf) {
                    if (vouch.given) {
                        totalGiven += amount
                    } else {
                        totalOwed += amount
                    }
                }
            }
        }

        return VouchesUiState(
            vouches = displayed,
            totalVouchesCount = totalVouchesCount,
            activeVouchesCount = activeVouchesCount,
            settledCount = settledCount,
            showOnlyGiven = showOnlyGiven,
            sortedBy = sortedBy,
            searchQuery = searchQuery,
            totalGiven = totalGiven,
            totalOwed = totalOwed,
            netTotal = totalGiven - totalOwed,
            currencyCode = currencyCode
        )
    }

    val uiState: StateFlow<VouchesUiState> = combine(
        repository.vouchesFlow, _showOnlyGiven, _sortedBy, _searchQuery, currencyFlow
    ) { allVouches, showOnlyGiven, sortedBy, searchQuery, currency ->
        computeState(allVouches, showOnlyGiven, sortedBy, searchQuery, currencyCode = currency.currencyCode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = computeState(
            repository.vouchesFlow.value,
            _showOnlyGiven.value,
            _sortedBy.value,
            _searchQuery.value,
            currencyCode = currencyFlow.value.currencyCode
        )
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowOnlyGiven() {
        _showOnlyGiven.value = _showOnlyGiven.value.next()
    }

    fun resetTypeFilter() {
        _showOnlyGiven.value = VouchTypeFilter.ALL
    }

    fun toggleSortedBy() {
        _sortedBy.value = _sortedBy.value.next(supportsPerson = true)
    }
}