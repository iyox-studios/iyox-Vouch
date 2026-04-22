package com.iyox.vouch.feature.person

import android.app.Application
import android.icu.util.Currency
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithTotal
import com.iyox.vouch.core.model.Vouch
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
    private val personRepository = PersonRepository.getInstance(application)
    private val vouchRepository = VouchRepository.getInstance(application)

    private val _uiState = MutableStateFlow(PersonUiState())
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    val sharedActions = SharedVouchActions(vouchRepository) {
        val personName = _uiState.value.personData?.person?.name
        if (personName != null) {
            loadPerson(personName, false)
        }
    }

    fun loadPerson(name: String, shouldShowLoading: Boolean = true) {
        _uiState.update { it.copy(isLoading = shouldShowLoading) }
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


    suspend fun settlePerson(): List<Vouch> {
        val unsettledVouches = _uiState.value.vouches.filter { !it.settled }
        for (vouch in unsettledVouches) {
            vouchRepository.update(vouch.copy(settled = true))
        }
        val personName = _uiState.value.personData?.person?.name
        if (personName != null) {
            loadPerson(personName, false)
        }
        return unsettledVouches
    }

    /**
     *
     * @return (listOfJustSettledVouches, theNewNettingVouchOrNull)
     */
    suspend fun netVouches(): Pair<List<Vouch>, Vouch?> {
        val diff = _uiState.value.amountGiven.subtract(_uiState.value.amountOwed)
        val settledVouches = settlePerson()
        var newNettingVouch: Vouch? = null

        val personName = _uiState.value.personData?.person?.name
        if (diff != BigDecimal.ZERO && personName != null) {
            val netVouch = vouchRepository.insert(
                Vouch(
                    personName = personName,
                    amount = diff.abs(),
                    description = "[NETTING]",
                    given = diff > BigDecimal.ZERO,
                    currency = Currency.getInstance("EUR")
                )
            )
            newNettingVouch = netVouch
            loadPerson(personName, false)
        }
        return Pair(settledVouches, newNettingVouch)
    }

    fun undoSettlePerson(previouslySettled: List<Vouch>) {
        viewModelScope.launch {
            for (vouch in previouslySettled) {
                vouchRepository.update(vouch.copy(settled = false))
            }
            _uiState.value.personData?.person?.name?.let { loadPerson(it, false) }
        }
    }

    fun undoNetVouches(previouslySettled: List<Vouch>, nettingVouch: Vouch?) {
        viewModelScope.launch {
            if (nettingVouch != null) {
                vouchRepository.delete(nettingVouch)
            }
            for (vouch in previouslySettled) {
                vouchRepository.update(vouch.copy(settled = false))
            }
            _uiState.value.personData?.person?.name?.let { loadPerson(it, false) }
        }
    }


}
