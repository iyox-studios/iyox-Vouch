package com.iyox.vouch.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.model.Person
import com.iyox.vouch.model.Vouch
import com.iyox.vouch.model.VouchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PersonsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VouchRepository.getInstance(application)
    private val _vouches = MutableStateFlow<List<Vouch>>(emptyList())
    val vouches: StateFlow<List<Vouch>> = _vouches
    val persons: StateFlow<List<Person>> = _vouches
        .map { vouches ->
            vouches
                .filter { !it.person.isNullOrBlank() && !it.settled }
                .groupBy { it.person!! }
                .map { (personName, personVouches) ->
                    val totalAmount = personVouches.sumOf { vouch ->
                        if (vouch.given) {
                            vouch.amount
                        } else {
                            -vouch.amount
                        }
                    }
                    Person(
                        TODO(), TODO(), TODO()
                    )
                }
                //.sortedByDescending { it.totalAmount }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

init {
    loadVouches()
}

fun loadVouches() {
    viewModelScope.launch {
        _vouches.value = repository.getAll()
    }
}
}
