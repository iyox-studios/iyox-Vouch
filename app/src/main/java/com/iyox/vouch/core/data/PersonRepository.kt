package com.iyox.vouch.core.data

import android.content.Context

class PersonRepository private constructor(context: Context) {

    private val personDao = AppDatabase.getDatabase(context).personDao()


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