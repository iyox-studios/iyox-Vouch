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

    @Volatile
    private var cachedVouches: List<VouchWithPerson>? = null

    @Volatile
    private var cachedVouchesByPerson: Map<String, List<VouchWithPerson>> = emptyMap()

    fun getCachedVouchesByPerson(name: String): List<VouchWithPerson>? =
        cachedVouchesByPerson[name]

    fun notifyDataChanged() {
        cachedVouches = null
        cachedVouchesByPerson = emptyMap()
        _dataUpdates.tryEmit(Unit)
    }

    fun getCachedVouches(): List<VouchWithPerson>? = cachedVouches

    suspend fun getAll(): List<VouchWithPerson> {
        cachedVouches?.let { return it }
        val result = database.withTransaction {
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
        cachedVouches = result

        // Pre-warm the per-person cache for free — data is already in memory.
        // Every person the user might tap on PersonsScreen or VouchesScreen will
        // open instantly without a spinner on first entry.
        val byPerson = mutableMapOf<String, MutableList<VouchWithPerson>>()
        result.forEach { vouch ->
            vouch.persons.forEach { personWithAmount ->
                byPerson.getOrPut(personWithAmount.person.name) { mutableListOf() }.add(vouch)
            }
        }
        cachedVouchesByPerson = byPerson

        return result
    }

    suspend fun getVouchesByPersonName(name: String): List<VouchWithPerson> {
        val result = database.withTransaction {
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
        cachedVouchesByPerson = cachedVouchesByPerson + (name to result)
        return result
    }

    fun getCachedVouchById(id: Int): VouchWithPerson? {
        cachedVouches?.firstOrNull { it.vouch.id == id }?.let { return it }
        for (personVouches in cachedVouchesByPerson.values) {
            personVouches.firstOrNull { it.vouch.id == id }?.let { return it }
        }
        return null
    }

    suspend fun getById(id: Int): VouchWithPerson? {
        getCachedVouchById(id)?.let { return it }
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