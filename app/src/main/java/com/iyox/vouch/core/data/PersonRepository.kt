package com.iyox.vouch.core.data

import com.iyox.vouch.core.di.ApplicationScope
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepository @Inject constructor(
    private val personDao: PersonDao,
    @ApplicationScope private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    /**
     * Hot, reactive StateFlow of all person totals, eagerly held in RAM by [externalScope]
     * for instant 0ms screen rendering and search.
     */
    val personsTotalFlow: StateFlow<List<PersonTotal>> =
        personDao.getPersonsTotalAllFlow()
            .stateIn(
                scope = externalScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    fun getPersonsTotalFlow(): Flow<List<PersonTotal>> = personsTotalFlow

    suspend fun getPersonTotal(nameQuery: String? = null): List<PersonTotal> {
        val cached = personsTotalFlow.value
        if (cached.isNotEmpty()) {
            return if (nameQuery.isNullOrBlank()) {
                cached
            } else {
                cached.filter { it.person.name.contains(nameQuery, ignoreCase = true) }
            }
        }

        return if (nameQuery.isNullOrEmpty()) {
            personDao.getPersonsTotalAll()
        } else {
            personDao.getPersonsTotalFiltered(nameQuery)
        }
    }

    suspend fun getPersonsByTotalVouches(): List<PersonTotal> {
        val cached = personsTotalFlow.value
        if (cached.isNotEmpty()) {
            return cached.sortedByDescending { it.vouchCount }
        }
        return personDao.getPersonsByTotalVouches()
    }

    suspend fun getPersonTotalByName(name: String): PersonTotal? {
        val cached = personsTotalFlow.value.find { it.person.name.equals(name, ignoreCase = true) }
        if (cached != null) return cached

        return personDao.getPersonTotalByName(name)
    }

    suspend fun getPersonById(id: Long): Person? {
        val cached = personsTotalFlow.value.find { it.person.id == id }?.person
        if (cached != null) return cached

        return personDao.getById(id)
    }

    suspend fun getPersonByName(name: String): Person? {
        val cached = personsTotalFlow.value.find { it.person.name.equals(name, ignoreCase = true) }?.person
        if (cached != null) return cached

        return personDao.getByName(name)
    }

    /**
     * Renames a person by ID.
     */
    suspend fun renamePerson(personId: Long, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }
        val existing = personDao.getByName(trimmed)
        if (existing != null && existing.id != personId) {
            return Result.failure(IllegalStateException("A person with this name already exists"))
        }
        val current = personDao.getById(personId)
            ?: return Result.failure(NoSuchElementException("Person not found"))
        if (current.name == trimmed) {
            return Result.success(Unit)
        }

        personDao.update(current.copy(name = trimmed))
        return Result.success(Unit)
    }

    suspend fun renamePersonByName(oldName: String, newName: String): Result<Unit> {
        val trimmedOld = oldName.trim()
        val person = personDao.getByName(trimmedOld)
            ?: return Result.failure(NoSuchElementException("Person not found"))
        return renamePerson(person.id, newName)
    }
}