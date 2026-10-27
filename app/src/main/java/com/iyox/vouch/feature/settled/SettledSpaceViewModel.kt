package com.iyox.vouch.feature.settled

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.VouchSortOrder
import com.iyox.vouch.core.model.VouchTypeFilter
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.isSettled
import com.iyox.vouch.core.model.isSettledForPerson
import com.iyox.vouch.core.utils.VouchFilterUtils.filterSearchAndSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SettledSpaceUiState(
    val vouches: List<VouchWithPerson> = emptyList(),
    val selectedPersonName: String? = null,
    val showOnlyGiven: VouchTypeFilter = VouchTypeFilter.ALL,
    val sortedBy: VouchSortOrder = VouchSortOrder.DATE,
    val searchQuery: String = "",
    val totalSettledGiven: Long = 0L,
    val totalSettledReceived: Long = 0L,
    val totalSettledVolume: Long = 0L,
    val settledCount: Int = 0
)

@HiltViewModel
class SettledSpaceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: VouchRepository
) : ViewModel() {

    private val initialPersonName: String? = savedStateHandle["personName"]

    val sharedActions = SharedVouchActions(repository)

    fun loadVouches() {
        // No-op: ViewModel reactively observes repository.vouchesFlow
    }

    private val _selectedPersonName = MutableStateFlow(initialPersonName)
    private val _showOnlyGiven = MutableStateFlow(VouchTypeFilter.ALL)
    private val _sortedBy = MutableStateFlow(VouchSortOrder.DATE)
    private val _searchQuery = MutableStateFlow("")

    private fun computeState(
        allVouches: List<VouchWithPerson>,
        selectedPerson: String?,
        showOnlyGiven: VouchTypeFilter,
        sortedBy: VouchSortOrder,
        searchQuery: String
    ): SettledSpaceUiState {
        val settledVouches = allVouches.filter { vouch ->
            if (selectedPerson != null) {
                vouch.isSettledForPerson(selectedPerson)
            } else {
                vouch.isSettled()
            }
        }

        val displayed = settledVouches.filterSearchAndSort(
            typeFilter = showOnlyGiven,
            searchQuery = searchQuery,
            sortOrder = sortedBy
        )

        var totalGiven = 0L
        var totalReceived = 0L

        for (vouch in settledVouches) {
            val relevantPersons = if (selectedPerson != null) {
                vouch.persons.filter { it.person.name == selectedPerson && it.settled && !it.isSelf }
            } else {
                vouch.persons.filter { it.settled && !it.isSelf }
            }

            for (person in relevantPersons) {
                if (vouch.vouch.given) {
                    totalGiven += person.amount
                } else {
                    totalReceived += person.amount
                }
            }
        }

        return SettledSpaceUiState(
            vouches = displayed,
            selectedPersonName = selectedPerson,
            showOnlyGiven = showOnlyGiven,
            sortedBy = sortedBy,
            searchQuery = searchQuery,
            totalSettledGiven = totalGiven,
            totalSettledReceived = totalReceived,
            totalSettledVolume = totalGiven + totalReceived,
            settledCount = settledVouches.size
        )
    }

    val uiState: StateFlow<SettledSpaceUiState> = combine(
        repository.vouchesFlow, _selectedPersonName, _showOnlyGiven, _sortedBy, _searchQuery
    ) { allVouches, selectedPerson, showOnlyGiven, sortedBy, searchQuery ->
        computeState(allVouches, selectedPerson, showOnlyGiven, sortedBy, searchQuery)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = computeState(
            repository.vouchesFlow.value,
            _selectedPersonName.value,
            _showOnlyGiven.value,
            _sortedBy.value,
            _searchQuery.value
        )
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedPerson(personName: String?) {
        _selectedPersonName.value = personName
    }

    fun toggleShowOnlyGiven() {
        _showOnlyGiven.value = _showOnlyGiven.value.next()
    }

    fun toggleSortedBy() {
        _sortedBy.value = _sortedBy.value.next(supportsPerson = true)
    }
}
