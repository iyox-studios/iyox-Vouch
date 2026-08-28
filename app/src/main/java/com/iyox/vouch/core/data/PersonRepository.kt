package com.iyox.vouch.core.data

import android.content.Context
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal

class PersonRepository private constructor(context: Context) {

    private val personDao = AppDatabase.getDatabase(context).personDao()
    private val vouchRepository = VouchRepository.getInstance(context)

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

    suspend fun getPersonTotalByName(name: String): PersonTotal? {
        return personDao.getPersonTotalByName(name)
    }

    suspend fun getPersonById(id: Int): Person? {
        return personDao.getById(id)
    }

    suspend fun getPersonByName(name: String): Person? {
        return personDao.getByName(name)
    }

    suspend fun renamePerson(personId: Int, newName: String): Result<Unit> {
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
        vouchRepository.notifyDataChanged()
        return Result.success(Unit)
    }

    suspend fun renamePersonByName(oldName: String, newName: String): Result<Unit> {
        val trimmedOld = oldName.trim()
        val person = personDao.getByName(trimmedOld)
            ?: return Result.failure(NoSuchElementException("Person not found"))
        return renamePerson(person.id, newName)
    }

    companion object {
        @Volatile
        private var INSTANCE: PersonRepository? = null

        fun getInstance(context: Context): PersonRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = PersonRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}