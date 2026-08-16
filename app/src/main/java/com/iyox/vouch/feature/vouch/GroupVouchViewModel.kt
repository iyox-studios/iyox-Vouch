package com.iyox.vouch.feature.vouch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STEP_DETAILS = 1
private const val STEP_MEMBERS = 2

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
    val isLoading: Boolean = false
)

class GroupVouchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VouchRepository.getInstance(application)
    private val personRepository = PersonRepository.getInstance(application)

    val sharedActions = SharedVouchActions(repository) {}

    private var searchJob: Job? = null
    private var editingVouchId: Int? = null
    private var originalVouch: VouchWithPerson? = null

    private val _uiState = MutableStateFlow(GroupVouchUiState())
    val uiState: StateFlow<GroupVouchUiState> = _uiState.asStateFlow()

    init {
        loadSuggestedPersons()
    }

    fun loadVouch(id: Long?) {
        searchJob?.cancel()
        if (id == null) {
            editingVouchId = null
            originalVouch = null
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        val intId = id.toInt()
        viewModelScope.launch {
            val vouch = repository.getById(intId)
            if (vouch != null) {
                editingVouchId = vouch.vouch.id
                originalVouch = vouch
                val memberCount = vouch.persons.size
                val totalAmount = vouch.vouch.amount
                val defaultSplit = if (memberCount > 0) totalAmount / memberCount else 0L
                val members = vouch.persons.map { p ->
                    val isCustom = p.amount != defaultSplit
                    GroupMemberInput(
                        personName = p.person.name,
                        amount = CurrencyManager.formatAmount(p.amount),
                        isCustom = isCustom,
                        isSelf = false
                    )
                }
                _uiState.update { state ->
                    state.copy(
                        amount = CurrencyManager.formatAmount(totalAmount),
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

    fun onPersonQueryChange(value: String) {
        _uiState.update { it.copy(personQuery = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val results = if (value.isBlank()) {
                emptyList()
            } else {
                personRepository.getPersonTotal(value)
            }
            _uiState.update { it.copy(personSearchResults = results) }
        }
    }

    fun onPersonSelected(person: Person) {
        addMember(person.name)
    }

    fun addMemberFromQuery() {
        addMember(_uiState.value.personQuery)
    }

    fun addSelfMember(selfName: String) {
        addMember(selfName, isSelf = true)
    }

    fun removeMember(member: GroupMemberInput) {
        _uiState.update { state ->
            state.copy(members = state.members.filterNot { it == member })
        }
        recomputeDefaultSplitAmounts()
    }

    fun removeMember(personName: String) {
        _uiState.update { state ->
            state.copy(members = state.members.filterNot { it.personName == personName })
        }
        recomputeDefaultSplitAmounts()
    }

    fun onMemberAmountChange(member: GroupMemberInput, value: String) {
        _uiState.update { state ->
            val updated = state.members.map { current ->
                if (current == member) {
                    current.copy(amount = value, isCustom = true)
                } else {
                    current
                }
            }
            state.copy(members = updated)
        }
    }

    fun onMemberAmountChange(personName: String, value: String) {
        _uiState.update { state ->
            val updated = state.members.map { member ->
                if (member.personName == personName) {
                    member.copy(amount = value, isCustom = true)
                } else {
                    member
                }
            }
            state.copy(members = updated)
        }
    }

    fun continueToMembers() {
        if (CurrencyManager.parseToMinorUnits(_uiState.value.amount) == null) {
            return
        }
        _uiState.update { it.copy(step = STEP_MEMBERS) }
        loadSuggestedPersons()
        recomputeDefaultSplitAmounts()
    }

    fun backToDetails() {
        _uiState.update { it.copy(step = STEP_DETAILS) }
    }

    fun loadSuggestedPersons() {
        viewModelScope.launch {
            val all = personRepository.getPersonsByTotalVouches()
            _uiState.update { it.copy(suggestedPersons = all) }
        }
    }

    fun resetAllToEqualSplit() {
        val totalAmount = CurrencyManager.parseToMinorUnits(_uiState.value.amount) ?: 0L
        val members = _uiState.value.members
        if (members.isEmpty()) return

        val customMembers = members.filter { it.isCustom }
        val nonCustomMembers = members.filterNot { it.isCustom }
        val sumCustom = customMembers.sumOf { CurrencyManager.parseToMinorUnits(it.amount) ?: 0L }
        val nonCustomCount = nonCustomMembers.size

        if (nonCustomCount > 0) {
            val remainingForNonCustom = (totalAmount - sumCustom).coerceAtLeast(0L)
            val splitAmount = remainingForNonCustom / nonCustomCount
            val formattedSplit = CurrencyManager.formatAmount(splitAmount)
            _uiState.update { current ->
                val updated = current.members.map { member ->
                    if (member.isCustom) {
                        member
                    } else {
                        member.copy(amount = formattedSplit, isCustom = false)
                    }
                }
                current.copy(members = updated)
            }
        } else {
            // If all members are custom, reset all to equal
            val splitAmount = totalAmount / members.size
            val formattedSplit = CurrencyManager.formatAmount(splitAmount)
            _uiState.update { current ->
                val updated = current.members.map { member ->
                    member.copy(amount = formattedSplit, isCustom = false)
                }
                current.copy(members = updated)
            }
        }
    }

    fun resetMemberToEqualSplit(member: GroupMemberInput) {
        _uiState.update { current ->
            val updated = current.members.map { currentMember ->
                if (currentMember == member) {
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

    suspend fun saveGroupVouches(): VouchWithPerson? {
        val currentState = _uiState.value
        val totalAmount = CurrencyManager.parseToMinorUnits(currentState.amount)
        if (totalAmount == null) {
            _uiState.update { it.copy(saveSuccess = false) }
            return null
        }
        val nonSelfMembers = currentState.members.filterNot { it.isSelf }
        if (nonSelfMembers.size < 2) {
            _uiState.update { it.copy(saveSuccess = false) }
            return null
        }

        val defaultSplit = computeDefaultSplitAmount(totalAmount, currentState.members.size)
        val otherMemberAmounts = mutableListOf<Pair<String, Long>>()
        for (member in nonSelfMembers) {
            val amount = if (member.isCustom) {
                CurrencyManager.parseToMinorUnits(member.amount)
            } else {
                defaultSplit
            }
            if (amount == null) {
                _uiState.update { it.copy(saveSuccess = false) }
                return null
            }
            otherMemberAmounts += member.personName to amount
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
                persons = otherMemberAmounts.map { (personName, amount) ->
                    PersonWithAmount(Person(name = personName), amount)
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
                persons = otherMemberAmounts.map { (personName, amount) ->
                    PersonWithAmount(Person(name = personName), amount)
                }
            )
            savedVouch = repository.insert(vouch)
        }
        _uiState.update { it.copy(saveSuccess = true) }
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

    private fun recomputeDefaultSplitAmounts() {
        val state = _uiState.value
        val totalAmount = CurrencyManager.parseToMinorUnits(state.amount) ?: 0L
        val members = state.members
        if (members.isEmpty()) return

        val customMembers = members.filter { it.isCustom }
        val nonCustomMembers = members.filterNot { it.isCustom }
        val sumCustom = customMembers.sumOf { CurrencyManager.parseToMinorUnits(it.amount) ?: 0L }
        val nonCustomCount = nonCustomMembers.size

        if (nonCustomCount > 0) {
            val remainingForNonCustom = (totalAmount - sumCustom).coerceAtLeast(0L)
            val defaultSplit = remainingForNonCustom / nonCustomCount
            val formattedDefault = CurrencyManager.formatAmount(defaultSplit)
            _uiState.update { current ->
                val updated = current.members.map { member ->
                    if (member.isCustom) {
                        member
                    } else {
                        member.copy(amount = formattedDefault)
                    }
                }
                current.copy(members = updated)
            }
        }
    }

    private fun computeDefaultSplitAmount(
        total: Long,
        memberCount: Int
    ): Long {
        if (memberCount == 0) return 0L
        return total / memberCount
    }
}
