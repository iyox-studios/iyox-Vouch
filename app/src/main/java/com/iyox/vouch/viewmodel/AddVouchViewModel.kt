package com.iyox.vouch.viewmodel

import android.app.Application
import android.icu.util.Currency
import android.icu.util.CurrencyAmount
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.model.VouchDataEntity
import com.iyox.vouch.model.VouchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class AddVouchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VouchRepository.getInstance(application)

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount

    private val _given = MutableStateFlow(true)
    val given: StateFlow<Boolean> = _given

    private val _person = MutableStateFlow("")
    val person: StateFlow<String> = _person

    private val _currency = MutableStateFlow(Currency.getInstance("EUR"))
    val currency: StateFlow<Currency> = _currency

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    fun onAmountChange(value: String) {
        _amount.value = value
    }

    fun onGivenChange(value: Boolean) {
        _given.value = value
    }

    fun onPersonChange(value: String) {
        _person.value = value
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
            val vouch = VouchDataEntity(
                id = System.currentTimeMillis().toInt(),
                amount = amountBigDecimal,
                description = _description.value,
                person = _person.value,
                given = _given.value,
                currency = _currency.value,
            )
            repository.insert(vouch)
            _saveSuccess.value = true
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}

