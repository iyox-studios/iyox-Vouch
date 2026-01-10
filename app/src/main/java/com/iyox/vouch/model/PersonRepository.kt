package com.iyox.vouch.model

import android.content.Context

class PersonRepository private constructor(context: Context) {

    private val personDao = AppDatabase.getDatabase(context).personDao()
    private val contactsRepository = ContactsRepository(context.contentResolver)

    data class CombinedPersonResult(
        val id: String,
        val name: String,
    )

    suspend fun searchPersonsCombined(query: String): List<Person> {
        val localResults = personDao.searchPersons(query)
        val deviceResults = contactsRepository.searchContacts(query)

        return (localResults + deviceResults)
            .sortedBy { it.firstName }
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
