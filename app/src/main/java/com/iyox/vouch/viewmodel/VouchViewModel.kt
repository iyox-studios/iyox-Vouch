package com.iyox.vouch.viewmodel

import android.app.Application
import android.icu.util.Currency
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.model.Person
import com.iyox.vouch.model.PersonRepository
import com.iyox.vouch.model.PersonWithTotal
import com.iyox.vouch.model.Vouch
import com.iyox.vouch.model.VouchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class VouchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VouchRepository.getInstance(application)
    private val personRepository = PersonRepository.getInstance(application)

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount

    private val _given = MutableStateFlow(true)
    val given: StateFlow<Boolean> = _given

    private val _personName = MutableStateFlow("")
    val personName: StateFlow<String> = _personName

    private val _personSearchResults = MutableStateFlow<List<PersonWithTotal>>(emptyList())
    val personSearchResults: StateFlow<List<PersonWithTotal>> = _personSearchResults

    private val _currency = MutableStateFlow(Currency.getInstance("EUR"))
    val currency: StateFlow<Currency> = _currency

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var searchJob: Job? = null

    private var editingVouchId: Int? = null
    private var originalVouch: Vouch? = null

    fun loadVouch(id: Long?) {
        if (id == null) {
            _isLoading.value = false
            return
        }
        val intId = id.toInt()
        viewModelScope.launch {
            val vouch = repository.getById(intId)
            if (vouch != null) {
                editingVouchId = vouch.id
                originalVouch = vouch
                _amount.value = vouch.amount.toPlainString()
                _given.value = vouch.given
                _personName.value = vouch.personName ?: ""
                _description.value = vouch.description
                _currency.value = vouch.currency
            }
            _isLoading.value = false
        }
    }

    fun onAmountChange(value: String) {
        _amount.value = value
    }

    fun onGivenChange(value: Boolean) {
        _given.value = value
    }

    fun onPersonChange(value: String) {
        _personName.value = value
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (value.isNotBlank()) {
                _personSearchResults.value = personRepository.getPersonsWithTotal(value)
            } else {
                _personSearchResults.value = emptyList()
            }
        }
    }

    fun onPersonSelected(person: Person) {
        _personName.value = person.name
        _personSearchResults.value = emptyList()
        searchJob?.cancel()
    }


    fun onCurrencyChange(value: Currency) {
        _currency.value = value
    }

    fun onDescriptionChange(value: String) {
        _description.value = value
    }

    fun saveVouch() {
        viewModelScope.launch {
            val amountStr = _amount.value.trim()
            val fractionDigits = _currency.value.defaultFractionDigits

            val amountPattern = if (fractionDigits > 0) {
                Regex("^\\d+([.,]\\d{1,$fractionDigits})?$")
            } else {
                Regex("^\\d+$")
            }

            if (!amountPattern.matches(amountStr)) {
                _saveSuccess.value = false
                return@launch
            }

            val normalized = amountStr.replace(',', '.')
            val amountBigDecimal = try {
                BigDecimal(normalized)
            } catch (e: NumberFormatException) {
                _saveSuccess.value = false
                return@launch
            }

            if (editingVouchId != null) {
                val newVouch = originalVouch!!.copy(
                    amount = amountBigDecimal,
                    description = _description.value.trim(),
                    personName = if (_personName.value.isBlank()) null else _personName.value.trim(),
                    given = _given.value,
                    currency = _currency.value
                )
                repository.update(originalVouch, newVouch)
            } else {
                val vouch = Vouch(
                    id = 0,
                    amount = amountBigDecimal,
                    description = _description.value.trim(),
                    personName = if (_personName.value.isBlank()) null else _personName.value.trim(),
                    given = _given.value,
                    currency = _currency.value,
                )
                repository.insert(vouch)
            }

            _saveSuccess.value = true
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}
