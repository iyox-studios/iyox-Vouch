package com.iyox.vouch.core.data

import androidx.room.withTransaction
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.VouchPersonCrossRef
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VouchRepository @Inject constructor(
    private val database: AppDatabase,
    private val vouchDataDao: VouchDao,
    private val personDataDao: PersonDao
) {

    /**
     * Reactive flow of all [VouchWithPerson] entities, backed by Room's InvalidationTracker.
     * Emits automatically whenever any Vouch, Person, or cross-reference changes.
     */
    val vouchesFlow: Flow<List<VouchWithPerson>> = combine(
        vouchDataDao.getAllFlow(),
        vouchDataDao.getAllPersonRowsFlow()
    ) { vouches, personRows ->
        val personsByVouchId = personRows.groupBy { it.vouchId }
        vouches.map { vouch ->
            VouchWithPerson(
                vouch = vouch,
                persons = personsByVouchId[vouch.id]?.map { it.toPersonWithAmount() } ?: emptyList()
            )
        }
    }

    /**
     * Reactive flow of [VouchWithPerson] entities involving a specific person by name.
     */
    fun getVouchesByPersonNameFlow(name: String): Flow<List<VouchWithPerson>> = combine(
        vouchDataDao.getVouchesByPersonNameFlow(name),
        vouchDataDao.getPersonRowsForVouchesOfPersonFlow(name)
    ) { vouches, personRows ->
        val personsByVouchId = personRows.groupBy { it.vouchId }
        vouches.map { vouch ->
            VouchWithPerson(
                vouch = vouch,
                persons = personsByVouchId[vouch.id]?.map { it.toPersonWithAmount() } ?: emptyList()
            )
        }
    }

    /**
     * Emits whenever the underlying vouches data changes. Maintained for backwards compatibility.
     */
    val dataUpdates: Flow<Unit> = vouchesFlow.map { }

    fun notifyDataChanged() {
        // No-op: Room's InvalidationTracker automatically notifies all active Flow collectors.
    }

    suspend fun getAll(): List<VouchWithPerson> {
        return database.withTransaction {
            val vouches = vouchDataDao.getAll()
            val personRows = vouchDataDao.getAllPersonRows()
            val personsByVouchId = personRows.groupBy { it.vouchId }
            vouches.map { vouch ->
                VouchWithPerson(
                    vouch = vouch,
                    persons = personsByVouchId[vouch.id]?.map { it.toPersonWithAmount() } ?: emptyList()
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
                    persons = personsByVouchId[vouch.id]?.map { it.toPersonWithAmount() } ?: emptyList()
                )
            }
        }
    }

    suspend fun getById(id: Long): VouchWithPerson? {
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
            val newVouchId = vouchDataDao.insert(vouchWithPerson.vouch)

            upsertCrossRefs(newVouchId, vouchWithPerson.persons, deleteExisting = false)

            VouchWithPerson(
                vouch = vouchWithPerson.vouch.copy(id = newVouchId),
                persons = vouchDataDao.getPersonsForVouch(newVouchId)
            )
        }
        return result
    }

    suspend fun delete(vouchId: Long) {
        database.withTransaction {
            vouchDataDao.deleteById(vouchId) // also deletes cross refs by cascade
            personDataDao.deletePersonsWithoutVouches()
        }
    }

    suspend fun deletePersonFromVouch(vouchId: Long, personId: Long) {
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
    }

    suspend fun update(newVouch: VouchWithPerson) {
        database.withTransaction {
            vouchDataDao.update(newVouch.vouch)
            upsertCrossRefs(newVouch.vouch.id, newVouch.persons, deleteExisting = true)
            personDataDao.deletePersonsWithoutVouches()
        }
    }

    suspend fun setVouchSettled(vouchId: Long, settled: Boolean) {
        database.withTransaction {
            vouchDataDao.updateCrossRefsSettledByVouchId(vouchId, settled)
        }
    }

    suspend fun setPersonSettlement(vouchId: Long, personId: Long, settled: Boolean) {
        database.withTransaction {
            vouchDataDao.updateCrossRefSettlement(vouchId, personId, settled)
        }
    }

    suspend fun setPersonSettled(personId: Long, settled: Boolean) {
        database.withTransaction {
            vouchDataDao.updateCrossRefsSettledByPersonId(personId, settled)
        }
    }

    suspend fun setPersonSettlements(personId: Long, vouchIds: List<Long>, settled: Boolean) {
        if (vouchIds.isEmpty()) return
        database.withTransaction {
            vouchDataDao.updateCrossRefsSettledForPerson(personId, vouchIds, settled)
        }
    }

    private suspend fun upsertCrossRefs(
        vouchId: Long,
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
}