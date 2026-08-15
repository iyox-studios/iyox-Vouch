package com.iyox.vouch.core.data

import android.content.Context
import com.iyox.vouch.core.model.PersonTotal

class PersonRepository private constructor(context: Context) {

    private val personDao = AppDatabase.getDatabase(context).personDao()

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