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
    private val _showOnlyUnsettled = MutableStateFlow(true)
    private val _showOnlyGiven = MutableStateFlow(0)

    val vouches: StateFlow<List<VouchDataEntity>> = _vouches
    val showOnlyUnsettled: StateFlow<Boolean> = _showOnlyUnsettled
    val showOnlyGiven: StateFlow<Int> = _showOnlyGiven


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

    fun toggleShowOnlyUnsettled() {
        _showOnlyUnsettled.value = !_showOnlyUnsettled.value
    }

    fun toggleShowOnlyGiven() {
        _showOnlyGiven.value = (_showOnlyGiven.value + 1) % 3
    }

    fun getDisplayedVouches(showOnlyUnsettled: Boolean, showOnlyGiven: Int): List<VouchDataEntity> {
        return vouches.value.filter {
            when (showOnlyUnsettled) {
                true -> !it.settled
                else -> true
            } && when (showOnlyGiven) {
                0 -> true
                1 -> it.given
                2 -> !it.given
                else -> true
            }
        }.reversed()
    }
}

