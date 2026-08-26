package com.iyox.vouch.feature.person

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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

/**
 * @param personName passed at construction so the ViewModel can pre-populate its state from
 *   the in-memory cache before the first Compose frame is drawn — eliminating the spinner
 *   on re-entry. Use [PersonViewModel.factory] to create instances.
 */
class PersonViewModel(
    application: Application,
    personName: String
) : AndroidViewModel(application) {

    private val vouchRepository = VouchRepository.getInstance(application)

    // Initialize from cache synchronously — if cached data exists for this person the
    // initial state already has isLoading=false and real content, so frame 1 shows
    // content with no spinner or layout pop.
    private val _uiState = MutableStateFlow(
        vouchRepository.getCachedVouchesByPerson(personName)
            ?.let { cached -> buildState(personName, cached) }
            ?: PersonUiState()
    )
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            vouchRepository.dataUpdates.collect {
                val name = _uiState.value.personData?.person?.name
                if (name != null) {
                    loadPerson(name, shouldShowLoading = false)
                }
            }
        }
    }

    val sharedActions = SharedVouchActions(vouchRepository) {
        val name = _uiState.value.personData?.person?.name
        if (name != null) {
            loadPerson(name, shouldShowLoading = false)
        }
    }

    fun loadPerson(name: String, shouldShowLoading: Boolean = true) {
        // Cache hit — update synchronously, no spinner needed
        val cached = vouchRepository.getCachedVouchesByPerson(name)
        if (cached != null) {
            _uiState.update { buildState(name, cached) }
            return
        }

        // Cache miss — show spinner and load from DB
        _uiState.update { it.copy(isLoading = shouldShowLoading) }
        viewModelScope.launch {
            val personVouches = vouchRepository.getVouchesByPersonName(name)
            _uiState.update { buildState(name, personVouches) }
        }
    }

    private fun buildState(name: String, personVouches: List<VouchWithPerson>): PersonUiState {
        val sortedVouches = personVouches.sortedByDescending { it.vouch.timestamp }
        var given = 0L
        var owed = 0L
        var unsettledCount = 0
        for (vouch in sortedVouches) {
            val personEntry = vouch.persons.firstOrNull { it.person.name == name }
            if (personEntry != null && !personEntry.settled) {
                unsettledCount++
                if (vouch.vouch.given) {
                    given += personEntry.amount
                } else {
                    owed += personEntry.amount
                }
            }
        }

        val updatedPersonData = if (sortedVouches.isNotEmpty()) {
            PersonTotal(
                person = sortedVouches
                    .firstNotNullOfOrNull { v -> v.persons.firstOrNull { it.person.name == name }?.person }
                    ?: Person(name = name),
                amount = given - owed,
                vouchCount = unsettledCount
            )
        } else null

        return PersonUiState(
            personData = updatedPersonData,
            vouches = sortedVouches,
            amountGiven = given,
            amountOwed = owed,
            isLoading = false
        )
    }

    suspend fun settlePerson(): List<PersonSettlementChange> {
        val personName = _uiState.value.personData?.person?.name ?: return emptyList()
        val unsettledEntries = _uiState.value.vouches.mapNotNull { vouch ->
            val personEntry = vouch.persons.firstOrNull { it.person.name == personName }
                ?: return@mapNotNull null
            if (personEntry.settled) return@mapNotNull null
            PersonSettlementChange(vouchId = vouch.vouch.id, personId = personEntry.person.id)
        }
        if (unsettledEntries.isEmpty()) return emptyList()

        unsettledEntries.groupBy { it.personId }.forEach { (personId, changes) ->
            vouchRepository.setPersonSettlements(personId, changes.map { it.vouchId }, true)
        }
        loadPerson(personName, shouldShowLoading = false)
        return unsettledEntries
    }

    /**
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
            loadPerson(personName, shouldShowLoading = false)
        }
        return Pair(settledVouches, newNettingVouch)
    }

    fun undoSettlePerson(previouslySettled: List<PersonSettlementChange>) {
        viewModelScope.launch {
            if (previouslySettled.isNotEmpty()) {
                previouslySettled.groupBy { it.personId }.forEach { (personId, changes) ->
                    vouchRepository.setPersonSettlements(personId, changes.map { it.vouchId }, false)
                }
            }
            _uiState.value.personData?.person?.name?.let { loadPerson(it, shouldShowLoading = false) }
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
            if (previouslySettled.isNotEmpty()) {
                previouslySettled.groupBy { it.personId }.forEach { (personId, changes) ->
                    vouchRepository.setPersonSettlements(personId, changes.map { it.vouchId }, false)
                }
            }
            _uiState.value.personData?.person?.name?.let { loadPerson(it, shouldShowLoading = false) }
        }
    }

    companion object {
        fun factory(personName: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                PersonViewModel(application, personName)
            }
        }
    }
}
