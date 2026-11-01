package com.iyox.vouch.core.data

import androidx.room.withTransaction
import com.iyox.vouch.core.di.ApplicationScope
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.VouchPersonCrossRef
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VouchRepository @Inject constructor(
    private val database: AppDatabase,
    private val vouchDao: VouchDao,
    private val personDao: PersonDao,
    @ApplicationScope private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    /**
     * Reactive, hot StateFlow of all [VouchWithPerson] entities in memory.
     * Backed by Room's InvalidationTracker and eagerly kept in RAM by [externalScope]
     * for instant 0ms access across screens while preserving SQLite as the single source of truth.
     */
    val vouchesFlow: StateFlow<List<VouchWithPerson>> = combine(
        vouchDao.getAllFlow(),
        vouchDao.getAllPersonRowsFlow()
    ) { vouches, personRows ->
        val personsByVouchId = personRows.groupBy { it.vouchId }
        vouches.map { vouch ->
            VouchWithPerson(
                vouch = vouch,
                persons = personsByVouchId[vouch.id]?.map { it.toPersonWithAmount() } ?: emptyList()
            )
        }
    }.stateIn(
        scope = externalScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    /**
     * Reactive flow of [VouchWithPerson] entities involving a specific person by name.
     */
    fun getVouchesByPersonNameFlow(name: String): Flow<List<VouchWithPerson>> = combine(
        vouchDao.getVouchesByPersonNameFlow(name),
        vouchDao.getPersonRowsForVouchesOfPersonFlow(name)
    ) { vouches, personRows ->
        val personsByVouchId = personRows.groupBy { it.vouchId }
        vouches.map { vouch ->
            VouchWithPerson(
                vouch = vouch,
                persons = personsByVouchId[vouch.id]?.map { it.toPersonWithAmount() } ?: emptyList()
            )
        }
    }

    suspend fun getAll(): List<VouchWithPerson> {
        return database.withTransaction {
            val vouches = vouchDao.getAll()
            val personRows = vouchDao.getAllPersonRows()
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
            val vouches = vouchDao.getVouchesByPersonName(name)
            if (vouches.isEmpty()) return@withTransaction emptyList()
            val personRows = vouchDao.getPersonRowsForVouchesOfPerson(name)
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
            val vouch = vouchDao.getById(id) ?: return@withTransaction null
            VouchWithPerson(
                vouch = vouch,
                persons = vouchDao.getPersonsForVouch(id)
            )
        }
    }

    suspend fun insert(vouchWithPerson: VouchWithPerson): VouchWithPerson {
        val result = database.withTransaction {
            val newVouchId = vouchDao.insert(vouchWithPerson.vouch)

            upsertCrossRefs(newVouchId, vouchWithPerson.persons, deleteExisting = false)

            VouchWithPerson(
                vouch = vouchWithPerson.vouch.copy(id = newVouchId),
                persons = vouchDao.getPersonsForVouch(newVouchId)
            )
        }
        return result
    }

    suspend fun delete(vouchId: Long) {
        database.withTransaction {
            vouchDao.deleteById(vouchId) //also deletes cross refs by cascade
            personDao.deletePersonsWithoutVouches()
        }
    }

    suspend fun deletePersonFromVouch(vouchId: Long, personId: Long) {
        database.withTransaction {
            val currentVouch = vouchDao.getById(vouchId) ?: return@withTransaction
            val currentPersons = vouchDao.getPersonsForVouch(vouchId)
            val remainingPersons = currentPersons.filterNot { it.person.id == personId }

            if (remainingPersons.isEmpty()) {
                vouchDao.deleteById(vouchId)
            } else {
                val newTotalAmount = remainingPersons.sumOf { it.amount }
                vouchDao.update(currentVouch.copy(amount = newTotalAmount))
                upsertCrossRefs(vouchId, remainingPersons, deleteExisting = true)
            }
            personDao.deletePersonsWithoutVouches()
        }
    }

    suspend fun restoreVouch(vouchWithPerson: VouchWithPerson) {
        database.withTransaction {
            val existing = vouchDao.getById(vouchWithPerson.vouch.id)
            if (existing != null) {
                vouchDao.update(vouchWithPerson.vouch)
                upsertCrossRefs(
                    vouchWithPerson.vouch.id,
                    vouchWithPerson.persons,
                    deleteExisting = true
                )
            } else {
                vouchDao.insert(vouchWithPerson.vouch)
                upsertCrossRefs(
                    vouchWithPerson.vouch.id,
                    vouchWithPerson.persons,
                    deleteExisting = false
                )
            }
            personDao.deletePersonsWithoutVouches()
        }
    }

    suspend fun update(newVouch: VouchWithPerson) {
        database.withTransaction {
            vouchDao.update(newVouch.vouch)
            upsertCrossRefs(newVouch.vouch.id, newVouch.persons, deleteExisting = true)
            personDao.deletePersonsWithoutVouches()
        }
    }

    suspend fun setVouchSettled(vouchId: Long, settled: Boolean) {
        database.withTransaction {
            vouchDao.updateCrossRefsSettledByVouchId(vouchId, settled)
        }
    }

    suspend fun setPersonSettlement(vouchId: Long, personId: Long, settled: Boolean) {
        database.withTransaction {
            vouchDao.updateCrossRefSettlement(vouchId, personId, settled)
        }
    }

    suspend fun setPersonSettlements(personId: Long, vouchIds: List<Long>, settled: Boolean) {
        if (vouchIds.isEmpty()) return
        database.withTransaction {
            vouchDao.updateCrossRefsSettledForPerson(personId, vouchIds, settled)
        }
    }

    private suspend fun upsertCrossRefs(
        vouchId: Long,
        persons: List<PersonWithAmount>,
        deleteExisting: Boolean
    ) {
        val existingSettlements = if (deleteExisting) {
            vouchDao.getSettlementsForVouch(vouchId).associateBy(
                { it.personId },
                { it.settled }
            )
        } else {
            emptyMap()
        }

        if (deleteExisting) {
            vouchDao.deleteCrossRefsByVouchId(vouchId)
        }

        val normalizedPersons = persons.mapNotNull {
            val normalizedName = it.person.name.trim()
            if (normalizedName.isBlank()) null else it to normalizedName
        }

        if (normalizedPersons.isEmpty()) {
            return
        }

        val uniqueNames = normalizedPersons.map { it.second }.distinct()
        personDao.insertAll(uniqueNames.map { Person(name = it) })
        val storedPersonsByName = personDao.getByNames(uniqueNames).associateBy { it.name }

        val resolvedPersons = normalizedPersons.mapNotNull { (personWithAmount, normalizedName) ->
            val storedPerson = storedPersonsByName[normalizedName] ?: return@mapNotNull null
            PersonWithAmount(
                person = storedPerson,
                amount = personWithAmount.amount,
                settled = personWithAmount.settled,
                isCustom = personWithAmount.isCustom,
                isSelf = personWithAmount.isSelf
            )
        }

        val crossRefs = resolvedPersons.map { personWithAmount ->
            val preservedSettlement = existingSettlements[personWithAmount.person.id]
            VouchPersonCrossRef(
                vouchId = vouchId,
                personId = personWithAmount.person.id,
                amount = personWithAmount.amount,
                settled = personWithAmount.isSelf || (preservedSettlement ?: personWithAmount.settled),
                isCustom = personWithAmount.isCustom,
                isSelf = personWithAmount.isSelf
            )
        }
        if (crossRefs.isNotEmpty()) {
            vouchDao.insertCrossRefs(crossRefs)
        }
    }
}