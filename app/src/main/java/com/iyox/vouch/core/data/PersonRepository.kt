package com.iyox.vouch.core.data

import android.content.Context
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithTotal
import com.iyox.vouch.core.model.Vouch

class PersonRepository private constructor(context: Context) {

    private val personDao = AppDatabase.getDatabase(context).personDao()

    suspend fun searchPersons(query: String): List<Person> {
        val localResults = personDao.findPersonsByName(query)

        return (localResults).sortedBy { it.name }
    }

    suspend fun getPersonsWithTotal(query: String? = null): List<PersonWithTotal> {
        return if (query.isNullOrEmpty()) {
            personDao.getPersonsWithTotalAll()
        } else {
            personDao.getPersonsWithTotalFiltered(query)
        }
    }

    suspend fun onVouchInserted(vouch: Vouch) {
        if (vouch.personName == null) return
        val existingPerson = personDao.findPersonsByName(vouch.personName)
        if (existingPerson.isEmpty()) {
            val newPerson = Person(name = vouch.personName)
            personDao.insert(newPerson)
        }
    }


    suspend fun insertPerson(person: Person) {
        personDao.insert(person)
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