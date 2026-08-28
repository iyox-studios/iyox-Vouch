package com.iyox.vouch.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonDao
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchDao
import com.iyox.vouch.core.model.VouchPersonCrossRef
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.sql.Timestamp

@RunWith(AndroidJUnit4::class)
class PersonDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var personDao: PersonDao
    private lateinit var vouchDao: VouchDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        personDao = database.personDao()
        vouchDao = database.vouchDataDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetByName() = runTest {
        val person = Person(name = "Alice")
        val id = personDao.insert(person)
        assertTrue(id > 0)

        val retrieved = personDao.getByName("Alice")
        assertNotNull(retrieved)
        assertEquals("Alice", retrieved?.name)
        assertEquals(id.toInt(), retrieved?.id)
    }

    @Test
    fun insertDuplicateName_ignored() = runTest {
        val id1 = personDao.insert(Person(name = "Bob"))
        assertTrue(id1 > 0)

        // Attempt duplicate insert
        val id2 = personDao.insert(Person(name = "Bob"))
        assertEquals(-1L, id2)

        val retrieved = personDao.getByName("Bob")
        assertEquals(id1.toInt(), retrieved?.id)
    }

    @Test
    fun insertAllAndGetByNames() = runTest {
        val persons = listOf(
            Person(name = "Charlie"),
            Person(name = "Dave"),
            Person(name = "Eve")
        )
        val ids = personDao.insertAll(persons)
        assertEquals(3, ids.size)

        val retrieved = personDao.getByNames(listOf("Charlie", "Eve", "NonExistent"))
        assertEquals(2, retrieved.size)
        val names = retrieved.map { it.name }
        assertTrue(names.contains("Charlie"))
        assertTrue(names.contains("Eve"))
    }

    @Test
    fun deleteAll() = runTest {
        personDao.insert(Person(name = "Alice"))
        personDao.insert(Person(name = "Bob"))

        personDao.deleteAll()
        assertNull(personDao.getByName("Alice"))
        assertNull(personDao.getByName("Bob"))
    }

    @Test
    fun deletePersonsWithoutVouches() = runTest {
        val aliceId = personDao.insert(Person(name = "Alice")).toInt()
        val bobId = personDao.insert(Person(name = "Bob")).toInt()
        val charlieId = personDao.insert(Person(name = "Charlie")).toInt()

        // Create vouch only for Alice and Bob
        val vouchId = vouchDao.insert(Vouch(amount = 2000L, given = true)).toInt()
        vouchDao.insertCrossRefs(
            listOf(
                VouchPersonCrossRef(vouchId = vouchId, personId = aliceId, amount = 1000L),
                VouchPersonCrossRef(vouchId = vouchId, personId = bobId, amount = 1000L)
            )
        )

        // Charlie has no vouches, should be deleted
        personDao.deletePersonsWithoutVouches()

        assertNotNull(personDao.getByName("Alice"))
        assertNotNull(personDao.getByName("Bob"))
        assertNull(personDao.getByName("Charlie"))
    }

    @Test
    fun getPersonTotalByName_calculations() = runTest {
        val aliceId = personDao.insert(Person(name = "Alice")).toInt()

        // 1. Given active vouch (I paid 30.00 EUR for Alice -> Alice owes me 30.00 EUR -> +3000)
        val v1 = vouchDao.insert(Vouch(amount = 3000L, given = true, timestamp = Timestamp(1000L))).toInt()
        vouchDao.insertCrossRefs(listOf(VouchPersonCrossRef(vouchId = v1, personId = aliceId, amount = 3000L, settled = false)))

        // 2. Received active vouch (Alice paid 10.00 EUR for me -> I owe Alice 10.00 EUR -> -1000)
        val v2 = vouchDao.insert(Vouch(amount = 1000L, given = false, timestamp = Timestamp(2000L))).toInt()
        vouchDao.insertCrossRefs(listOf(VouchPersonCrossRef(vouchId = v2, personId = aliceId, amount = 1000L, settled = false)))

        // 3. Settled vouch (should NOT count in amount, should NOT count in active vouchCount)
        val v3 = vouchDao.insert(Vouch(amount = 5000L, given = true, timestamp = Timestamp(3000L))).toInt()
        vouchDao.insertCrossRefs(listOf(VouchPersonCrossRef(vouchId = v3, personId = aliceId, amount = 5000L, settled = true)))

        val total = personDao.getPersonTotalByName("Alice")
        assertNotNull(total)
        assertEquals("Alice", total?.person?.name)
        assertEquals(2000L, total?.amount) // 3000 - 1000 = 2000
        assertEquals(2, total?.vouchCount) // v1 and v2, excluding settled v3
    }

    @Test
    fun getPersonTotalByName_nonExistent() = runTest {
        val total = personDao.getPersonTotalByName("Nobody")
        assertNull(total)
    }

    @Test
    fun getPersonsTotalAll_multiplePersons() = runTest {
        val aliceId = personDao.insert(Person(name = "Alice")).toInt()
        val bobId = personDao.insert(Person(name = "Bob")).toInt()

        val v1 = vouchDao.insert(Vouch(amount = 5000L, given = true)).toInt()
        vouchDao.insertCrossRefs(
            listOf(
                VouchPersonCrossRef(vouchId = v1, personId = aliceId, amount = 2500L, settled = false),
                VouchPersonCrossRef(vouchId = v1, personId = bobId, amount = 2500L, settled = false)
            )
        )

        val totals = personDao.getPersonsTotalAll()
        assertEquals(2, totals.size)

        val aliceTotal = totals.find { it.person.name == "Alice" }
        assertNotNull(aliceTotal)
        assertEquals(2500L, aliceTotal?.amount)
        assertEquals(1, aliceTotal?.vouchCount)

        val bobTotal = totals.find { it.person.name == "Bob" }
        assertNotNull(bobTotal)
        assertEquals(2500L, bobTotal?.amount)
        assertEquals(1, bobTotal?.vouchCount)
    }

    @Test
    fun getPersonsTotalFiltered_queryMatching() = runTest {
        val aliceId = personDao.insert(Person(name = "Alice Wonderland")).toInt()
        val bobId = personDao.insert(Person(name = "Bob Smith")).toInt()

        val v1 = vouchDao.insert(Vouch(amount = 1000L, given = true)).toInt()
        vouchDao.insertCrossRefs(listOf(VouchPersonCrossRef(vouchId = v1, personId = aliceId, amount = 1000L)))

        val v2 = vouchDao.insert(Vouch(amount = 2000L, given = true)).toInt()
        vouchDao.insertCrossRefs(listOf(VouchPersonCrossRef(vouchId = v2, personId = bobId, amount = 2000L)))

        val results = personDao.getPersonsTotalFiltered("Alice")
        assertEquals(1, results.size)
        assertEquals("Alice Wonderland", results[0].person.name)

        val emptyResults = personDao.getPersonsTotalFiltered("Zzz")
        assertTrue(emptyResults.isEmpty())
    }

    @Test
    fun getPersonsByTotalVouches_ordering() = runTest {
        val aliceId = personDao.insert(Person(name = "Alice")).toInt()
        val bobId = personDao.insert(Person(name = "Bob")).toInt()
        val charlieId = personDao.insert(Person(name = "Charlie")).toInt()

        // Bob: 3 vouches (including settled)
        for (i in 1..3) {
            val v = vouchDao.insert(Vouch(amount = 1000L, given = true)).toInt()
            vouchDao.insertCrossRefs(listOf(VouchPersonCrossRef(vouchId = v, personId = bobId, amount = 1000L, settled = (i == 3))))
        }

        // Alice: 2 vouches
        for (i in 1..2) {
            val v = vouchDao.insert(Vouch(amount = 1000L, given = true)).toInt()
            vouchDao.insertCrossRefs(listOf(VouchPersonCrossRef(vouchId = v, personId = aliceId, amount = 1000L)))
        }

        // Charlie: 0 vouches
        val ordered = personDao.getPersonsByTotalVouches()
        assertEquals(3, ordered.size)
        assertEquals("Bob", ordered[0].person.name)
        assertEquals(3, ordered[0].vouchCount)
        assertEquals("Alice", ordered[1].person.name)
        assertEquals(2, ordered[1].vouchCount)
        assertEquals("Charlie", ordered[2].person.name)
        assertEquals(0, ordered[2].vouchCount)
    }

    @Test
    fun getById_and_update() = runTest {
        val id = personDao.insert(Person(name = "Alice")).toInt()
        val person = personDao.getById(id)
        assertNotNull(person)
        assertEquals("Alice", person?.name)

        // Update name
        personDao.update(person!!.copy(name = "Alicia"))
        val updated = personDao.getById(id)
        assertNotNull(updated)
        assertEquals("Alicia", updated?.name)
        assertNull(personDao.getByName("Alice"))
        assertNotNull(personDao.getByName("Alicia"))

        // Update via updateName query
        personDao.updateName(id, "Alice Smith")
        val updated2 = personDao.getById(id)
        assertEquals("Alice Smith", updated2?.name)
    }
}
