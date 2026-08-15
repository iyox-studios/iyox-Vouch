package com.iyox.vouch.feature.person

import android.app.Application
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
import kotlin.math.abs

data class PersonUiState(
    val personData: PersonTotal? = null,
    val vouches: List<VouchWithPerson> = emptyList(),
    val amountGiven: Long = 0L,
    val amountOwed: Long = 0L,
    val isLoading: Boolean = true
)

data class PersonSettlementChange(
    val vouchId: Int,
    val personId: Int
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
            val personVouches = allVouches.mapNotNull { vouch ->
                vouch.persons.firstOrNull { it.person.name == name } ?: return@mapNotNull null
                vouch
            }.sortedByDescending { it.vouch.timestamp }

            // Calculate amounts from unsettled vouches
            var given = 0L
            var owed = 0L
            for (vouch in personVouches) {
                val personEntry = vouch.persons.firstOrNull { it.person.name == name }
                if (personEntry != null && !personEntry.settled) {
                    if (vouch.vouch.given) {
                        given += personEntry.amount
                    } else {
                        owed += personEntry.amount
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
                    amount = given - owed,
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

    suspend fun settlePerson(): List<PersonSettlementChange> {
        val personName = _uiState.value.personData?.person?.name ?: return emptyList()
        val unsettledEntries = _uiState.value.vouches.mapNotNull { vouch ->
            val personEntry = vouch.persons.firstOrNull { it.person.name == personName } //TODO
                ?: return@mapNotNull null
            if (personEntry.settled) return@mapNotNull null
            PersonSettlementChange(vouchId = vouch.vouch.id, personId = personEntry.person.id)
        }
        for (entry in unsettledEntries) {
            vouchRepository.setPersonSettlement(entry.vouchId, entry.personId, true)
        }
        loadPerson(personName, false)
        return unsettledEntries
    }

    /**
     *
     * @return (listOfJustSettledVouches, theNewNettingVouchOrNull)
     */
    suspend fun netVouches(): Pair<List<PersonSettlementChange>, VouchWithPerson?> {
        val diff = _uiState.value.amountGiven - _uiState.value.amountOwed
        val settledVouches = settlePerson()
        var newNettingVouch: VouchWithPerson? = null

        val personName = _uiState.value.personData?.person?.name
        if (diff != 0L && personName != null) {
            val netVouch = vouchRepository.insert(
                VouchWithPerson(
                    vouch = Vouch(
                        amount = abs(diff),
                        description = "[NETTING]", // TODO: localization
                        given = diff > 0L
                    ), persons = listOf(
                        PersonWithAmount(
                            person = Person(name = personName), amount = abs(diff)
                        )
                    )
                )
            )
            newNettingVouch = netVouch
            loadPerson(personName, false)
        }
        return Pair(settledVouches, newNettingVouch)
    }

    fun undoSettlePerson(previouslySettled: List<PersonSettlementChange>) {
        viewModelScope.launch {
            for (entry in previouslySettled) {
                vouchRepository.setPersonSettlement(entry.vouchId, entry.personId, false)
            }
            _uiState.value.personData?.person?.name?.let { loadPerson(it, false) }
        }
    }

    fun undoNetVouches(
        previouslySettled: List<PersonSettlementChange>,
        nettingVouch: VouchWithPerson?
    ) {
        viewModelScope.launch {
            if (nettingVouch != null) {
                vouchRepository.delete(nettingVouch.vouch.id)
            }
            for (entry in previouslySettled) {
                vouchRepository.setPersonSettlement(entry.vouchId, entry.personId, false)
            }
            _uiState.value.personData?.person?.name?.let { loadPerson(it, false) }
        }
    }
}
