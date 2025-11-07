package com.iyox.vouch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.model.VouchData
import com.iyox.vouch.model.VouchRepository;
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val vouchRepository = VouchRepository()
    private val _vouchData = MutableLiveData<VouchData>()
    val vouchData: LiveData<VouchData> = _vouchData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun fetchVouchData() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            val result = vouchRepository.fetchUserData()
            _vouchData.postValue(result)
            _isLoading.postValue(false)
        }
    }
}

