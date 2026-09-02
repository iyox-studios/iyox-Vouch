package com.iyox.vouch.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VouchRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: VouchRepository
    private lateinit var database: AppDatabase

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        repository = VouchRepository(database, database.vouchDao(), database.personDao())

        // Clear all data before each test
        database.vouchDao().deleteAll()
        database.personDao().deleteAll()
    }

    @After
    fun tearDown() = runTest {
        database.vouchDao().deleteAll()
        database.personDao().deleteAll()
    }

    @Test
    fun testInsertAndGetAll_singleVouch() = runTest {
        val input = VouchWithPerson(
            vouch = Vouch(amount = 2500L, description = "Dinner", given = true),
            persons = listOf(PersonWithAmount(person = Person(name = "Alice"), amount = 2500L))
        )

        val inserted = repository.insert(input)
        assertTrue(inserted.vouch.id > 0)
        assertEquals(1, inserted.persons.size)
        assertEquals("Alice", inserted.persons[0].person.name)
        assertEquals(2500L, inserted.persons[0].amount)

        val all = repository.getAll()
        assertEquals(1, all.size)
        assertEquals(inserted.vouch.id, all[0].vouch.id)
        assertEquals("Dinner", all[0].vouch.description)
        assertEquals("Alice", all[0].persons[0].person.name)
    }

    @Test
    fun testInsertAndGetAll_groupVouch() = runTest {
        val input = VouchWithPerson(
            vouch = Vouch(amount = 6000L, description = "Hotel", given = true),
            persons = listOf(
                PersonWithAmount(person = Person(name = "Bob"), amount = 3000L),
                PersonWithAmount(person = Person(name = "Charlie"), amount = 3000L)
            )
        )

        val inserted = repository.insert(input)
        assertEquals(2, inserted.persons.size)

        val all = repository.getAll()
        assertEquals(1, all.size)
        assertEquals(2, all[0].persons.size)
        assertEquals(6000L, all[0].vouch.amount)
    }

    @Test
    fun testGetVouchesByPersonName() = runTest {
        val v1 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L, description = "Coffee"),
                persons = listOf(PersonWithAmount(person = Person(name = "Alice"), amount = 1000L))
            )
        )
        val v2 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L, description = "Lunch"),
                persons = listOf(
                    PersonWithAmount(person = Person(name = "Alice"), amount = 1000L),
                    PersonWithAmount(person = Person(name = "Bob"), amount = 1000L)
                )
            )
        )
        val v3 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 3000L, description = "Taxi"),
                persons = listOf(PersonWithAmount(person = Person(name = "Bob"), amount = 3000L))
            )
        )

        val aliceVouches = repository.getVouchesByPersonName("Alice")
        assertEquals(2, aliceVouches.size)
        val aliceIds = aliceVouches.map { it.vouch.id }
        assertTrue(aliceIds.contains(v1.vouch.id))
        assertTrue(aliceIds.contains(v2.vouch.id))
        assertFalse(aliceIds.contains(v3.vouch.id))
    }

    @Test
    fun testGetById_and_GetCachedVouchById() = runTest {
        val inserted = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1234L, description = "Snack"),
                persons = listOf(PersonWithAmount(person = Person(name = "Dave"), amount = 1234L))
            )
        )

        // Query database
        val fromDb = repository.getById(inserted.vouch.id)
        assertNotNull(fromDb)
        assertEquals(1234L, fromDb?.vouch?.amount)
        assertEquals("Snack", fromDb?.vouch?.description)
    }

    @Test
    fun testDeleteVouch_cleansOrphanedPersons() = runTest {
        val inserted = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(person = Person(name = "OrphanPerson"), amount = 1000L))
            )
        )

        assertNotNull(database.personDao().getByName("OrphanPerson"))

        repository.delete(inserted.vouch.id)
        assertNull(repository.getById(inserted.vouch.id))
        // Orphaned person should have been deleted
        assertNull(database.personDao().getByName("OrphanPerson"))
    }

    @Test
    fun testDeletePersonFromVouch_multipleRemaining() = runTest {
        val inserted = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 3000L, description = "Dinner"),
                persons = listOf(
                    PersonWithAmount(person = Person(name = "Alice"), amount = 1000L),
                    PersonWithAmount(person = Person(name = "Bob"), amount = 1000L),
                    PersonWithAmount(person = Person(name = "Charlie"), amount = 1000L)
                )
            )
        )

        val bob = inserted.persons.find { it.person.name == "Bob" }!!
        repository.deletePersonFromVouch(inserted.vouch.id, bob.person.id)

        val updated = repository.getById(inserted.vouch.id)
        assertNotNull(updated)
        assertEquals(2000L, updated?.vouch?.amount) // 3000 - 1000 = 2000
        assertEquals(2, updated?.persons?.size)
        assertFalse(updated?.persons?.any { it.person.name == "Bob" } ?: true)
    }

    @Test
    fun testDeletePersonFromVouch_lastPersonRemovesEntireVouch() = runTest {
        val inserted = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(person = Person(name = "SolePerson"), amount = 1000L))
            )
        )

        val personId = inserted.persons[0].person.id
        repository.deletePersonFromVouch(inserted.vouch.id, personId)

        assertNull(repository.getById(inserted.vouch.id))
        assertNull(database.personDao().getByName("SolePerson"))
    }

    @Test
    fun testUpdateVouch() = runTest {
        val original = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L, description = "Original", given = true),
                persons = listOf(PersonWithAmount(person = Person(name = "Alice"), amount = 1000L))
            )
        )

        val modified = original.copy(
            vouch = original.vouch.copy(amount = 1500L, description = "Updated", given = false),
            persons = listOf(PersonWithAmount(person = Person(name = "Alice"), amount = 1500L))
        )

        repository.update(modified)

        val updated = repository.getById(original.vouch.id)
        assertNotNull(updated)
        assertEquals(1500L, updated?.vouch?.amount)
        assertEquals("Updated", updated?.vouch?.description)
        assertEquals(false, updated?.vouch?.given)
    }

    @Test
    fun testRestoreVouch() = runTest {
        val inserted = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L, description = "Trip"),
                persons = listOf(PersonWithAmount(person = Person(name = "Eve"), amount = 2000L))
            )
        )

        repository.delete(inserted.vouch.id)
        assertNull(repository.getById(inserted.vouch.id))

        repository.restoreVouch(inserted)
        val restored = repository.getById(inserted.vouch.id)
        assertNotNull(restored)
        assertEquals(2000L, restored?.vouch?.amount)
        assertEquals("Eve", restored?.persons?.get(0)?.person?.name)
    }

    @Test
    fun testSetVouchSettled() = runTest {
        val inserted = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(person = Person(name = "Alice"), amount = 1000L))
            )
        )

        repository.setVouchSettled(inserted.vouch.id, true)
        val settlements = database.vouchDao().getSettlementsForVouch(inserted.vouch.id)
        assertTrue(settlements[0].settled)

        repository.setVouchSettled(inserted.vouch.id, false)
        val settlementsUnsettled = database.vouchDao().getSettlementsForVouch(inserted.vouch.id)
        assertFalse(settlementsUnsettled[0].settled)
    }

    @Test
    fun testSetPersonSettlement() = runTest {
        val inserted = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L),
                persons = listOf(
                    PersonWithAmount(person = Person(name = "Alice"), amount = 1000L),
                    PersonWithAmount(person = Person(name = "Bob"), amount = 1000L)
                )
            )
        )

        val aliceId = inserted.persons.find { it.person.name == "Alice" }!!.person.id
        val bobId = inserted.persons.find { it.person.name == "Bob" }!!.person.id

        repository.setPersonSettlement(inserted.vouch.id, aliceId, true)

        val settlements = database.vouchDao().getSettlementsForVouch(inserted.vouch.id)
        assertTrue(settlements.find { it.personId == aliceId }!!.settled)
        assertFalse(settlements.find { it.personId == bobId }!!.settled)
    }

    @Test
    fun testSetPersonSettlements() = runTest {
        val v1 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(person = Person(name = "Alice"), amount = 1000L))
            )
        )
        val v2 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L),
                persons = listOf(PersonWithAmount(person = Person(name = "Alice"), amount = 2000L))
            )
        )

        val aliceId = v1.persons[0].person.id
        repository.setPersonSettlements(aliceId, listOf(v1.vouch.id, v2.vouch.id), true)

        assertTrue(database.vouchDao().getSettlementsForVouch(v1.vouch.id)[0].settled)
        assertTrue(database.vouchDao().getSettlementsForVouch(v2.vouch.id)[0].settled)
    }

    @Test
    fun testVouchesFlow_reactsToInserts() = runTest {
        val initialList = repository.vouchesFlow.first()
        assertTrue(initialList.isEmpty())

        val input = VouchWithPerson(
            vouch = Vouch(amount = 2500L, description = "Dinner", given = true),
            persons = listOf(PersonWithAmount(person = Person(name = "Alice"), amount = 2500L))
        )
        repository.insert(input)

        val updatedList = repository.vouchesFlow.first { it.isNotEmpty() }
        assertEquals(1, updatedList.size)
        assertEquals(2500L, updatedList[0].vouch.amount)
        assertEquals("Alice", updatedList[0].persons[0].person.name)
    }

    @Test
    fun testGetVouchesByPersonNameFlow() = runTest {
        val input = VouchWithPerson(
            vouch = Vouch(amount = 1500L, description = "Lunch", given = true),
            persons = listOf(PersonWithAmount(person = Person(name = "Bob"), amount = 1500L))
        )
        repository.insert(input)

        val bobVouches = repository.getVouchesByPersonNameFlow("Bob").first { it.isNotEmpty() }
        assertEquals(1, bobVouches.size)
        assertEquals("Bob", bobVouches[0].persons[0].person.name)
    }
}
