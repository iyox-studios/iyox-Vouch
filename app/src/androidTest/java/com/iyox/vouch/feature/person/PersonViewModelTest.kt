package com.iyox.vouch.feature.person

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.R
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchSortOrder
import com.iyox.vouch.core.model.VouchTypeFilter
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
class PersonViewModelTest {

    private lateinit var application: Application
    private lateinit var repository: VouchRepository
    private lateinit var personRepository: PersonRepository
    private lateinit var database: AppDatabase

    @Before
    fun setUp() = runTest {
        application = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(application)
        repository = VouchRepository(database, database.vouchDataDao(), database.personDao())
        personRepository = PersonRepository(database.personDao())

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

    private fun createViewModel(personName: String): PersonViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("personName" to personName))
        return PersonViewModel(savedStateHandle, repository, personRepository)
    }

    @Test
    fun testInitialLoadAndCalculations() = runTest {
        // Insert 1 given vouch (2000) and 1 received vouch (500)
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L, given = true, timestamp = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 2000L))
            )
        )
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 500L, given = false, timestamp = 2000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 500L))
            )
        )

        val viewModel = createViewModel("Alice")
        viewModel.loadPerson("Alice")

        val state = viewModel.uiState.first { !it.isLoading && it.vouches.isNotEmpty() }
        assertEquals(2000L, state.amountGiven)
        assertEquals(500L, state.amountOwed)
        assertEquals(1500L, state.personData?.amount)
        assertEquals(2, state.personData?.vouchCount)
        assertEquals(2, state.vouches.size)
        assertEquals(0, state.settledCount)
        assertFalse(state.isLoading)
    }

    @Test
    fun testToggleFiltersAndSorting() = runTest {
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L, given = true, timestamp = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Bob"), 1000L))
            )
        )
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L, given = false, timestamp = 2000L),
                persons = listOf(PersonWithAmount(Person(name = "Bob"), 2000L))
            )
        )

        val viewModel = createViewModel("Bob")
        viewModel.loadPerson("Bob")
        viewModel.uiState.first { !it.isLoading && it.vouches.size == 2 }

        // Initial: show all
        assertEquals(VouchTypeFilter.ALL, viewModel.uiState.value.showOnlyGiven)
        assertEquals(2, viewModel.uiState.value.vouches.size)

        // Toggle to given only
        viewModel.toggleShowOnlyGiven()
        assertEquals(VouchTypeFilter.GIVEN, viewModel.uiState.value.showOnlyGiven)
        assertEquals(1, viewModel.uiState.value.vouches.size)
        assertTrue(viewModel.uiState.value.vouches[0].vouch.given)

        // Toggle to received only
        viewModel.toggleShowOnlyGiven()
        assertEquals(VouchTypeFilter.RECEIVED, viewModel.uiState.value.showOnlyGiven)
        assertEquals(1, viewModel.uiState.value.vouches.size)
        assertFalse(viewModel.uiState.value.vouches[0].vouch.given)

        // Toggle back to all
        viewModel.toggleShowOnlyGiven()
        assertEquals(VouchTypeFilter.ALL, viewModel.uiState.value.showOnlyGiven)
        assertEquals(2, viewModel.uiState.value.vouches.size)

        // Sort toggle
        assertEquals(VouchSortOrder.DATE, viewModel.uiState.value.sortedBy)
        viewModel.toggleSortedBy()
        assertEquals(VouchSortOrder.AMOUNT, viewModel.uiState.value.sortedBy)
    }

    @Test
    fun testSettlePersonAndUndo() = runTest {
        val v1 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L, given = true),
                persons = listOf(PersonWithAmount(Person(name = "Charlie"), 1000L))
            )
        )

        val viewModel = createViewModel("Charlie")
        viewModel.loadPerson("Charlie")
        viewModel.uiState.first { !it.isLoading && it.vouches.isNotEmpty() }

        // Settle person
        val settledChanges = viewModel.settlePerson()
        assertEquals(1, settledChanges.size)
        assertEquals(v1.vouch.id, settledChanges[0].vouchId)

        viewModel.loadPerson("Charlie")
        val settledState = viewModel.uiState.first { !it.isLoading && it.settledCount == 1 }
        assertEquals(0, settledState.vouches.size)
        assertEquals(1, settledState.settledCount)

        // Undo settle
        viewModel.undoSettlePerson(settledChanges)
        viewModel.loadPerson("Charlie")
        val undoneState = viewModel.uiState.first { !it.isLoading && it.vouches.isNotEmpty() }
        assertEquals(1, undoneState.vouches.size)
        assertEquals(0, undoneState.settledCount)
    }

    @Test
    fun testNetVouchesAndUndo() = runTest {
        // Given 3000, Received 1000 -> Diff +2000
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 3000L, given = true),
                persons = listOf(PersonWithAmount(Person(name = "Dave"), 3000L))
            )
        )
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L, given = false),
                persons = listOf(PersonWithAmount(Person(name = "Dave"), 1000L))
            )
        )

        val viewModel = createViewModel("Dave")
        viewModel.loadPerson("Dave")
        viewModel.uiState.first { !it.isLoading && it.vouches.size == 2 }

        val (settled, nettingVouch) = viewModel.netVouches(application.getString(R.string.vouch_description_netting))
        assertEquals(2, settled.size)
        assertNotNull(nettingVouch)
        assertEquals(2000L, nettingVouch?.vouch?.amount)
        assertEquals(true, nettingVouch?.vouch?.given)
        assertEquals(
            application.getString(R.string.vouch_description_netting),
            nettingVouch?.vouch?.description
        )

        viewModel.loadPerson("Dave")
        val nettedState = viewModel.uiState.first { !it.isLoading && it.vouches.size == 1 }
        assertEquals(1, nettedState.vouches.size) // Only the new netting vouch is active
        assertEquals(nettingVouch?.vouch?.id, nettedState.vouches[0].vouch.id)
        assertEquals(2, nettedState.settledCount)

        // Undo netting
        viewModel.undoNetVouches(settled, nettingVouch)
        viewModel.loadPerson("Dave")
        val undoneState = viewModel.uiState.first { !it.isLoading && it.vouches.size == 2 }
        assertEquals(2, undoneState.vouches.size)
        assertEquals(0, undoneState.settledCount)
        assertNull(repository.getById(nettingVouch!!.vouch.id))
    }

    @Test
    fun testRenamePerson() = runTest {
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1500L, given = true),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1500L))
            )
        )

        val viewModel = createViewModel("Alice")
        viewModel.loadPerson("Alice")
        viewModel.uiState.first { !it.isLoading && it.vouches.isNotEmpty() }

        // Rename Alice to Alicia
        val renameResult = viewModel.renamePerson("Alicia")
        assertTrue(renameResult.isSuccess)

        val renamedState = viewModel.uiState.first { !it.isLoading && it.personData?.person?.name == "Alicia" }
        assertEquals("Alicia", renamedState.personData?.person?.name)
        assertEquals(1, renamedState.vouches.size)
        assertEquals(1500L, renamedState.amountGiven)

        // Renaming to empty should fail
        val blankResult = viewModel.renamePerson("   ")
        assertTrue(blankResult.isFailure)

        // Renaming to an existing other person should fail
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L, given = true),
                persons = listOf(PersonWithAmount(Person(name = "Bob"), 2000L))
            )
        )
        val duplicateResult = viewModel.renamePerson("Bob")
        assertTrue(duplicateResult.isFailure)
    }

    @Test
    fun testSearchQuery() = runTest {
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2500L, description = "Concert Ticket", given = true),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 2500L))
            )
        )
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 800L, description = "Lunch Tacos", given = false),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 800L))
            )
        )

        val viewModel = createViewModel("Alice")
        viewModel.loadPerson("Alice")
        viewModel.uiState.first { !it.isLoading && it.vouches.size == 2 }

        // Search by description
        viewModel.setSearchQuery("Concert")
        val concertState =
            viewModel.uiState.first { it.searchQuery == "Concert" && it.vouches.size == 1 }
        assertEquals("Concert Ticket", concertState.vouches[0].vouch.description)

        // Search by other description
        viewModel.setSearchQuery("Tacos")
        val tacosState =
            viewModel.uiState.first { it.searchQuery == "Tacos" && it.vouches.size == 1 }
        assertEquals("Lunch Tacos", tacosState.vouches[0].vouch.description)

        // Clear query
        viewModel.setSearchQuery("")
        val clearedState = viewModel.uiState.first { it.searchQuery == "" && it.vouches.size == 2 }
        assertEquals(2, clearedState.vouches.size)
    }
}
