package com.iyox.vouch.model

import android.content.Context
import java.math.BigDecimal

class VouchRepository private constructor(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val vouchDataDao = database.vouchDataDao()
    private val personDataDao = database.personDao()
    private val personRepository = PersonRepository.getInstance(context)

    suspend fun getAll(): List<Vouch> {
        return vouchDataDao.getAll()
    }

    suspend fun insert(vouch: Vouch) {
        vouchDataDao.insert(vouch)
        personRepository.onVouchInserted(vouch)
    }

    suspend fun delete(vouch: Vouch) {
        vouchDataDao.delete(vouch)
        if (vouch.personName == null) return
        if (personDataDao.getVouchCount(vouch.personName) == 0) {
            personDataDao.deleteByName(vouch.personName)
        }
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