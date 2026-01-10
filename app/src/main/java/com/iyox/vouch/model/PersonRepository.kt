package com.iyox.vouch.model

import android.content.Context

class PersonRepository private constructor(context: Context) {

    private val personDao = AppDatabase.getDatabase(context).personDao()

    suspend fun searchPersons(query: String): List<Person> {
        val localResults = personDao.findPersonsByName(query)

        return (localResults).sortedBy { it.name }
    }

    suspend fun getPersonsWithTotal(): List<PersonWithTotal> {
        return personDao.getPersonsWithTotal()
    }

    suspend fun onVouchInserted(vouch: Vouch) {
        if(vouch.personName == null) return
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
