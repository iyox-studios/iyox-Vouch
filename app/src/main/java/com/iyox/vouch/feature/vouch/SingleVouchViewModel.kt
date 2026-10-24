package com.iyox.vouch.feature.vouch

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VouchUiState(
    val amount: String = "",
    val given: Boolean = true,
    val personName: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false,
    val personSearchResults: List<PersonTotal> = emptyList(),
    @StringRes val amountError: Int? = null,
    @StringRes val personNameError: Int? = null
)

@HiltViewModel
class SingleVouchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VouchRepository,
    private val personRepository: PersonRepository,
    private val currencyManager: CurrencyManager
) : ViewModel() {

    private val initialVouchId: Long? = when (val raw = savedStateHandle.get<Any>("vouchId")) {
        is Long -> raw
        is Int -> raw.toLong()
        is String -> raw.toLongOrNull()
        else -> null
    }
    private val initialPersonName: String? = savedStateHandle["personName"]

    val sharedActions = SharedVouchActions(repository)

    private var searchJob: Job? = null
    private var editingVouchId: Long? = null
    private var originalVouch: VouchWithPerson? = null

    private fun resolveInitialState(): VouchUiState {
        if (initialVouchId != null) {
            val cached = repository.vouchesFlow.value.find { it.vouch.id == initialVouchId }
            if (cached != null) {
                editingVouchId = cached.vouch.id
                originalVouch = cached
                return VouchUiState(
                    amount = currencyManager.formatAmount(cached.vouch.amount),
                    given = cached.vouch.given,
                    personName = cached.persons.firstOrNull()?.person?.name ?: "",
                    description = cached.vouch.description,
                    timestamp = cached.vouch.timestamp,
                    isLoading = false
                )
            }
        }
        return VouchUiState(
            personName = initialPersonName ?: "",
            isLoading = initialVouchId != null
        )
    }

    private val _uiState = MutableStateFlow(resolveInitialState())
    val uiState: StateFlow<VouchUiState> = _uiState.asStateFlow()

    init {
        if (initialVouchId != null && originalVouch == null) {
            loadVouch(initialVouchId, initialPersonName)
        }
    }

    fun loadVouch(id: Long?, prefilledPersonName: String?) {
        searchJob?.cancel()

        if (id != null && id == editingVouchId && originalVouch != null) {
            return
        }

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

        val inMemory = repository.vouchesFlow.value.find { it.vouch.id == id }
        if (inMemory != null) {
            editingVouchId = inMemory.vouch.id
            originalVouch = inMemory
            _uiState.update { state ->
                state.copy(
                    amount = currencyManager.formatAmount(inMemory.vouch.amount),
                    given = inMemory.vouch.given,
                    personName = inMemory.persons.firstOrNull()?.person?.name ?: "",
                    description = inMemory.vouch.description,
                    timestamp = inMemory.vouch.timestamp,
                    isLoading = false
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val vouch = repository.getById(id)
            _uiState.update { state ->
                if (vouch != null) {
                    editingVouchId = vouch.vouch.id
                    originalVouch = vouch
                    state.copy(
                        amount = currencyManager.formatAmount(vouch.vouch.amount),
                        given = vouch.vouch.given,
                        personName = vouch.persons.firstOrNull()?.person?.name ?: "",
                        description = vouch.vouch.description,
                        timestamp = vouch.vouch.timestamp,
                        isLoading = false
                    )
                } else {
                    state.copy(isLoading = false)
                }
            }
        }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amount = value, amountError = null) }
    }

    fun onGivenChange(value: Boolean) {
        _uiState.update { it.copy(given = value) }
    }

    fun onPersonChange(value: String) {
        _uiState.update { it.copy(personName = value, personNameError = null) }
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
                personSearchResults = emptyList(),
                personNameError = null
            )
        }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onDateChange(timestamp: Long) {
        _uiState.update { it.copy(timestamp = timestamp) }
    }

    fun saveVouch(onSaved: (VouchWithPerson?) -> Unit = {}) {
        viewModelScope.launch {
            val saved = saveVouchInternal()
            onSaved(saved)
        }
    }

    suspend fun saveVouchDirect(): VouchWithPerson? = saveVouchInternal()

    fun validateForm(): Boolean {
        var isValid = false
        _uiState.update { state ->
            val amountLong = currencyManager.parseToMinorUnits(state.amount)
            val isAmountValid = amountLong != null && amountLong > 0
            val isPersonValid = state.personName.trim().isNotBlank()
            isValid = isAmountValid && isPersonValid
            state.copy(
                amountError = if (!isAmountValid) R.string.error_amount_required else null,
                personNameError = if (!isPersonValid) R.string.error_person_name_required else null
            )
        }
        return isValid
    }

    private suspend fun saveVouchInternal(): VouchWithPerson? {
        if (!validateForm()) {
            _uiState.update { it.copy(saveSuccess = false) }
            return null
        }

        val currentState = _uiState.value
        val amountLong = currencyManager.parseToMinorUnits(currentState.amount) ?: return null
        val personName = currentState.personName.trim()

        val savedVouch: VouchWithPerson?
        if (editingVouchId != null && originalVouch != null) {
            val updated = originalVouch!!.copy(
                vouch = originalVouch!!.vouch.copy(
                    amount = amountLong,
                    description = currentState.description.trim(),
                    given = currentState.given,
                    timestamp = currentState.timestamp
                ),
                persons = listOf(PersonWithAmount(Person(name = personName), amountLong))
            )
            repository.update(updated)
            savedVouch = updated
        } else {
            val vouch = VouchWithPerson(
                vouch = Vouch(
                    amount = amountLong,
                    description = currentState.description.trim(),
                    given = currentState.given,
                    timestamp = currentState.timestamp
                ),
                persons = listOf(PersonWithAmount(Person(name = personName), amountLong))
            )
            savedVouch = repository.insert(vouch)
        }
        _uiState.update {
            it.copy(
                saveSuccess = true,
                amountError = null,
                personNameError = null
            )
        }
        return savedVouch
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
