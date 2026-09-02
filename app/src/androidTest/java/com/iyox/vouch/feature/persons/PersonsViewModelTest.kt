package com.iyox.vouch.feature.persons

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonsViewModelTest {

    private lateinit var repository: VouchRepository
    private lateinit var database: AppDatabase

    @Before
    fun setUp() = runTest {
        val context: Application = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        repository = VouchRepository(database, database.vouchDataDao(), database.personDao())

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
    fun testLoadPersons_and_toggleShowOnlyNonZero() = runTest {
        // Alice has active vouch (vouchCount = 1)
        val v1 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L))
            )
        )

        // Bob has settled vouch (vouchCount = 0)
        val v2 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L),
                persons = listOf(PersonWithAmount(Person(name = "Bob"), 2000L))
            )
        )
        repository.setVouchSettled(v2.vouch.id, true)

        val viewModel = PersonsViewModel(
            com.iyox.vouch.core.data.PersonRepository(database.personDao())
        )
        viewModel.loadPersons()

        // By default showOnlyNonZero is true -> wait for loaded persons (only Alice)
        val stateOnlyNonZero = viewModel.uiState.first { it.persons.isNotEmpty() }
        assertTrue(stateOnlyNonZero.showOnlyNonZero)
        assertEquals(1, stateOnlyNonZero.persons.size)
        assertEquals("Alice", stateOnlyNonZero.persons[0].person.name)

        // Toggle showOnlyNonZero to false -> both Alice and Bob
        viewModel.toggleShowOnlyNonZero()
        val stateAll = viewModel.uiState.first { !it.showOnlyNonZero && it.persons.size == 2 }
        assertFalse(stateAll.showOnlyNonZero)
        assertEquals(2, stateAll.persons.size)

        // Toggle back to true
        viewModel.toggleShowOnlyNonZero()
        val stateToggledBack = viewModel.uiState.first { it.showOnlyNonZero && it.persons.size == 1 }
        assertTrue(stateToggledBack.showOnlyNonZero)
        assertEquals(1, stateToggledBack.persons.size)
    }

    @Test
    fun testSearchQuery() = runTest {
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice Anderson"), 1000L))
            )
        )
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L),
                persons = listOf(PersonWithAmount(Person(name = "Bob Builder"), 2000L))
            )
        )

        val viewModel = PersonsViewModel(
            com.iyox.vouch.core.data.PersonRepository(database.personDao())
        )
        viewModel.loadPersons()
        viewModel.uiState.first { it.persons.size == 2 }

        // Search for "Alice"
        viewModel.setSearchQuery("alice")
        val aliceState =
            viewModel.uiState.first { it.searchQuery == "alice" && it.persons.size == 1 }
        assertEquals("Alice Anderson", aliceState.persons[0].person.name)

        // Search for "Bob"
        viewModel.setSearchQuery("Bob")
        val bobState = viewModel.uiState.first { it.searchQuery == "Bob" && it.persons.size == 1 }
        assertEquals("Bob Builder", bobState.persons[0].person.name)

        // Non-matching search
        viewModel.setSearchQuery("Charlie")
        val emptyState =
            viewModel.uiState.first { it.searchQuery == "Charlie" && it.persons.isEmpty() }
        assertEquals(0, emptyState.persons.size)

        // Clear search
        viewModel.setSearchQuery("")
        val clearedState = viewModel.uiState.first { it.searchQuery == "" && it.persons.size == 2 }
        assertEquals(2, clearedState.persons.size)
    }
}
