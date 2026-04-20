package com.iyox.vouch.feature.vouch

import android.app.Application
import android.icu.util.Currency
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithTotal
import com.iyox.vouch.core.model.Vouch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class VouchUiState(
    val amount: String = "",
    val currency: Currency = Currency.getInstance("EUR"),
    val given: Boolean = true,
    val personName: String = "",
    val description: String = "",
    val isLoading: Boolean = true,
    val saveSuccess: Boolean = false,
    val personSearchResults: List<PersonWithTotal> = emptyList(),
    val error: String? = null
)

class VouchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VouchRepository.getInstance(application)
    private val personRepository = PersonRepository.getInstance(application)

    private var searchJob: Job? = null
    private var editingVouchId: Int? = null
    private var originalVouch: Vouch? = null

    private val _uiState = MutableStateFlow(VouchUiState())
    val uiState: StateFlow<VouchUiState> = _uiState.asStateFlow()

    fun loadVouch(id: Long?) {
        _uiState.update { VouchUiState(isLoading = id != null) }
        searchJob?.cancel()

        if (id == null) {
            editingVouchId = null
            originalVouch = null
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        val intId = id.toInt()
        viewModelScope.launch {
            val vouch = repository.getById(intId)
            _uiState.update { state ->
                if (vouch != null) {
                    editingVouchId = vouch.id
                    originalVouch = vouch
                    state.copy(
                        amount = vouch.amount.toPlainString(),
                        currency = vouch.currency,
                        given = vouch.given,
                        personName = vouch.personName ?: "",
                        description = vouch.description,
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
                val results = personRepository.getPersonsWithTotal(value)
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


    fun onCurrencyChange(value: Currency) {
        _uiState.update { it.copy(currency = value) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun saveVouch() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val amountStr = currentState.amount.trim()
            val fractionDigits = currentState.currency.defaultFractionDigits

            val amountPattern = if (fractionDigits > 0) {
                Regex("^\\d+([.,]\\d{1,$fractionDigits})?$")
            } else {
                Regex("^\\d+$")
            }

            if (!amountPattern.matches(amountStr)) {
                _uiState.update { it.copy(saveSuccess = false, error = "Invalid amount format") }
                return@launch
            }

            val normalized = amountStr.replace(',', '.')
            val amountBigDecimal = try {
                BigDecimal(normalized)
            } catch (e: NumberFormatException) {
                _uiState.update { it.copy(saveSuccess = false, error = "Invalid amount format") }
                return@launch
            }

            if (editingVouchId != null) {
                val newVouch = originalVouch!!.copy(
                    amount = amountBigDecimal,
                    description = currentState.description.trim(),
                    personName = if (currentState.personName.isBlank()) null else currentState.personName.trim(),
                    given = currentState.given,
                    currency = currentState.currency
                )
                repository.update(originalVouch!!, newVouch)
            } else {
                val vouch = Vouch(
                    id = 0,
                    amount = amountBigDecimal,
                    description = currentState.description.trim(),
                    personName = if (currentState.personName.isBlank()) null else currentState.personName.trim(),
                    given = currentState.given,
                    currency = currentState.currency,
                )
                repository.insert(vouch)
            }

            _uiState.update { it.copy(saveSuccess = true) }
        }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
