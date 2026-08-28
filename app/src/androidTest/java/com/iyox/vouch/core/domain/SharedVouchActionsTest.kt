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

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        repository = VouchRepository.getInstance(context)

        database.vouchDataDao().deleteAll()
        database.personDao().deleteAll()
        repository.notifyDataChanged()
    }

    @After
    fun tearDown() = runTest {
        database.vouchDataDao().deleteAll()
        database.personDao().deleteAll()
        repository.notifyDataChanged()
    }

    @Test
    fun testAddVouch_invokesCallback() = runTest {
        var callbackInvoked = 0
        val actions = SharedVouchActions(repository) { callbackInvoked++ }

        val vouch = VouchWithPerson(
            vouch = Vouch(amount = 1000L, description = "Test"),
            persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L))
        )

        actions.addVouch(vouch)
        assertEquals(1, callbackInvoked)
        assertEquals(1, repository.getAll().size)
    }

    @Test
    fun testDeleteVouch_invokesCallback() = runTest {
        val vouch = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L))
            )
        )

        var callbackInvoked = 0
        val actions = SharedVouchActions(repository) { callbackInvoked++ }

        actions.deleteVouch(vouch.vouch.id)
        assertEquals(1, callbackInvoked)
        assertNull(repository.getById(vouch.vouch.id))
    }

    @Test
    fun testDeletePersonFromVouch_invokesCallback() = runTest {
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

        var callbackInvoked = 0
        val actions = SharedVouchActions(repository) { callbackInvoked++ }

        actions.deletePersonFromVouch(vouch.vouch.id, aliceId)
        assertEquals(1, callbackInvoked)

        val updated = repository.getById(vouch.vouch.id)
        assertEquals(1, updated?.persons?.size)
        assertEquals("Bob", updated?.persons?.get(0)?.person?.name)
    }

    @Test
    fun testRestoreVouch_invokesCallback() = runTest {
        val vouch = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L))
            )
        )
        repository.delete(vouch.vouch.id)

        var callbackInvoked = 0
        val actions = SharedVouchActions(repository) { callbackInvoked++ }

        actions.restoreVouch(vouch)
        assertEquals(1, callbackInvoked)
        assertNotNull(repository.getById(vouch.vouch.id))
    }

    @Test
    fun testSettleAndUnsettleVouch_invokesCallback() = runTest {
        val vouch = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L, settled = false))
            )
        )

        var callbackInvoked = 0
        val actions = SharedVouchActions(repository) { callbackInvoked++ }

        actions.settleVouch(vouch)
        assertEquals(1, callbackInvoked)
        assertTrue(database.vouchDataDao().getSettlementsForVouch(vouch.vouch.id)[0].settled)

        actions.unSettleVouch(vouch)
        assertEquals(2, callbackInvoked)
        assertFalse(database.vouchDataDao().getSettlementsForVouch(vouch.vouch.id)[0].settled)
    }

    @Test
    fun testSetVouchPersonSettlement_invokesCallback() = runTest {
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

        var callbackInvoked = 0
        val actions = SharedVouchActions(repository) { callbackInvoked++ }

        actions.setVouchPersonSettlement(vouch.vouch.id, aliceId, true)
        assertEquals(1, callbackInvoked)

        val settlements = database.vouchDataDao().getSettlementsForVouch(vouch.vouch.id)
        assertTrue(settlements.find { it.personId == aliceId }!!.settled)
    }
}
