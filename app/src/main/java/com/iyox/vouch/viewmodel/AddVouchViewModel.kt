package com.iyox.vouch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.model.VouchDataEntity
import com.iyox.vouch.model.VouchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.sql.Timestamp

class AddVouchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VouchRepository.getInstance(application)

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    fun onAmountChange(value: String) {
        _amount.value = value
    }

    fun onDescriptionChange(value: String) {
        _description.value = value
    }

    fun saveVouch() {
        viewModelScope.launch {
            val amountValue = _amount.value.toIntOrNull() ?: 0
            val vouch = VouchDataEntity(
                id = System.currentTimeMillis().toInt(),
                amount = amountValue,
                description = _description.value,
                timestamp = Timestamp(System.currentTimeMillis())
            )
            repository.insert(vouch)
            _saveSuccess.value = true
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}

