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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class PersonUiState(
    val personData: PersonWithTotal? = null,
    val vouches: List<Vouch> = emptyList(),
    val amountGiven: BigDecimal = BigDecimal.ZERO,
    val amountOwed: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = true
)

class PersonViewModel(application: Application) : AndroidViewModel(application) {
    private val vouchRepository = VouchRepository.getInstance(application)
    private val personRepository = PersonRepository.getInstance(application)

    private val _uiState = MutableStateFlow(PersonUiState())
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    fun loadPerson(name: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val allVouches = vouchRepository.getAll()
            val personVouches =
                allVouches.filter { it.personName == name }.sortedByDescending { it.timestamp }

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

            // Load person total from db
            val pwtList = personRepository.getPersonsWithTotal()
            val pwt = pwtList.find { it.person.name == name }

            val updatedPersonData = pwt ?: if (personVouches.isNotEmpty()) {
                // fallback if not found in db from query somehow
                PersonWithTotal(
                    person = Person(name = name),
                    totalAmount = given.subtract(owed),
                    vouchCount = personVouches.size
                )
            } else null

            _uiState.update {
                it.copy(
                    personData = updatedPersonData,
                    vouches = personVouches,
                    amountGiven = given,
                    amountOwed = owed,
                    isLoading = false
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

    fun settlePerson() {
        viewModelScope.launch {
            val person = _uiState.value.personData?.person
            if (person != null) {
                val vouches = _uiState.value.vouches
                for (vouch in vouches) {
                    vouchRepository.update(vouch.copy(settled = true))
                }
                loadPerson(person.name)
            }
        }
    }

    fun netVouches() {
        viewModelScope.launch {
            val diff = _uiState.value.amountGiven.subtract(_uiState.value.amountOwed)
            settlePerson()
            if (diff != BigDecimal.ZERO) {
                vouchRepository.insert(
                    Vouch(
                        personName = _uiState.value.personData?.person?.name,
                        amount = diff,
                        description = "[NETTING]",
                        given = diff > BigDecimal.ZERO,
                        currency = Currency.getInstance("EUR")
                    )
                )
            }
        }
    }

}
