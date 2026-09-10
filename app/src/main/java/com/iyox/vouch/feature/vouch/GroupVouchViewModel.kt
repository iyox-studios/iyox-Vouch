package com.iyox.vouch.feature.vouch

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val STEP_DETAILS = 1
const val STEP_MEMBERS = 2

data class GroupMemberInput(
    val personName: String,
    val amount: String,
    val isCustom: Boolean,
    val isSelf: Boolean = false
)

data class GroupVouchUiState(
    val step: Int = STEP_DETAILS,
    val amount: String = "",
    val given: Boolean = true,
    val description: String = "",
    val personQuery: String = "",
    val personSearchResults: List<PersonTotal> = emptyList(),
    val suggestedPersons: List<PersonTotal> = emptyList(),
    val members: List<GroupMemberInput> = emptyList(),
    val saveSuccess: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class GroupVouchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VouchRepository,
    private val personRepository: PersonRepository,
    private val currencyManager: CurrencyManager
) : ViewModel() {

    private val initialVouchId: Long? = when (val raw = savedStateHandle.get<Any>("vouchId")) {
        is Long -> raw
        is Int -> raw.toLong()
        is String -> raw.toLongOrNull()
        else -> null
    }

    val sharedActions = SharedVouchActions(repository)

    private var searchJob: Job? = null
    private var editingVouchId: Long? = null
    private var originalVouch: VouchWithPerson? = null

    private fun resolveInitialState(): GroupVouchUiState {
        if (initialVouchId != null) {
            val cached = repository.vouchesFlow.value.find { it.vouch.id == initialVouchId }
            if (cached != null) {
                editingVouchId = cached.vouch.id
                originalVouch = cached
                val members = cached.toGroupMemberInputs(currencyManager)
                return GroupVouchUiState(
                    amount = currencyManager.formatAmount(cached.vouch.amount),
                    given = cached.vouch.given,
                    description = cached.vouch.description,
                    members = members,
                    step = STEP_DETAILS,
                    isLoading = false
                )
            }
        }
        return GroupVouchUiState(isLoading = initialVouchId != null)
    }

    private val _uiState = MutableStateFlow(resolveInitialState())
    val uiState: StateFlow<GroupVouchUiState> = _uiState.asStateFlow()

    init {
        loadSuggestedPersons()
        if (initialVouchId != null && originalVouch == null) {
            loadVouch(initialVouchId)
        }
    }

    fun loadVouch(id: Long?) {
        searchJob?.cancel()
        if (id != null && id == editingVouchId && originalVouch != null) {
            return
        }
        if (id == null) {
            editingVouchId = null
            originalVouch = null
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        val inMemory = repository.vouchesFlow.value.find { it.vouch.id == id }
        if (inMemory != null) {
            editingVouchId = inMemory.vouch.id
            originalVouch = inMemory
            val members = inMemory.toGroupMemberInputs(currencyManager)
            _uiState.update { state ->
                state.copy(
                    amount = currencyManager.formatAmount(inMemory.vouch.amount),
                    given = inMemory.vouch.given,
                    description = inMemory.vouch.description,
                    members = members,
                    step = STEP_DETAILS,
                    isLoading = false
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val vouch = repository.getById(id)
            if (vouch != null) {
                editingVouchId = vouch.vouch.id
                originalVouch = vouch
                val members = vouch.toGroupMemberInputs(currencyManager)
                _uiState.update { state ->
                    state.copy(
                        amount = currencyManager.formatAmount(vouch.vouch.amount),
                        given = vouch.vouch.given,
                        description = vouch.vouch.description,
                        members = members,
                        step = STEP_DETAILS,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amount = value) }
        recomputeDefaultSplitAmounts()
    }

    fun onGivenChange(value: Boolean) {
        _uiState.update { it.copy(given = value) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun continueToMembers() {
        val parsed = currencyManager.parseToMinorUnits(_uiState.value.amount)
        if (parsed != null && parsed > 0) {
            _uiState.update { it.copy(step = STEP_MEMBERS, errorMessageRes = null) }
            recomputeDefaultSplitAmounts()
        }
    }

    fun backToDetails() {
        _uiState.update { it.copy(step = STEP_DETAILS, errorMessageRes = null) }
    }

    fun onPersonQueryChange(query: String) {
        _uiState.update { it.copy(personQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotBlank()) {
                val results = personRepository.getPersonTotal(query)
                _uiState.update { it.copy(personSearchResults = results) }
            } else {
                _uiState.update { it.copy(personSearchResults = emptyList()) }
            }
        }
    }

    fun onMemberAmountChange(index: Int, amount: String) {
        _uiState.update { state ->
            val members = state.members.toMutableList()
            if (index in members.indices) {
                val current = members[index]
                members[index] = current.copy(
                    amount = amount,
                    isCustom = true
                )
            }
            state.copy(members = members)
        }
        recomputeDefaultSplitAmounts()
    }

    fun onMemberAmountChange(member: GroupMemberInput, amount: String) {
        _uiState.update { state ->
            val members = state.members.map { current ->
                if (current.personName == member.personName) {
                    current.copy(
                        amount = amount,
                        isCustom = true
                    )
                } else {
                    current
                }
            }
            state.copy(members = members)
        }
        recomputeDefaultSplitAmounts()
    }

    fun removeMember(index: Int) {
        _uiState.update { state ->
            val members = state.members.toMutableList()
            if (index in members.indices) {
                members.removeAt(index)
            }
            state.copy(members = members)
        }
        recomputeDefaultSplitAmounts()
    }

    fun removeMember(member: GroupMemberInput) {
        _uiState.update { state ->
            val members = state.members.filterNot { it.personName == member.personName }
            state.copy(members = members)
        }
        recomputeDefaultSplitAmounts()
    }

    fun resetAllToEqualSplit() {
        _uiState.update { current ->
            val updated = current.members.map { it.copy(isCustom = false) }
            current.copy(members = updated)
        }
        recomputeDefaultSplitAmounts()
    }

    fun resetMemberToEqualSplit(member: GroupMemberInput) {
        _uiState.update { current ->
            val updated = current.members.map { currentMember ->
                if (currentMember.personName == member.personName) {
                    currentMember.copy(isCustom = false)
                } else {
                    currentMember
                }
            }
            current.copy(members = updated)
        }
        recomputeDefaultSplitAmounts()
    }

    fun resetMemberToEqualSplit(personName: String) {
        _uiState.update { current ->
            val updated = current.members.map { member ->
                if (member.personName == personName) {
                    member.copy(isCustom = false)
                } else {
                    member
                }
            }
            current.copy(members = updated)
        }
        recomputeDefaultSplitAmounts()
    }

    fun saveGroupVouches(onSaved: (VouchWithPerson?) -> Unit = {}) {
        viewModelScope.launch {
            val saved = saveGroupVouchesInternal()
            onSaved(saved)
        }
    }

    suspend fun saveGroupVouchesDirect(): VouchWithPerson? = saveGroupVouchesInternal()

    private suspend fun saveGroupVouchesInternal(): VouchWithPerson? {
        val currentState = _uiState.value
        val totalAmount = currencyManager.parseToMinorUnits(currentState.amount)
        if (totalAmount == null) {
            _uiState.update {
                it.copy(
                    saveSuccess = false,
                    errorMessageRes = R.string.error_invalid_total_amount
                )
            }
            return null
        }
        val nonSelfMembers = currentState.members.filterNot { it.isSelf }
        if (nonSelfMembers.size < 2) {
            _uiState.update {
                it.copy(
                    saveSuccess = false,
                    errorMessageRes = R.string.error_group_min_members
                )
            }
            return null
        }

        val customSum = currentState.members
            .filter { it.isCustom }
            .sumOf { currencyManager.parseToMinorUnits(it.amount) ?: 0L }
        val nonCustomCount = currentState.members.count { !it.isCustom }
        val defaultSplit = if (nonCustomCount > 0) {
            maxOf(0L, totalAmount - customSum) / nonCustomCount
        } else {
            0L
        }
        val otherMemberAmounts = mutableListOf<Pair<GroupMemberInput, Long>>()
        for (member in nonSelfMembers) {
            val amount = if (member.isCustom) {
                currencyManager.parseToMinorUnits(member.amount)
            } else {
                currencyManager.parseToMinorUnits(member.amount) ?: defaultSplit
            }
            if (amount == null) {
                _uiState.update {
                    it.copy(
                        saveSuccess = false,
                        errorMessageRes = R.string.error_invalid_member_amount
                    )
                }
                return null
            }
            otherMemberAmounts += member to amount
        }

        // The created vouch amount represents the total amount owed by / to the others (excluding self)
        val vouchTotalAmount = otherMemberAmounts.sumOf { it.second }

        val savedVouch: VouchWithPerson?
        if (editingVouchId != null && originalVouch != null) {
            val updatedVouch = originalVouch!!.copy(
                vouch = originalVouch!!.vouch.copy(
                    amount = vouchTotalAmount,
                    description = currentState.description.trim(),
                    given = currentState.given
                ),
                persons = otherMemberAmounts.map { (member, amount) ->
                    PersonWithAmount(
                        person = Person(name = member.personName),
                        amount = amount,
                        isCustom = member.isCustom
                    )
                }
            )
            repository.update(updatedVouch)
            savedVouch = updatedVouch
        } else {
            val vouch = VouchWithPerson(
                vouch = Vouch(
                    id = 0,
                    amount = vouchTotalAmount,
                    description = currentState.description.trim(),
                    given = currentState.given
                ),
                persons = otherMemberAmounts.map { (member, amount) ->
                    PersonWithAmount(
                        person = Person(name = member.personName),
                        amount = amount,
                        isCustom = member.isCustom
                    )
                }
            )
            savedVouch = repository.insert(vouch)
        }
        _uiState.update { it.copy(saveSuccess = true, errorMessageRes = null) }
        return savedVouch
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun addMember(name: String, isSelf: Boolean = false) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        _uiState.update { state ->
            if (state.members.any {
                    it.isSelf == isSelf && it.personName.equals(
                        trimmed,
                        ignoreCase = true
                    )
                }) {
                return@update state.copy(personQuery = "", personSearchResults = emptyList())
            }
            val newMember = GroupMemberInput(
                personName = trimmed,
                amount = "",
                isCustom = false,
                isSelf = isSelf
            )
            state.copy(
                members = if (isSelf) listOf(newMember) + state.members else state.members + newMember,
                personQuery = "",
                personSearchResults = emptyList()
            )
        }
        recomputeDefaultSplitAmounts()
    }

    fun onPersonSelected(person: Person) {
        addMember(person.name, isSelf = false)
    }

    fun addMemberFromQuery() {
        val query = _uiState.value.personQuery.trim()
        if (query.isNotBlank()) {
            addMember(query, isSelf = false)
        }
    }

    fun addSelfMember(selfName: String) {
        addMember(selfName, isSelf = true)
    }

    private fun recomputeDefaultSplitAmounts() {
        val state = _uiState.value
        val totalAmount = currencyManager.parseToMinorUnits(state.amount) ?: 0L
        val members = state.members
        if (members.isEmpty()) return

        val customSum = members
            .filter { it.isCustom }
            .sumOf { currencyManager.parseToMinorUnits(it.amount) ?: 0L }

        val nonCustomCount = members.count { !it.isCustom }
        if (nonCustomCount == 0) return

        val remaining = maxOf(0L, totalAmount - customSum)
        val defaultAmount = remaining / nonCustomCount

        _uiState.update { currentState ->
            currentState.copy(
                members = currentState.members.map { member ->
                    if (member.isCustom) {
                        member
                    } else {
                        member.copy(amount = currencyManager.formatAmount(defaultAmount))
                    }
                }
            )
        }
    }

    private fun loadSuggestedPersons() {
        viewModelScope.launch {
            val suggestions = personRepository.getPersonsByTotalVouches()
            _uiState.update { it.copy(suggestedPersons = suggestions) }
        }
    }
}

/**
 * Extension mapper extracted to avoid code duplication across create/load flows.
 */
fun VouchWithPerson.toGroupMemberInputs(currencyManager: CurrencyManager): List<GroupMemberInput> {
    val result = mutableListOf<GroupMemberInput>()
    for (personEntry in this.persons) {
        result.add(
            GroupMemberInput(
                personName = personEntry.person.name,
                amount = currencyManager.formatAmount(personEntry.amount),
                isCustom = personEntry.isCustom,
                isSelf = false
            )
        )
    }
    return result
}
