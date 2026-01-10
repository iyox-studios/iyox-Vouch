package com.iyox.vouch.model

import android.content.Context
class VouchRepository private constructor(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val vouchDataDao = database.vouchDataDao()

    suspend fun getAll(): List<Vouch> {
        return vouchDataDao.getAll()
    }

    suspend fun insert(vouch: Vouch, name: String? = null) {
        vouchDataDao.insert(vouch)
        val personID = vouch.personID
        if (personID == null)
        {
            val split = name?.split(" ")
            val firstName = split?.getOrNull(0)
            val secondName = split?.getOrNull(1)

            if (firstName != null && secondName != null) {
                val person = Person(firstName = firstName, secondName = secondName)
                database.personDao().insert(person)
            }
        }
    }

    suspend fun delete(vouch: Vouch) {
        vouchDataDao.delete(vouch)
        //delete person if last vouch
    }

    suspend fun update(vouch: Vouch) {
        vouchDataDao.update(vouch)
    }

    companion object {
        @Volatile
        private var INSTANCE: VouchRepository? = null

        fun getInstance(context: Context): VouchRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = VouchRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}