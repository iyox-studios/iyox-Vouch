package com.iyox.vouch.core.data

import android.content.Context
import androidx.room.withTransaction
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.VouchPersonCrossRef
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class VouchRepository private constructor(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val vouchDataDao = database.vouchDataDao()
    private val personDataDao = database.personDao()

    private val _dataUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dataUpdates: SharedFlow<Unit> = _dataUpdates.asSharedFlow()

    fun notifyDataChanged() {
        _dataUpdates.tryEmit(Unit)
    }

    suspend fun getAll(): List<VouchWithPerson> {
        return database.withTransaction {
            val vouches = vouchDataDao.getAll()
            val personRows = vouchDataDao.getAllPersonRows()
            val personsByVouchId = personRows.groupBy { it.vouchId }
            vouches.map { vouch ->
                VouchWithPerson(
                    vouch = vouch,
                    persons = personsByVouchId[vouch.id]?.map {
                        PersonWithAmount(
                            person = it.person,
                            amount = it.amount,
                            settled = it.settled
                        )
                    } ?: emptyList()
                )
            }
        }
    }

    suspend fun getVouchesByPersonName(name: String): List<VouchWithPerson> {
        return database.withTransaction {
            val vouches = vouchDataDao.getVouchesByPersonName(name)
            if (vouches.isEmpty()) return@withTransaction emptyList()
            val personRows = vouchDataDao.getPersonRowsForVouchesOfPerson(name)
            val personsByVouchId = personRows.groupBy { it.vouchId }
            vouches.map { vouch ->
                VouchWithPerson(
                    vouch = vouch,
                    persons = personsByVouchId[vouch.id]?.map {
                        PersonWithAmount(
                            person = it.person,
                            amount = it.amount,
                            settled = it.settled
                        )
                    } ?: emptyList()
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
        val result = database.withTransaction {
            val newVouchId = vouchDataDao.insert(vouchWithPerson.vouch).toInt()

            upsertCrossRefs(newVouchId, vouchWithPerson.persons, deleteExisting = false)

            VouchWithPerson(
                vouch = vouchWithPerson.vouch.copy(id = newVouchId),
                persons = vouchDataDao.getPersonsForVouch(newVouchId)
            )
        }
        notifyDataChanged()
        return result
    }

    suspend fun delete(vouchId: Int) {
        database.withTransaction {
            vouchDataDao.deleteById(vouchId) //also deletes cross refs by cascade
            personDataDao.deletePersonsWithoutVouches()
        }
        notifyDataChanged()
    }

    suspend fun deletePersonFromVouch(vouchId: Int, personId: Int) {
        database.withTransaction {
            val currentVouch = vouchDataDao.getById(vouchId) ?: return@withTransaction
            val currentPersons = vouchDataDao.getPersonsForVouch(vouchId)
            val remainingPersons = currentPersons.filterNot { it.person.id == personId }

            if (remainingPersons.isEmpty()) {
                vouchDataDao.deleteById(vouchId)
            } else {
                val newTotalAmount = remainingPersons.sumOf { it.amount }
                vouchDataDao.update(currentVouch.copy(amount = newTotalAmount))
                upsertCrossRefs(vouchId, remainingPersons, deleteExisting = true)
            }
            personDataDao.deletePersonsWithoutVouches()
        }
        notifyDataChanged()
    }

    suspend fun restoreVouch(vouchWithPerson: VouchWithPerson) {
        database.withTransaction {
            val existing = vouchDataDao.getById(vouchWithPerson.vouch.id)
            if (existing != null) {
                vouchDataDao.update(vouchWithPerson.vouch)
                upsertCrossRefs(
                    vouchWithPerson.vouch.id,
                    vouchWithPerson.persons,
                    deleteExisting = true
                )
            } else {
                vouchDataDao.insert(vouchWithPerson.vouch)
                upsertCrossRefs(
                    vouchWithPerson.vouch.id,
                    vouchWithPerson.persons,
                    deleteExisting = false
                )
            }
            personDataDao.deletePersonsWithoutVouches()
        }
        notifyDataChanged()
    }

    suspend fun update(newVouch: VouchWithPerson) {
        database.withTransaction {
            vouchDataDao.update(newVouch.vouch)
            upsertCrossRefs(newVouch.vouch.id, newVouch.persons, deleteExisting = true)
            personDataDao.deletePersonsWithoutVouches()
        }
        notifyDataChanged()
    }

    suspend fun setVouchSettled(vouchId: Int, settled: Boolean) {
        database.withTransaction {
            vouchDataDao.updateCrossRefsSettledByVouchId(vouchId, settled)
        }
        notifyDataChanged()
    }

    suspend fun setPersonSettlement(vouchId: Int, personId: Int, settled: Boolean) {
        database.withTransaction {
            vouchDataDao.updateCrossRefSettlement(vouchId, personId, settled)
        }
        notifyDataChanged()
    }

    suspend fun setPersonSettled(personId: Int, settled: Boolean) {
        database.withTransaction {
            vouchDataDao.updateCrossRefsSettledByPersonId(personId, settled)
        }
        notifyDataChanged()
    }

    suspend fun setPersonSettlements(personId: Int, vouchIds: List<Int>, settled: Boolean) {
        if (vouchIds.isEmpty()) return
        database.withTransaction {
            vouchDataDao.updateCrossRefsSettledForPerson(personId, vouchIds, settled)
        }
        notifyDataChanged()
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

        val normalizedPersons = persons.mapNotNull {
            val normalizedName = it.person.name.trim()
            if (normalizedName.isBlank()) null else it to normalizedName
        }

        if (normalizedPersons.isEmpty()) {
            return
        }

        val uniqueNames = normalizedPersons.map { it.second }.distinct()
        personDataDao.insertAll(uniqueNames.map { Person(name = it) })
        val storedPersonsByName = personDataDao.getByNames(uniqueNames).associateBy { it.name }

        val resolvedPersons = normalizedPersons.mapNotNull { (personWithAmount, normalizedName) ->
            val storedPerson = storedPersonsByName[normalizedName] ?: return@mapNotNull null
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