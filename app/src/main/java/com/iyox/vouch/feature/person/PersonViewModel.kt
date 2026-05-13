package com.iyox.vouch.feature.person

import android.app.Application
import android.icu.util.Currency
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class PersonUiState(
    val personData: PersonTotal? = null,
    val vouches: List<VouchWithPerson> = emptyList(),
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
                allVouches.filter { it.persons.first().person.name == name }
                    .sortedByDescending { it.vouch.timestamp }

            // Calculate amounts from unsettled vouches
            var given = BigDecimal.ZERO
            var owed = BigDecimal.ZERO
            for (vouch in personVouches) {
                if (!vouch.vouch.settled) {
                    if (vouch.vouch.given) {
                        given = given.add(vouch.vouch.amount)
                    } else {
                        owed = owed.add(vouch.vouch.amount)
                    }
                }
            }

            // Load person total from db
            val pwtList = personRepository.getPersonTotal()
            val pwt = pwtList.find { it.person.name == name }
            //TODO: use id instead of query and then maybe remove query altogether

            val updatedPersonData = pwt ?: if (personVouches.isNotEmpty()) {
                // fallback if not found in db from query somehow
                PersonTotal(
                    person = Person(name = name),
                    amount = given.subtract(owed),
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


    suspend fun settlePerson(): List<VouchWithPerson> {
        val unsettledVouches = _uiState.value.vouches.filter { !it.vouch.settled }
        for (vouch in unsettledVouches) {
            vouchRepository.update(vouch.copy(vouch = vouch.vouch.copy(settled = true)))
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
    suspend fun netVouches(): Pair<List<VouchWithPerson>, VouchWithPerson?> {
        val diff = _uiState.value.amountGiven.subtract(_uiState.value.amountOwed)
        val settledVouches = settlePerson()
        var newNettingVouch: VouchWithPerson? = null

        val personName = _uiState.value.personData?.person?.name
        if (diff != BigDecimal.ZERO && personName != null) {
            val netVouch = vouchRepository.insert(
                VouchWithPerson(
                    vouch = Vouch(
                        amount = diff.abs(),
                        description = "[NETTING]", // TODO: localization
                        given = diff > BigDecimal.ZERO,
                        currency = Currency.getInstance("EUR")
                    ), persons = listOf(
                        PersonWithAmount(
                            person = Person(name = personName), amount = diff.abs()
                        )
                    )
                )
            )
            newNettingVouch = netVouch
            loadPerson(personName, false)
        }
        return Pair(settledVouches, newNettingVouch)
    }

    fun undoSettlePerson(previouslySettled: List<VouchWithPerson>) {
        viewModelScope.launch {
            for (vouch in previouslySettled) {
                vouchRepository.update(vouch.copy(vouch = vouch.vouch.copy(settled = true)))
            }
            _uiState.value.personData?.person?.name?.let { loadPerson(it, false) }
        }
    }

    fun undoNetVouches(previouslySettled: List<VouchWithPerson>, nettingVouch: VouchWithPerson?) {
        viewModelScope.launch {
            if (nettingVouch != null) {
                vouchRepository.delete(nettingVouch.vouch.id)
            }
            for (vouch in previouslySettled) {
                vouchRepository.update(vouch.copy(vouch = vouch.vouch.copy(settled = true)))
            }
            _uiState.value.personData?.person?.name?.let { loadPerson(it, false) }
        }
    }


}
