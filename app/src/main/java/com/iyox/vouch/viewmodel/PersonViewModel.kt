package com.iyox.vouch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.model.Person
import com.iyox.vouch.model.PersonRepository
import com.iyox.vouch.model.PersonWithTotal
import com.iyox.vouch.model.Vouch
import com.iyox.vouch.model.VouchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class PersonViewModel(application: Application) : AndroidViewModel(application) {
    private val vouchRepository = VouchRepository.getInstance(application)
    private val personRepository = PersonRepository.getInstance(application)

    private val _personData = MutableStateFlow<PersonWithTotal?>(null)
    val personData: StateFlow<PersonWithTotal?> = _personData

    private val _vouches = MutableStateFlow<List<Vouch>>(emptyList())
    val vouches: StateFlow<List<Vouch>> = _vouches

    private val _amountGiven = MutableStateFlow(BigDecimal.ZERO)
    val amountGiven: StateFlow<BigDecimal> = _amountGiven

    private val _amountOwed = MutableStateFlow(BigDecimal.ZERO)
    val amountOwed: StateFlow<BigDecimal> = _amountOwed

    fun loadPerson(name: String) {
        viewModelScope.launch {
            val allVouches = vouchRepository.getAll()
            val personVouches =
                allVouches.filter { it.personName == name }.sortedByDescending { it.timestamp }
            _vouches.value = personVouches

            // Calculate amounts from unsettled vouches
            var given = BigDecimal.ZERO
            var owed = BigDecimal.ZERO
            for (vouch in personVouches) {
                if (!vouch.settled) {
                    if (vouch.given) {
                        given = given.add(vouch.amount)
                    } else {
                        owed = owed.add(vouch.amount)
                    }
                }
            }
            _amountGiven.value = given
            _amountOwed.value = owed

            // Load person total from db
            val pwt = personRepository.getPersonsWithTotal().find { it.person.name == name }
            if (pwt != null) {
                _personData.value = pwt
            } else if (personVouches.isNotEmpty()) {
                // fallback if not found in db from query somehow
                _personData.value = PersonWithTotal(
                    person = Person(name = name),
                    totalAmount = given.subtract(owed),
                    vouchCount = personVouches.size
                )
            }
        }
    }

    fun deleteVouch(vouch: Vouch) {
        viewModelScope.launch {
            vouchRepository.delete(vouch)
            vouch.personName?.let { loadPerson(it) }
        }
    }

    fun settleVouch(vouch: Vouch) {
        viewModelScope.launch {
            vouchRepository.update(vouch.copy(settled = true))
            vouch.personName?.let { loadPerson(it) }
        }
    }

    fun unSettleVouch(vouch: Vouch) {
        viewModelScope.launch {
            vouchRepository.update(vouch.copy(settled = false))
            vouch.personName?.let { loadPerson(it) }
        }
    }
}
