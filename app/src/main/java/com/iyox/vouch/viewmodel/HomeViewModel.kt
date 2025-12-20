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
    val vouches: StateFlow<List<VouchDataEntity>> = _vouches

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
}

