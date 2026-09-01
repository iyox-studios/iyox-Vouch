package com.iyox.vouch.feature.vouches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.VouchSortOrder
import com.iyox.vouch.core.model.VouchTypeFilter
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.isSettled
import com.iyox.vouch.core.utils.VouchFilterUtils.filterByGiven
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
    val settledCount: Int = 0,
    val showOnlyGiven: VouchTypeFilter = VouchTypeFilter.ALL,
    val sortedBy: VouchSortOrder = VouchSortOrder.DATE,
    val searchQuery: String = "",
    val totalGiven: Long = 0L,
    val totalOwed: Long = 0L,
    val netTotal: Long = 0L
)

@HiltViewModel
class VouchesViewModel @Inject constructor(
    private val repository: VouchRepository
) : ViewModel() {

    val sharedActions = SharedVouchActions(repository) {}

    private val _showOnlyGiven = MutableStateFlow(VouchTypeFilter.ALL)
    private val _sortedBy = MutableStateFlow(VouchSortOrder.DATE)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<VouchesUiState> = combine(
        repository.vouchesFlow, _showOnlyGiven, _sortedBy, _searchQuery
    ) { allVouches, showOnlyGiven, sortedBy, searchQuery ->
        val activeVouches = allVouches.filter { !it.isSettled() }
        val settledCount = allVouches.count { it.isSettled() }

        val displayed = activeVouches.filterSearchAndSort(
            typeFilter = showOnlyGiven,
            searchQuery = searchQuery,
            sortOrder = sortedBy
        )

        var totalGiven = 0L
        var totalOwed = 0L
        for (vouch in activeVouches) {
            for (personEntry in vouch.persons) {
                if (!personEntry.settled) {
                    if (vouch.vouch.given) {
                        totalGiven += personEntry.amount
                    } else {
                        totalOwed += personEntry.amount
                    }
                }
            }
        }

        VouchesUiState(
            vouches = displayed,
            settledCount = settledCount,
            showOnlyGiven = showOnlyGiven,
            sortedBy = sortedBy,
            searchQuery = searchQuery,
            totalGiven = totalGiven,
            totalOwed = totalOwed,
            netTotal = totalGiven - totalOwed
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VouchesUiState())

    fun loadVouches() {
        // No-op: repository.vouchesFlow automatically updates reactively from Room.
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowOnlyGiven() {
        _showOnlyGiven.value = _showOnlyGiven.value.next()
    }

    fun toggleSortedBy() {
        _sortedBy.value = _sortedBy.value.next(supportsPerson = true)
    }

    fun getDisplayedVouches(
        showOnlyGiven: VouchTypeFilter,
        activeVouches: List<VouchWithPerson>
    ): List<VouchWithPerson> = activeVouches.filterByGiven(showOnlyGiven)
}