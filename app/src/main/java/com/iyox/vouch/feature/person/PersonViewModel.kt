package com.iyox.vouch.feature.person

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
    val settledVouches: List<VouchWithPerson> = emptyList(),
    val settledCount: Int = 0,
    val amountGiven: Long = 0L,
    val amountOwed: Long = 0L,
    val showOnlyGiven: Int = 0,
    val sortedBy: Int = 0,
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
    private val personName: String
) : AndroidViewModel(application) {

    private val vouchRepository = VouchRepository.getInstance(application)
    private val personRepository = PersonRepository.getInstance(application)
    private var currentPersonName: String = personName
    private val _showOnlyGiven = MutableStateFlow(0)
    private val _sortedBy = MutableStateFlow(0)
    private var currentPersonVouches: List<VouchWithPerson> =
        vouchRepository.getCachedVouchesByPerson(personName) ?: emptyList()

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
                loadPerson(currentPersonName, shouldShowLoading = false)
            }
        }
    }

    val sharedActions = SharedVouchActions(vouchRepository) {
        loadPerson(currentPersonName, shouldShowLoading = false)
    }

    fun loadPerson(name: String, shouldShowLoading: Boolean = true) {
        currentPersonName = name
        // Cache hit — update synchronously, no spinner needed
        val cached = vouchRepository.getCachedVouchesByPerson(name)
        if (cached != null) {
            currentPersonVouches = cached
            _uiState.update { buildState(name, cached) }
            return
        }

        // Cache miss — show spinner and load from DB
        _uiState.update { it.copy(isLoading = shouldShowLoading) }
        viewModelScope.launch {
            val personVouches = vouchRepository.getVouchesByPersonName(name)
            currentPersonVouches = personVouches
            _uiState.update { buildState(name, personVouches) }
        }
    }

    fun toggleShowOnlyGiven() {
        _showOnlyGiven.value = (_showOnlyGiven.value + 1) % 3
        _uiState.update { buildState(currentPersonName, currentPersonVouches) }
    }

    fun toggleSortedBy() {
        _sortedBy.value = (_sortedBy.value + 1) % 2
        _uiState.update { buildState(currentPersonName, currentPersonVouches) }
    }

    private fun buildState(name: String, personVouches: List<VouchWithPerson>): PersonUiState {
        val activeVouches = mutableListOf<VouchWithPerson>()
        val settledVouches = mutableListOf<VouchWithPerson>()
        var given = 0L
        var owed = 0L

        for (vouch in personVouches) {
            val personEntry = vouch.persons.firstOrNull { it.person.name == name }
            if (personEntry != null) {
                if (personEntry.settled) {
                    settledVouches.add(vouch)
                } else {
                    activeVouches.add(vouch)
                    if (vouch.vouch.given) {
                        given += personEntry.amount
                    } else {
                        owed += personEntry.amount
                    }
                }
            }
        }

        val showOnlyGiven = _showOnlyGiven.value
        val sortedBy = _sortedBy.value

        val filteredActive = activeVouches.filter {
            when (showOnlyGiven) {
                0 -> true
                1 -> it.vouch.given
                2 -> !it.vouch.given
                else -> true
            }
        }

        val sortedActive = when (sortedBy) {
            0 -> filteredActive.sortedBy { it.vouch.timestamp }
            1 -> filteredActive.sortedBy { it.vouch.amount }
            2 -> filteredActive.sortedBy { if (it.persons.size == 1) it.persons.first().person.name else "" }
            else -> filteredActive.sortedBy { it.vouch.timestamp }
        }.reversed()

        val sortedSettled = settledVouches.sortedByDescending { it.vouch.timestamp }

        val updatedPersonData = if (personVouches.isNotEmpty()) {
            PersonTotal(
                person = personVouches
                    .firstNotNullOfOrNull { v -> v.persons.firstOrNull { it.person.name == name }?.person }
                    ?: Person(name = name),
                amount = given - owed,
                vouchCount = activeVouches.size
            )
        } else null

        return PersonUiState(
            personData = updatedPersonData,
            vouches = sortedActive,
            settledVouches = sortedSettled,
            settledCount = sortedSettled.size,
            amountGiven = given,
            amountOwed = owed,
            showOnlyGiven = showOnlyGiven,
            sortedBy = sortedBy,
            isLoading = false
        )
    }

    suspend fun renamePerson(newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }
        val person = _uiState.value.personData?.person
            ?: personRepository.getPersonByName(currentPersonName)
            ?: return Result.failure(NoSuchElementException("Person not found"))

        val result = personRepository.renamePerson(person.id, trimmed)
        if (result.isSuccess) {
            currentPersonName = trimmed
            loadPerson(trimmed, shouldShowLoading = false)
        }
        return result
    }

    suspend fun settlePerson(): List<PersonSettlementChange> {
        val personName = currentPersonName
        val unsettledEntries = currentPersonVouches.mapNotNull { vouch ->
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

        val personName = currentPersonName
        if (diff != 0L) {
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
            loadPerson(currentPersonName, shouldShowLoading = false)
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
            loadPerson(currentPersonName, shouldShowLoading = false)
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
