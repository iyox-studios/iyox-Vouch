package com.iyox.vouch.feature.persons

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.utils.VouchFilterUtils.filterBySearchQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonsUiState(
    val persons: List<PersonTotal> = emptyList(),
    val showOnlyNonZero: Boolean = true,
    val searchQuery: String = ""
)

class PersonsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PersonRepository.getInstance(application)
    private val vouchRepository = VouchRepository.getInstance(application)

    private val _persons = MutableStateFlow<List<PersonTotal>>(emptyList())
    private val _showOnlyNonZero = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<PersonsUiState> = combine(
        _persons, _showOnlyNonZero, _searchQuery
    ) { personsList, showOnlyNonZero, searchQuery ->
        val filtered = if (showOnlyNonZero) {
            personsList.filter { it.vouchCount > 0 }
        } else {
            personsList
        }
        val searched = filtered.filterBySearchQuery(searchQuery)
        PersonsUiState(
            persons = searched,
            showOnlyNonZero = showOnlyNonZero,
            searchQuery = searchQuery
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PersonsUiState()
    )

    init {
        loadPersons()
        viewModelScope.launch {
            vouchRepository.dataUpdates.collect {
                loadPersons()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowOnlyNonZero() {
        _showOnlyNonZero.value = !_showOnlyNonZero.value
    }

    fun loadPersons() {
        viewModelScope.launch {
            _persons.value = repository.getPersonTotal()
        }
    }
}