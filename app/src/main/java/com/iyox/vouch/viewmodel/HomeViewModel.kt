package com.iyox.vouch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.model.VouchDataEntity
import com.iyox.vouch.model.VouchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VouchRepository.getInstance(application)

    private val _vouches = MutableStateFlow<List<VouchDataEntity>>(emptyList())
    private val _showSettled = MutableStateFlow(false)

    val vouches: StateFlow<List<VouchDataEntity>> = _vouches
    val showSettled: StateFlow<Boolean> = _showSettled



    init {
        loadVouches()
    }

    fun loadVouches() {
        viewModelScope.launch {
            _vouches.value = repository.getAll()
        }
    }

    fun deleteVouch(vouch: VouchDataEntity) {
        viewModelScope.launch {
            repository.delete(vouch)
            loadVouches()
        }
    }

    fun settleVouch(vouch: VouchDataEntity) {
        viewModelScope.launch {
            repository.update(vouch.copy(settled = true))
            loadVouches()
        }
    }

    fun toggleShowSettled() {
        _showSettled.value = !_showSettled.value
    }
}

