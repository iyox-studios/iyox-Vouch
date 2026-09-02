package com.iyox.vouch.feature.persons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.utils.VouchFilterUtils.filterBySearchQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PersonsUiState(
    val persons: List<PersonTotal> = emptyList(),
    val showOnlyNonZero: Boolean = true,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class PersonsViewModel @Inject constructor(
    repository: PersonRepository
) : ViewModel() {

    private val _showOnlyNonZero = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")

    private fun computeState(
        personsList: List<PersonTotal>,
        showOnlyNonZero: Boolean,
        searchQuery: String,
        isLoading: Boolean = false
    ): PersonsUiState {
        val filtered = if (showOnlyNonZero) {
            personsList.filter { it.vouchCount > 0 }
        } else {
            personsList
        }
        val searched = filtered.filterBySearchQuery(searchQuery)
        return PersonsUiState(
            persons = searched,
            showOnlyNonZero = showOnlyNonZero,
            searchQuery = searchQuery,
            isLoading = isLoading
        )
    }

    val uiState: StateFlow<PersonsUiState> = combine(
        repository.getPersonsTotalFlow(), _showOnlyNonZero, _searchQuery
    ) { personsList, showOnlyNonZero, searchQuery ->
        if (personsList == null) {
            PersonsUiState(
                persons = emptyList(),
                showOnlyNonZero = showOnlyNonZero,
                searchQuery = searchQuery,
                isLoading = true
            )
        } else {
            computeState(personsList, showOnlyNonZero, searchQuery, isLoading = false)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = repository.personsTotalFlow.value?.let {
            computeState(it, _showOnlyNonZero.value, _searchQuery.value, isLoading = false)
        } ?: PersonsUiState(
            persons = emptyList(),
            showOnlyNonZero = _showOnlyNonZero.value,
            searchQuery = _searchQuery.value,
            isLoading = true
        )
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowOnlyNonZero() {
        _showOnlyNonZero.value = !_showOnlyNonZero.value
    }
}