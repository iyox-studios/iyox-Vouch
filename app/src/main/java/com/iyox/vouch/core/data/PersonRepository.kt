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
     * Reactive, hot StateFlow of all person totals, eagerly held in RAM by [externalScope]
     * for instant 0ms screen rendering on tab navigation.
     * Initialized to null until SQLite finishes the first load.
     */
    val personsTotalFlow: StateFlow<List<PersonTotal>?> =
        personDao.getPersonsTotalAllFlow()
            .stateIn(
                scope = externalScope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )

    fun getPersonsTotalFlow(): Flow<List<PersonTotal>?> = personsTotalFlow

    /**
     * Explicit warm-up hook called during application startup to prime the in-memory cache
     * before the user navigates to the People tab, enabling 0ms instant tab transitions.
     */
    fun warmUp() {
        personsTotalFlow
    }

    suspend fun getPersonTotal(nameQuery: String? = null): List<PersonTotal> {
        return if (nameQuery.isNullOrEmpty()) {
            personDao.getPersonsTotalAll()
        } else {
            personDao.getPersonsTotalFiltered(nameQuery)
        }
    }

    suspend fun getPersonsByTotalVouches(): List<PersonTotal> {
        return personDao.getPersonsByTotalVouches()
    }

    suspend fun getPersonByName(name: String): Person? {
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
}