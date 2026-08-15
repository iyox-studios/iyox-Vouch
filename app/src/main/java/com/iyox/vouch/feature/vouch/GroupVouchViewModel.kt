package com.iyox.vouch.feature.vouch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
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
    val isCustom: Boolean
)

data class GroupVouchUiState(
    val step: Int = STEP_DETAILS,
    val amount: String = "",
    val given: Boolean = true,
    val description: String = "",
    val personQuery: String = "",
    val personSearchResults: List<PersonTotal> = emptyList(),
    val members: List<GroupMemberInput> = emptyList(),
    val error: String? = null,
    val saveSuccess: Boolean = false
)

class GroupVouchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VouchRepository.getInstance(application)
    private val personRepository = PersonRepository.getInstance(application)

    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(GroupVouchUiState())
    val uiState: StateFlow<GroupVouchUiState> = _uiState.asStateFlow()

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

    fun removeMember(personName: String) {
        _uiState.update { state ->
            state.copy(members = state.members.filterNot { it.personName == personName })
        }
        recomputeDefaultSplitAmounts()
    }

    fun onMemberAmountChange(personName: String, value: String) {
        _uiState.update { state ->
            val updated = state.members.map { member ->
                if (member.personName == personName) {
                    val isCustom = value.isNotBlank()
                    member.copy(amount = value, isCustom = isCustom)
                } else {
                    member
                }
            }
            state.copy(members = updated)
        }
        if (value.isBlank()) {
            recomputeDefaultSplitAmounts()
        }
    }

    fun continueToMembers() {
        if (CurrencyManager.parseToMinorUnits(_uiState.value.amount) == null) {
            _uiState.update { it.copy(error = "Invalid amount format") }
            return
        }
        _uiState.update { it.copy(step = STEP_MEMBERS) }
        recomputeDefaultSplitAmounts()
    }

    fun saveGroupVouches() {
        val currentState = _uiState.value
        val totalAmount = CurrencyManager.parseToMinorUnits(currentState.amount)
        if (totalAmount == null) {
            _uiState.update { it.copy(error = "Invalid amount format") }
            return
        }
        if (currentState.members.isEmpty()) {
            _uiState.update { it.copy(error = "Add at least one person") }
            return
        }

        val defaultSplit = computeDefaultSplitAmount(totalAmount, currentState.members.size)
        val memberAmounts = mutableListOf<Pair<String, Long>>()
        for (member in currentState.members) {
            val amount = if (member.isCustom) {
                CurrencyManager.parseToMinorUnits(member.amount)
            } else {
                defaultSplit
            }
            if (amount == null) {
                _uiState.update { it.copy(error = "Invalid amount for ${member.personName}") }
                return
            }
            memberAmounts += member.personName to amount
        }

        viewModelScope.launch {
            val vouch = VouchWithPerson(
                vouch = Vouch(
                    id = 0,
                    amount = totalAmount,
                    description = currentState.description.trim(),
                    given = currentState.given
                ),
                persons = memberAmounts.map { (personName, amount) ->
                    PersonWithAmount(Person(name = personName), amount)
                }
            )

            repository.insert(vouch)
            _uiState.update { it.copy(saveSuccess = true) }
        }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun addMember(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        _uiState.update { state ->
            if (state.members.any { it.personName.equals(trimmed, ignoreCase = true) }) {
                return@update state.copy(personQuery = "", personSearchResults = emptyList())
            }
            val newMember = GroupMemberInput(
                personName = trimmed,
                amount = "",
                isCustom = false
            )
            state.copy(
                members = state.members + newMember,
                personQuery = "",
                personSearchResults = emptyList()
            )
        }
        recomputeDefaultSplitAmounts()
    }

    private fun recomputeDefaultSplitAmounts() {
        val state = _uiState.value
        val totalAmount = CurrencyManager.parseToMinorUnits(state.amount) ?: 0L
        val memberCount = state.members.size
        if (memberCount == 0) return

        val defaultSplit = computeDefaultSplitAmount(totalAmount, memberCount)
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

    private fun computeDefaultSplitAmount(
        total: Long,
        memberCount: Int
    ): Long {
        if (memberCount == 0) return 0L
        return total / memberCount
    }
}
