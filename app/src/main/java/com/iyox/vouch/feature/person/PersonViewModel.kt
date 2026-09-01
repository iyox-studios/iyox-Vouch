package com.iyox.vouch.feature.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchSortOrder
import com.iyox.vouch.core.model.VouchTypeFilter
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.utils.VouchFilterUtils.filterSearchAndSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

data class PersonUiState(
    val personData: PersonTotal? = null,
    val vouches: List<VouchWithPerson> = emptyList(),
    val settledVouches: List<VouchWithPerson> = emptyList(),
    val settledCount: Int = 0,
    val amountGiven: Long = 0L,
    val amountOwed: Long = 0L,
    val showOnlyGiven: VouchTypeFilter = VouchTypeFilter.ALL,
    val sortedBy: VouchSortOrder = VouchSortOrder.DATE,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

data class PersonSettlementChange(
    val vouchId: Long,
    val personId: Long
)

@HiltViewModel
class PersonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vouchRepository: VouchRepository,
    private val personRepository: PersonRepository
) : ViewModel() {

    private val initialPersonName: String = checkNotNull(savedStateHandle["personName"])
    private val _personName = MutableStateFlow(initialPersonName)
    private val _showOnlyGiven = MutableStateFlow(VouchTypeFilter.ALL)
    private val _sortedBy = MutableStateFlow(VouchSortOrder.DATE)
    private val _searchQuery = MutableStateFlow("")

    val sharedActions = SharedVouchActions(vouchRepository) {}

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PersonUiState> = _personName.flatMapLatest { name ->
        combine(
            vouchRepository.getVouchesByPersonNameFlow(name),
            _showOnlyGiven,
            _sortedBy,
            _searchQuery
        ) { personVouches, showOnlyGiven, sortedBy, searchQuery ->
            buildState(name, personVouches, showOnlyGiven, sortedBy, searchQuery)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PersonUiState(isLoading = false)
    )

    fun loadPerson(name: String, shouldShowLoading: Boolean = false) {
        _personName.value = name
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowOnlyGiven() {
        _showOnlyGiven.value = _showOnlyGiven.value.next()
    }

    fun toggleSortedBy() {
        _sortedBy.value = _sortedBy.value.next(supportsPerson = false)
    }

    private fun buildState(
        name: String,
        personVouches: List<VouchWithPerson>,
        showOnlyGiven: VouchTypeFilter,
        sortOrder: VouchSortOrder,
        searchQuery: String
    ): PersonUiState {
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

        val sortedActive = activeVouches.filterSearchAndSort(
            typeFilter = showOnlyGiven,
            searchQuery = searchQuery,
            sortOrder = sortOrder
        )

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
            sortedBy = sortOrder,
            searchQuery = searchQuery,
            isLoading = false
        )
    }

    suspend fun renamePerson(newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }
        val currentName = _personName.value
        val person = uiState.value.personData?.person
            ?: personRepository.getPersonByName(currentName)
            ?: return Result.failure(NoSuchElementException("Person not found"))

        val result = personRepository.renamePerson(person.id, trimmed)
        if (result.isSuccess) {
            _personName.value = trimmed
        }
        return result
    }

    suspend fun settlePerson(): List<PersonSettlementChange> {
        val personName = _personName.value
        val currentVouches = vouchRepository.getVouchesByPersonName(personName)
        val unsettledEntries = currentVouches.mapNotNull { vouch ->
            val personEntry = vouch.persons.firstOrNull { it.person.name == personName }
                ?: return@mapNotNull null
            if (personEntry.settled) return@mapNotNull null
            PersonSettlementChange(vouchId = vouch.vouch.id, personId = personEntry.person.id)
        }
        if (unsettledEntries.isEmpty()) return emptyList()

        unsettledEntries.groupBy { it.personId }.forEach { (personId, changes) ->
            vouchRepository.setPersonSettlements(personId, changes.map { it.vouchId }, true)
        }
        return unsettledEntries
    }

    /**
     * @return (listOfJustSettledVouches, theNewNettingVouchOrNull)
     */
    suspend fun netVouches(): Pair<List<PersonSettlementChange>, VouchWithPerson?> {
        val diff = uiState.value.amountGiven - uiState.value.amountOwed
        val settledVouches = settlePerson()
        var newNettingVouch: VouchWithPerson? = null

        val personName = _personName.value
        if (diff != 0L) {
            val netVouch = vouchRepository.insert(
                VouchWithPerson(
                    vouch = Vouch(
                        amount = abs(diff),
                        description = com.iyox.vouch.core.utils.VouchConstants.TAG_NETTING,
                        given = diff > 0L
                    ), persons = listOf(
                        PersonWithAmount(
                            person = Person(name = personName), amount = abs(diff)
                        )
                    )
                )
            )
            newNettingVouch = netVouch
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
        }
    }
}
