package com.iyox.vouch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.model.PersonRepository
import com.iyox.vouch.model.PersonWithTotal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PersonsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PersonRepository.getInstance(application)

    private val _persons = MutableStateFlow<List<PersonWithTotal>>(emptyList())
    val persons: StateFlow<List<PersonWithTotal>> = _persons

    init {
        loadPersons()
    }

    fun loadPersons() {
        viewModelScope.launch {
            _persons.value = repository.getPersonsWithTotal()
        }
    }
}
