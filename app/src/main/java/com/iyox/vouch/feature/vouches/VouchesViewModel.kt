package com.iyox.vouch.feature.vouches

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.isSettled
import com.iyox.vouch.core.utils.VouchFilterUtils.filterByGiven
import com.iyox.vouch.core.utils.VouchFilterUtils.filterSearchAndSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VouchesUiState(
    val vouches: List<VouchWithPerson> = emptyList(),
    val settledCount: Int = 0,
    val showOnlyGiven: Int = 0,
    val sortedBy: Int = 0,
    val searchQuery: String = "",
    val totalGiven: Long = 0L,
    val totalOwed: Long = 0L,
    val netTotal: Long = 0L
)

class VouchesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VouchRepository.getInstance(application)

    val sharedActions = SharedVouchActions(repository) { loadVouches() }

    private val _vouches = MutableStateFlow(repository.getCachedVouches() ?: emptyList())
    private val _showOnlyGiven = MutableStateFlow(0)
    private val _sortedBy = MutableStateFlow(0)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<VouchesUiState> = combine(
        _vouches, _showOnlyGiven, _sortedBy, _searchQuery
    ) { allVouches, showOnlyGiven, sortedBy, searchQuery ->
        val activeVouches = allVouches.filter { !it.isSettled() }
        val settledCount = allVouches.count { it.isSettled() }

        val displayed = activeVouches.filterSearchAndSort(
            showOnlyGiven = showOnlyGiven,
            searchQuery = searchQuery,
            sortedBy = sortedBy
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


    init {
        loadVouches()
        viewModelScope.launch {
            repository.dataUpdates.collect {
                loadVouches()
            }
        }
    }

    fun loadVouches() {
        viewModelScope.launch {
            _vouches.value = repository.getAll()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowOnlyGiven() {
        _showOnlyGiven.value = (_showOnlyGiven.value + 1) % 3
    }

    fun toggleSortedBy() {
        _sortedBy.value = (_sortedBy.value + 1) % 3
    }

    fun getDisplayedVouches(
        showOnlyGiven: Int,
        activeVouches: List<VouchWithPerson>
    ): List<VouchWithPerson> = activeVouches.filterByGiven(showOnlyGiven)
}