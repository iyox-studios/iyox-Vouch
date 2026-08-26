package com.iyox.vouch.feature.vouches

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.isSettled
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

    val uiState: StateFlow<VouchesUiState> = combine(
        _vouches, _showOnlyGiven, _sortedBy
    ) { allVouches, showOnlyGiven, sortedBy ->
        val activeVouches = allVouches.filter { !it.isSettled() }
        val settledCount = allVouches.count { it.isSettled() }

        val displayed = getDisplayedVouches(showOnlyGiven, activeVouches)
        val sorted = when (sortedBy) {
            0 -> displayed.sortedBy { it.vouch.timestamp }
            1 -> displayed.sortedBy { it.vouch.amount }
            2 -> displayed.sortedBy { if (it.persons.size == 1) it.persons.first().person.name else "" }
            else -> displayed.sortedBy { it.vouch.timestamp }
        }.reversed()

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
            vouches = sorted,
            settledCount = settledCount,
            showOnlyGiven = showOnlyGiven,
            sortedBy = sortedBy,
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

    fun toggleShowOnlyGiven() {
        _showOnlyGiven.value = (_showOnlyGiven.value + 1) % 3
    }

    fun toggleSortedBy() {
        _sortedBy.value = (_sortedBy.value + 1) % 3
    }

    fun getDisplayedVouches(
        showOnlyGiven: Int,
        activeVouches: List<VouchWithPerson>
    ): List<VouchWithPerson> {
        return activeVouches.filter {
            when (showOnlyGiven) {
                0 -> true
                1 -> it.vouch.given
                2 -> !it.vouch.given
                else -> true
            }
        }
    }
}