package com.iyox.vouch.feature.settled

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.isSettled
import com.iyox.vouch.core.model.isSettledForPerson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettledSpaceUiState(
    val vouches: List<VouchWithPerson> = emptyList(),
    val selectedPersonName: String? = null,
    val showOnlyGiven: Int = 0, // 0 = all, 1 = given, 2 = received
    val sortedBy: Int = 0, // 0 = date, 1 = amount, 2 = person
    val totalSettledGiven: Long = 0L,
    val totalSettledReceived: Long = 0L,
    val totalSettledVolume: Long = 0L,
    val settledCount: Int = 0
)

class SettledSpaceViewModel(
    application: Application,
    initialPersonName: String? = null
) : AndroidViewModel(application) {

    private val repository = VouchRepository.getInstance(application)

    val sharedActions = SharedVouchActions(repository) { loadVouches() }

    private val _allVouches = MutableStateFlow(repository.getCachedVouches() ?: emptyList())
    private val _selectedPersonName = MutableStateFlow(initialPersonName)
    private val _showOnlyGiven = MutableStateFlow(0)
    private val _sortedBy = MutableStateFlow(0)

    val uiState: StateFlow<SettledSpaceUiState> = combine(
        _allVouches, _selectedPersonName, _showOnlyGiven, _sortedBy
    ) { allVouches, selectedPerson, showOnlyGiven, sortedBy ->
        // Filter to settled vouches
        val settledVouches = allVouches.filter { vouch ->
            if (selectedPerson != null) {
                vouch.isSettledForPerson(selectedPerson)
            } else {
                vouch.isSettled()
            }
        }

        // Apply given/received filter
        val filtered = settledVouches.filter { vouch ->
            when (showOnlyGiven) {
                0 -> true
                1 -> vouch.vouch.given
                2 -> !vouch.vouch.given
                else -> true
            }
        }

        // Apply sorting
        val sorted = when (sortedBy) {
            0 -> filtered.sortedBy { it.vouch.timestamp }
            1 -> filtered.sortedBy { it.vouch.amount }
            2 -> filtered.sortedBy { if (it.persons.size == 1) it.persons.first().person.name else "" }
            else -> filtered.sortedBy { it.vouch.timestamp }
        }.reversed()

        var totalGiven = 0L
        var totalReceived = 0L

        for (vouch in settledVouches) {
            val relevantPersons = if (selectedPerson != null) {
                vouch.persons.filter { it.person.name == selectedPerson && it.settled }
            } else {
                vouch.persons.filter { it.settled }
            }

            for (person in relevantPersons) {
                if (vouch.vouch.given) {
                    totalGiven += person.amount
                } else {
                    totalReceived += person.amount
                }
            }
        }

        SettledSpaceUiState(
            vouches = sorted,
            selectedPersonName = selectedPerson,
            showOnlyGiven = showOnlyGiven,
            sortedBy = sortedBy,
            totalSettledGiven = totalGiven,
            totalSettledReceived = totalReceived,
            totalSettledVolume = totalGiven + totalReceived,
            settledCount = settledVouches.size
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettledSpaceUiState())

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
            _allVouches.value = repository.getAll()
        }
    }

    fun setSelectedPerson(personName: String?) {
        _selectedPersonName.value = personName
    }

    fun toggleShowOnlyGiven() {
        _showOnlyGiven.value = (_showOnlyGiven.value + 1) % 3
    }

    fun toggleSortedBy() {
        _sortedBy.value = (_sortedBy.value + 1) % 3
    }

    companion object {
        fun factory(initialPersonName: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                SettledSpaceViewModel(application, initialPersonName)
            }
        }
    }
}
