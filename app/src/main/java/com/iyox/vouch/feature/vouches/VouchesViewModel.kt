package com.iyox.vouch.feature.vouches

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VouchesUiState(
    val vouches: List<VouchWithPerson> = emptyList(),
    val showOnlyUnsettled: Boolean = true,
    val showOnlyGiven: Int = 0,
    val sortedBy: Int = 0
)

class VouchesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VouchRepository.getInstance(application)

    val sharedActions = SharedVouchActions(repository) { loadVouches() }

    private val _vouches = MutableStateFlow<List<VouchWithPerson>>(emptyList())
    private val _showOnlyUnsettled = MutableStateFlow(true)
    private val _showOnlyGiven = MutableStateFlow(0)
    private val _sortedBy = MutableStateFlow(0)

    val uiState: StateFlow<VouchesUiState> = combine(
        _vouches, _showOnlyUnsettled, _showOnlyGiven, _sortedBy
    ) { vouches, showOnlyUnsettled, showOnlyGiven, sortedBy ->
        val displayed = getDisplayedVouches(showOnlyUnsettled, showOnlyGiven, vouches)
        val sorted = when (sortedBy) {
            0 -> displayed.sortedBy { it.vouch.timestamp }
            1 -> displayed.sortedBy { it.vouch.amount }
            2 -> displayed.sortedBy { if (it.persons.size == 1) it.persons.first().person.name else "" }
            else -> displayed.sortedBy { it.vouch.timestamp }
        }.reversed()

        VouchesUiState(
            vouches = sorted,
            showOnlyUnsettled = showOnlyUnsettled,
            showOnlyGiven = showOnlyGiven,
            sortedBy = sortedBy
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VouchesUiState())


    init {
        loadVouches()
    }

    fun loadVouches() {
        viewModelScope.launch {
            _vouches.value = repository.getAll()
        }
    }


    fun toggleShowOnlyUnsettled() {
        _showOnlyUnsettled.value = !_showOnlyUnsettled.value
    }

    fun toggleShowOnlyGiven() {
        _showOnlyGiven.value = (_showOnlyGiven.value + 1) % 3
    }

    fun toggleSortedBy() {
        _sortedBy.value = (_sortedBy.value + 1) % 3
    }


    fun getDisplayedVouches(
        showOnlyUnsettled: Boolean,
        showOnlyGiven: Int,
        vouches: List<VouchWithPerson>
    ): List<VouchWithPerson> {
        return vouches.filter {
            when (showOnlyUnsettled) {
                true -> !it.vouch.settled
                else -> true
            } && when (showOnlyGiven) {
                0 -> true
                1 -> it.vouch.given
                2 -> !it.vouch.given
                else -> true
            }
        }.reversed()
    }
}