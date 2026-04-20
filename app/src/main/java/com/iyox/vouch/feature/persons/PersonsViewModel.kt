package com.iyox.vouch.feature.persons

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.model.PersonWithTotal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonsUiState(
    val persons: List<PersonWithTotal> = emptyList(),
    val showOnlyNonZero: Boolean = true
)

class PersonsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PersonRepository.getInstance(application)

    private val _persons = MutableStateFlow<List<PersonWithTotal>>(emptyList())
    private val _showOnlyNonZero = MutableStateFlow(true)

    val uiState: StateFlow<PersonsUiState> = combine(
        _persons, _showOnlyNonZero
    ) { personsList, showOnlyNonZero ->
        PersonsUiState(
            persons = if (showOnlyNonZero) {
                personsList.filter { it.vouchCount > 0 }
            } else {
                personsList
            },
            showOnlyNonZero = showOnlyNonZero
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PersonsUiState()
    )

    init {
        loadPersons()
    }

    fun toggleShowOnlyNonZero() {
        _showOnlyNonZero.value = !_showOnlyNonZero.value
    }

    fun loadPersons() {
        viewModelScope.launch {
            _persons.value = repository.getPersonsWithTotal()
        }
    }
}