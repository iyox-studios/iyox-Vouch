package com.iyox.vouch.core.data

import android.content.Context
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithTotal
import com.iyox.vouch.core.model.Vouch

class PersonRepository private constructor(context: Context) {

    private val personDao = AppDatabase.getDatabase(context).personDao()

    suspend fun getPersonsWithTotal(query: String? = null): List<PersonWithTotal> {
        return if (query.isNullOrEmpty()) {
            personDao.getPersonsWithTotalAll()
        } else {
            personDao.getPersonsWithTotalFiltered(query)
        }
    }

    suspend fun onVouchInserted(vouch: Vouch) {
        if (vouch.personName.isNullOrBlank()) return
        personDao.insert(Person(name = vouch.personName.trim()))
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