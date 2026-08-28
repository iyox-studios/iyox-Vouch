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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.sql.Timestamp

@RunWith(AndroidJUnit4::class)
class VouchDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var vouchDao: VouchDao
    private lateinit var personDao: PersonDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        vouchDao = database.vouchDataDao()
        personDao = database.personDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val vouch = Vouch(amount = 1500L, description = "Lunch", given = true)
        val id = vouchDao.insert(vouch).toInt()
        assertTrue(id > 0)

        val retrieved = vouchDao.getById(id)
        assertNotNull(retrieved)
        assertEquals(1500L, retrieved?.amount)
        assertEquals("Lunch", retrieved?.description)
        assertEquals(true, retrieved?.given)
    }

    @Test
    fun updateVouch() = runTest {
        val id = vouchDao.insert(Vouch(amount = 1000L, description = "Initial")).toInt()
        val original = vouchDao.getById(id)!!

        vouchDao.update(original.copy(amount = 2000L, description = "Updated", given = false))

        val updated = vouchDao.getById(id)
        assertNotNull(updated)
        assertEquals(2000L, updated?.amount)
        assertEquals("Updated", updated?.description)
        assertEquals(false, updated?.given)
    }

    @Test
    fun deleteVouchEntity() = runTest {
        val id = vouchDao.insert(Vouch(amount = 1000L)).toInt()
        val vouch = vouchDao.getById(id)!!

        vouchDao.delete(vouch)
        assertNull(vouchDao.getById(id))
    }

    @Test
    fun deleteById() = runTest {
        val id = vouchDao.insert(Vouch(amount = 1000L)).toInt()
        vouchDao.deleteById(id)
        assertNull(vouchDao.getById(id))
    }

    @Test
    fun deleteAll() = runTest {
        vouchDao.insert(Vouch(amount = 1000L))
        vouchDao.insert(Vouch(amount = 2000L))

        assertEquals(2, vouchDao.getAll().size)
        vouchDao.deleteAll()
        assertEquals(0, vouchDao.getAll().size)
    }

    @Test
    fun cascadeDelete_whenVouchDeleted_crossRefsDeleted() = runTest {
        val personId = personDao.insert(Person(name = "Alice")).toInt()
        val vouchId = vouchDao.insert(Vouch(amount = 1000L)).toInt()

        vouchDao.insertCrossRefs(listOf(VouchPersonCrossRef(vouchId = vouchId, personId = personId, amount = 1000L)))
        assertEquals(1, vouchDao.getPersonsForVouch(vouchId).size)

        // Delete vouch -> cross ref should be cascade deleted
        vouchDao.deleteById(vouchId)
        assertEquals(0, vouchDao.getPersonsForVouch(vouchId).size)
    }

    @Test
    fun cascadeDelete_whenPersonDeleted_crossRefsDeleted() = runTest {
        val personId = personDao.insert(Person(name = "Bob")).toInt()
        val vouchId = vouchDao.insert(Vouch(amount = 1000L)).toInt()

        vouchDao.insertCrossRefs(listOf(VouchPersonCrossRef(vouchId = vouchId, personId = personId, amount = 1000L)))
        assertEquals(1, vouchDao.getPersonsForVouch(vouchId).size)

        // Delete person -> cross ref should be cascade deleted
        personDao.deleteAll()
        assertEquals(0, vouchDao.getPersonsForVouch(vouchId).size)
    }

    @Test
    fun getVouchesByPersonName_orderedByTimestampDesc() = runTest {
        val aliceId = personDao.insert(Person(name = "Alice")).toInt()
        val bobId = personDao.insert(Person(name = "Bob")).toInt()

        val v1 = vouchDao.insert(Vouch(amount = 1000L, timestamp = Timestamp(1000L))).toInt()
        val v2 = vouchDao.insert(Vouch(amount = 2000L, timestamp = Timestamp(3000L))).toInt()
        val v3 = vouchDao.insert(Vouch(amount = 3000L, timestamp = Timestamp(2000L))).toInt()

        vouchDao.insertCrossRefs(
            listOf(
                VouchPersonCrossRef(vouchId = v1, personId = aliceId, amount = 1000L),
                VouchPersonCrossRef(vouchId = v2, personId = aliceId, amount = 2000L),
                VouchPersonCrossRef(vouchId = v3, personId = bobId, amount = 3000L) // Only Bob
            )
        )

        val aliceVouches = vouchDao.getVouchesByPersonName("Alice")
        assertEquals(2, aliceVouches.size)
        assertEquals(v2, aliceVouches[0].id) // Timestamp 3000L
        assertEquals(v1, aliceVouches[1].id) // Timestamp 1000L
    }

    @Test
    fun getPersonsForVouch_and_getSettlementsForVouch() = runTest {
        val p1 = personDao.insert(Person(name = "Alice")).toInt()
        val p2 = personDao.insert(Person(name = "Bob")).toInt()
        val v1 = vouchDao.insert(Vouch(amount = 3000L)).toInt()

        vouchDao.insertCrossRefs(
            listOf(
                VouchPersonCrossRef(vouchId = v1, personId = p1, amount = 2000L, settled = false),
                VouchPersonCrossRef(vouchId = v1, personId = p2, amount = 1000L, settled = true)
            )
        )

        val persons = vouchDao.getPersonsForVouch(v1)
        assertEquals(2, persons.size)

        val settlements = vouchDao.getSettlementsForVouch(v1)
        assertEquals(2, settlements.size)
        val aliceSettlement = settlements.find { it.personId == p1 }
        val bobSettlement = settlements.find { it.personId == p2 }
        assertFalse(aliceSettlement!!.settled)
        assertTrue(bobSettlement!!.settled)
    }

    @Test
    fun updateCrossRefSettlement() = runTest {
        val p1 = personDao.insert(Person(name = "Alice")).toInt()
        val v1 = vouchDao.insert(Vouch(amount = 1000L)).toInt()

        vouchDao.insertCrossRefs(listOf(VouchPersonCrossRef(vouchId = v1, personId = p1, amount = 1000L, settled = false)))
        assertFalse(vouchDao.getSettlementsForVouch(v1)[0].settled)

        vouchDao.updateCrossRefSettlement(v1, p1, true)
        assertTrue(vouchDao.getSettlementsForVouch(v1)[0].settled)

        vouchDao.updateCrossRefSettlement(v1, p1, false)
        assertFalse(vouchDao.getSettlementsForVouch(v1)[0].settled)
    }

    @Test
    fun updateCrossRefsSettledByVouchId() = runTest {
        val p1 = personDao.insert(Person(name = "Alice")).toInt()
        val p2 = personDao.insert(Person(name = "Bob")).toInt()
        val v1 = vouchDao.insert(Vouch(amount = 2000L)).toInt()

        vouchDao.insertCrossRefs(
            listOf(
                VouchPersonCrossRef(vouchId = v1, personId = p1, amount = 1000L, settled = false),
                VouchPersonCrossRef(vouchId = v1, personId = p2, amount = 1000L, settled = false)
            )
        )

        vouchDao.updateCrossRefsSettledByVouchId(v1, true)
        val settlements = vouchDao.getSettlementsForVouch(v1)
        assertTrue(settlements.all { it.settled })
    }

    @Test
    fun updateCrossRefsSettledForPerson() = runTest {
        val p1 = personDao.insert(Person(name = "Alice")).toInt()
        val v1 = vouchDao.insert(Vouch(amount = 1000L)).toInt()
        val v2 = vouchDao.insert(Vouch(amount = 2000L)).toInt()
        val v3 = vouchDao.insert(Vouch(amount = 3000L)).toInt()

        vouchDao.insertCrossRefs(
            listOf(
                VouchPersonCrossRef(vouchId = v1, personId = p1, amount = 1000L, settled = false),
                VouchPersonCrossRef(vouchId = v2, personId = p1, amount = 2000L, settled = false),
                VouchPersonCrossRef(vouchId = v3, personId = p1, amount = 3000L, settled = false)
            )
        )

        // Settle only v1 and v2 for Alice
        vouchDao.updateCrossRefsSettledForPerson(p1, listOf(v1, v2), true)

        assertTrue(vouchDao.getSettlementsForVouch(v1)[0].settled)
        assertTrue(vouchDao.getSettlementsForVouch(v2)[0].settled)
        assertFalse(vouchDao.getSettlementsForVouch(v3)[0].settled)
    }

    @Test
    fun updateCrossRefsSettledByPersonId() = runTest {
        val p1 = personDao.insert(Person(name = "Alice")).toInt()
        val v1 = vouchDao.insert(Vouch(amount = 1000L)).toInt()
        val v2 = vouchDao.insert(Vouch(amount = 2000L)).toInt()

        vouchDao.insertCrossRefs(
            listOf(
                VouchPersonCrossRef(vouchId = v1, personId = p1, amount = 1000L, settled = false),
                VouchPersonCrossRef(vouchId = v2, personId = p1, amount = 2000L, settled = false)
            )
        )

        vouchDao.updateCrossRefsSettledByPersonId(p1, true)
        assertTrue(vouchDao.getSettlementsForVouch(v1)[0].settled)
        assertTrue(vouchDao.getSettlementsForVouch(v2)[0].settled)
    }
}
