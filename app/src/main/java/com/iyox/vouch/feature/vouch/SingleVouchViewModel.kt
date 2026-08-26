package com.iyox.vouch.feature.vouch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VouchUiState(
    val amount: String = "",
    val given: Boolean = true,
    val personName: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false,
    val personSearchResults: List<PersonTotal> = emptyList()
)

class SingleVouchViewModel(
    application: Application,
    private val initialVouchId: Long? = null,
    private val initialPersonName: String? = null
) : AndroidViewModel(application) {
    private val repository = VouchRepository.getInstance(application)
    private val personRepository = PersonRepository.getInstance(application)

    val sharedActions = SharedVouchActions(repository) {}

    private var searchJob: Job? = null
    private var editingVouchId: Int? = null
    private var originalVouch: VouchWithPerson? = null

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<VouchUiState> = _uiState.asStateFlow()

    private fun createInitialState(): VouchUiState {
        if (initialVouchId != null) {
            val cached = repository.getCachedVouchById(initialVouchId.toInt())
            if (cached != null) {
                editingVouchId = cached.vouch.id
                originalVouch = cached
                return VouchUiState(
                    amount = CurrencyManager.formatAmount(cached.vouch.amount),
                    given = cached.vouch.given,
                    personName = cached.persons.firstOrNull()?.person?.name ?: "",
                    description = cached.vouch.description,
                    isLoading = false
                )
            }
            return VouchUiState(isLoading = true)
        }
        return VouchUiState(
            personName = initialPersonName ?: "",
            isLoading = false
        )
    }

    init {
        if (initialVouchId != null && originalVouch == null) {
            loadVouch(initialVouchId, initialPersonName)
        }
    }

    fun loadVouch(id: Long?, prefilledPersonName: String?) {
        searchJob?.cancel()

        if (id == null) {
            editingVouchId = null
            originalVouch = null
            _uiState.update {
                it.copy(
                    isLoading = false,
                    personName = prefilledPersonName ?: it.personName
                )
            }
            return
        }

        val cached = repository.getCachedVouchById(id.toInt())
        if (cached != null) {
            editingVouchId = cached.vouch.id
            originalVouch = cached
            _uiState.update {
                it.copy(
                    amount = CurrencyManager.formatAmount(cached.vouch.amount),
                    given = cached.vouch.given,
                    personName = cached.persons.firstOrNull()?.person?.name ?: "",
                    description = cached.vouch.description,
                    isLoading = false
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        val intId = id.toInt()
        viewModelScope.launch {
            val vouch = repository.getById(intId)
            _uiState.update { state ->
                if (vouch != null) {
                    editingVouchId = vouch.vouch.id
                    originalVouch = vouch
                    state.copy(
                        amount = CurrencyManager.formatAmount(vouch.vouch.amount),
                        given = vouch.vouch.given,
                        personName = vouch.persons.firstOrNull()?.person?.name ?: "",
                        description = vouch.vouch.description,
                        isLoading = false
                    )
                } else {
                    state.copy(isLoading = false)
                }
            }
        }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amount = value) }
    }

    fun onGivenChange(value: Boolean) {
        _uiState.update { it.copy(given = value) }
    }

    fun onPersonChange(value: String) {
        _uiState.update { it.copy(personName = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (value.isNotBlank()) {
                val results = personRepository.getPersonTotal(value)
                _uiState.update { it.copy(personSearchResults = results) }
            } else {
                _uiState.update { it.copy(personSearchResults = emptyList()) }
            }
        }
    }

    fun onPersonSelected(person: Person) {
        _uiState.update {
            it.copy(
                personName = person.name,
                personSearchResults = emptyList()
            )
        }
        searchJob?.cancel()
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    suspend fun saveVouch(): VouchWithPerson? {
        val currentState = _uiState.value
        val minorUnits = CurrencyManager.parseToMinorUnits(currentState.amount)
        if (minorUnits == null) {
            _uiState.update { it.copy(saveSuccess = false) }
            return null
        }

        var savedVouch: VouchWithPerson?
        if (editingVouchId != null) {
            val newVouch = originalVouch!!.copy(
                vouch = originalVouch!!.vouch.copy(
                    amount = minorUnits,
                    description = currentState.description.trim(),
                    given = currentState.given
                ),
                persons = listOf(
                    PersonWithAmount(
                        person = Person(name = currentState.personName.trim()),
                        amount = minorUnits
                    )
                )
            )
            repository.update(newVouch)
            savedVouch = newVouch
        } else {
            val vouch = VouchWithPerson(
                vouch = Vouch(
                    id = 0,
                    amount = minorUnits,
                    description = currentState.description.trim(),
                    given = currentState.given
                ), persons = if (currentState.personName.isBlank()) emptyList() else listOf(
                    PersonWithAmount(
                        person = Person(name = currentState.personName.trim()),
                        amount = minorUnits
                    )
                )
            )
            savedVouch = repository.insert(vouch)
        }

        _uiState.update { it.copy(saveSuccess = true) }
        return savedVouch
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    companion object {
        fun factory(
            vouchId: Long? = null,
            prefilledPersonName: String? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                SingleVouchViewModel(application, vouchId, prefilledPersonName)
            }
        }
    }
}
