package com.iyox.vouch.core.data

import android.content.Context
import androidx.room.withTransaction
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.VouchPersonCrossRef
import com.iyox.vouch.core.model.VouchWithPerson

class VouchRepository private constructor(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val vouchDataDao = database.vouchDataDao()
    private val personDataDao = database.personDao()

    suspend fun getAll(): List<VouchWithPerson> {
        return database.withTransaction {
            vouchDataDao.getAll().map { vouch ->
                VouchWithPerson(
                    vouch = vouch,
                    persons = vouchDataDao.getPersonsForVouch(vouch.id)
                )
            }
        }
    }

    suspend fun getById(id: Int): VouchWithPerson? {
        return database.withTransaction {
            val vouch = vouchDataDao.getById(id) ?: return@withTransaction null
            VouchWithPerson(
                vouch = vouch,
                persons = vouchDataDao.getPersonsForVouch(id)
            )
        }
    }

    suspend fun insert(vouchWithPerson: VouchWithPerson): VouchWithPerson {
        return database.withTransaction {
            val newVouchId = vouchDataDao.insert(vouchWithPerson.vouch).toInt()

            upsertCrossRefs(newVouchId, vouchWithPerson.persons, deleteExisting = false)

            VouchWithPerson(
                vouch = vouchWithPerson.vouch.copy(id = newVouchId),
                persons = vouchDataDao.getPersonsForVouch(newVouchId)
            )
        }
    }

    suspend fun delete(vouchId: Int) {
        database.withTransaction {
            vouchDataDao.deleteById(vouchId) //also deletes cross refs by cascade
            personDataDao.deletePersonsWithoutVouches()
        }
    }

    suspend fun update(newVouch: VouchWithPerson) {
        database.withTransaction {
            vouchDataDao.update(newVouch.vouch)
            upsertCrossRefs(newVouch.vouch.id, newVouch.persons, deleteExisting = true)
            personDataDao.deletePersonsWithoutVouches()
        }
    }

    suspend fun setVouchSettled(vouchId: Int, settled: Boolean) {
        database.withTransaction {
            vouchDataDao.updateCrossRefsSettledByVouchId(vouchId, settled)
        }
    }

    suspend fun setPersonSettlement(vouchId: Int, personId: Int, settled: Boolean) {
        database.withTransaction {
            vouchDataDao.updateCrossRefSettlement(vouchId, personId, settled)
        }
    }

    private suspend fun upsertCrossRefs(
        vouchId: Int,
        persons: List<PersonWithAmount>,
        deleteExisting: Boolean
    ) {
        val existingSettlements = if (deleteExisting) {
            vouchDataDao.getSettlementsForVouch(vouchId).associateBy(
                { it.personId },
                { it.settled }
            )
        } else {
            emptyMap()
        }

        if (deleteExisting) {
            vouchDataDao.deleteCrossRefsByVouchId(vouchId)
        }

        val resolvedPersons = persons.mapNotNull { personWithAmount ->
            val normalizedName = personWithAmount.person.name.trim()
            if (normalizedName.isBlank()) {
                return@mapNotNull null
            }
            personDataDao.insert(Person(name = normalizedName))
            val storedPerson = personDataDao.getByName(normalizedName)
                ?: error("Failed to resolve person: $normalizedName")
            PersonWithAmount(
                person = storedPerson,
                amount = personWithAmount.amount,
                settled = personWithAmount.settled
            )
        }

        val crossRefs = resolvedPersons.map { personWithAmount ->
            val preservedSettlement = existingSettlements[personWithAmount.person.id]
            VouchPersonCrossRef(
                vouchId = vouchId,
                personId = personWithAmount.person.id,
                amount = personWithAmount.amount,
                settled = preservedSettlement ?: personWithAmount.settled
            )
        }
        if (crossRefs.isNotEmpty()) {
            vouchDataDao.insertCrossRefs(crossRefs)
        }
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