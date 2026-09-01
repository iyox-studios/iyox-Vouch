package com.iyox.vouch.feature.vouches

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
class VouchesViewModelTest {

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
    fun testLoadVouches_totalsAndSettledCount() = runTest {
        // Active Given: 3000
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 3000L, given = true, timestamp = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 3000L))
            )
        )

        // Active Received: 1000
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L, given = false, timestamp = 2000L),
                persons = listOf(PersonWithAmount(Person(name = "Bob"), 1000L))
            )
        )

        // Settled: 5000
        val v3 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 5000L, given = true, timestamp = 3000L),
                persons = listOf(PersonWithAmount(Person(name = "Charlie"), 5000L))
            )
        )
        repository.setVouchSettled(v3.vouch.id, true)

        val viewModel = VouchesViewModel(repository)
        viewModel.loadVouches()

        val state = viewModel.uiState.first { it.vouches.size == 2 && it.settledCount == 1 }
        assertEquals(2, state.vouches.size) // Only active vouches
        assertEquals(1, state.settledCount)
        assertEquals(3000L, state.totalGiven)
        assertEquals(1000L, state.totalOwed)
        assertEquals(2000L, state.netTotal) // 3000 - 1000 = 2000
    }

    @Test
    fun testToggleFiltersAndSorting() = runTest {
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L, given = true, timestamp = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L))
            )
        )
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L, given = false, timestamp = 2000L),
                persons = listOf(PersonWithAmount(Person(name = "Bob"), 2000L))
            )
        )

        val viewModel = VouchesViewModel(repository)
        viewModel.loadVouches()
        viewModel.uiState.first { it.vouches.size == 2 }

        assertEquals(com.iyox.vouch.core.model.VouchTypeFilter.ALL, viewModel.uiState.value.showOnlyGiven)
        assertEquals(2, viewModel.uiState.value.vouches.size)

        // Filter: Given
        viewModel.toggleShowOnlyGiven()
        val givenState = viewModel.uiState.first { it.showOnlyGiven == com.iyox.vouch.core.model.VouchTypeFilter.GIVEN && it.vouches.size == 1 }
        assertEquals(com.iyox.vouch.core.model.VouchTypeFilter.GIVEN, givenState.showOnlyGiven)
        assertEquals(1, givenState.vouches.size)
        assertTrue(givenState.vouches[0].vouch.given)

        // Filter: Received
        viewModel.toggleShowOnlyGiven()
        val receivedState = viewModel.uiState.first { it.showOnlyGiven == com.iyox.vouch.core.model.VouchTypeFilter.RECEIVED && it.vouches.size == 1 }
        assertEquals(com.iyox.vouch.core.model.VouchTypeFilter.RECEIVED, receivedState.showOnlyGiven)
        assertEquals(1, receivedState.vouches.size)
        assertFalse(receivedState.vouches[0].vouch.given)

        // Filter: All
        viewModel.toggleShowOnlyGiven()
        val allState = viewModel.uiState.first { it.showOnlyGiven == com.iyox.vouch.core.model.VouchTypeFilter.ALL && it.vouches.size == 2 }
        assertEquals(com.iyox.vouch.core.model.VouchTypeFilter.ALL, allState.showOnlyGiven)
        assertEquals(2, allState.vouches.size)

        // Sort toggle
        assertEquals(com.iyox.vouch.core.model.VouchSortOrder.DATE, viewModel.uiState.value.sortedBy)
        viewModel.toggleSortedBy()
        assertEquals(com.iyox.vouch.core.model.VouchSortOrder.AMOUNT, viewModel.uiState.first { it.sortedBy == com.iyox.vouch.core.model.VouchSortOrder.AMOUNT }.sortedBy)
        viewModel.toggleSortedBy()
        assertEquals(com.iyox.vouch.core.model.VouchSortOrder.PERSON, viewModel.uiState.first { it.sortedBy == com.iyox.vouch.core.model.VouchSortOrder.PERSON }.sortedBy)
        viewModel.toggleSortedBy()
        assertEquals(com.iyox.vouch.core.model.VouchSortOrder.DATE, viewModel.uiState.first { it.sortedBy == com.iyox.vouch.core.model.VouchSortOrder.DATE }.sortedBy)
    }

    @Test
    fun testSearchQuery() = runTest {
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1200L, description = "Pizza Dinner", given = true),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1200L))
            )
        )
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 450L, description = "Morning Coffee", given = false),
                persons = listOf(PersonWithAmount(Person(name = "Bob"), 450L))
            )
        )

        val viewModel = VouchesViewModel(repository)
        viewModel.loadVouches()
        viewModel.uiState.first { it.vouches.size == 2 }

        // Search by description
        viewModel.setSearchQuery("Pizza")
        val pizzaState =
            viewModel.uiState.first { it.searchQuery == "Pizza" && it.vouches.size == 1 }
        assertEquals("Pizza Dinner", pizzaState.vouches[0].vouch.description)

        // Search by person name
        viewModel.setSearchQuery("Bob")
        val bobState = viewModel.uiState.first { it.searchQuery == "Bob" && it.vouches.size == 1 }
        assertEquals("Bob", bobState.vouches[0].persons[0].person.name)

        // Search clearing
        viewModel.setSearchQuery("")
        val clearedState = viewModel.uiState.first { it.searchQuery == "" && it.vouches.size == 2 }
        assertEquals(2, clearedState.vouches.size)
    }
}
