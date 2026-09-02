package com.iyox.vouch.core.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
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
class SharedVouchActionsTest {

    private lateinit var context: Context
    private lateinit var repository: VouchRepository
    private lateinit var database: AppDatabase
    private lateinit var actions: SharedVouchActions

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        repository = VouchRepository(database, database.vouchDao(), database.personDao())
        actions = SharedVouchActions(repository)

        database.vouchDao().deleteAll()
        database.personDao().deleteAll()
    }

    @After
    fun tearDown() = runTest {
        database.vouchDao().deleteAll()
        database.personDao().deleteAll()
    }

    @Test
    fun testAddVouch() = runTest {
        val vouch = VouchWithPerson(
            vouch = Vouch(amount = 1000L, description = "Test"),
            persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L))
        )

        actions.addVouch(vouch)
        assertEquals(1, repository.getAll().size)
    }

    @Test
    fun testDeleteVouch() = runTest {
        val vouch = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L))
            )
        )

        actions.deleteVouch(vouch.vouch.id)
        assertNull(repository.getById(vouch.vouch.id))
    }

    @Test
    fun testDeletePersonFromVouch() = runTest {
        val vouch = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L),
                persons = listOf(
                    PersonWithAmount(Person(name = "Alice"), 1000L),
                    PersonWithAmount(Person(name = "Bob"), 1000L)
                )
            )
        )
        val aliceId = vouch.persons.find { it.person.name == "Alice" }!!.person.id

        actions.deletePersonFromVouch(vouch.vouch.id, aliceId)

        val updated = repository.getById(vouch.vouch.id)
        assertEquals(1, updated?.persons?.size)
        assertEquals("Bob", updated?.persons?.get(0)?.person?.name)
    }

    @Test
    fun testRestoreVouch() = runTest {
        val vouch = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L))
            )
        )
        repository.delete(vouch.vouch.id)

        actions.restoreVouch(vouch)
        assertNotNull(repository.getById(vouch.vouch.id))
    }

    @Test
    fun testSettleAndUnsettleVouch() = runTest {
        val vouch = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L, settled = false))
            )
        )

        actions.settleVouch(vouch)
        assertTrue(database.vouchDao().getSettlementsForVouch(vouch.vouch.id)[0].settled)

        actions.unSettleVouch(vouch)
        assertFalse(database.vouchDao().getSettlementsForVouch(vouch.vouch.id)[0].settled)
    }

    @Test
    fun testSetVouchPersonSettlement() = runTest {
        val vouch = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L),
                persons = listOf(
                    PersonWithAmount(Person(name = "Alice"), 1000L, settled = false),
                    PersonWithAmount(Person(name = "Bob"), 1000L, settled = false)
                )
            )
        )
        val aliceId = vouch.persons.find { it.person.name == "Alice" }!!.person.id

        actions.setVouchPersonSettlement(vouch.vouch.id, aliceId, true)

        val settlements = database.vouchDao().getSettlementsForVouch(vouch.vouch.id)
        assertTrue(settlements.find { it.personId == aliceId }!!.settled)
    }
}
