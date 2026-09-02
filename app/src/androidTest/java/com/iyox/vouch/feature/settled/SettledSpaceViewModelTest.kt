package com.iyox.vouch.feature.settled

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.core.data.AppDatabase
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettledSpaceViewModelTest {

    private lateinit var application: Application
    private lateinit var repository: VouchRepository
    private lateinit var database: AppDatabase

    @Before
    fun setUp() = runTest {
        application = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(application)
        repository = VouchRepository(database, database.vouchDao(), database.personDao())

        database.vouchDao().deleteAll()
        database.personDao().deleteAll()
    }

    @After
    fun tearDown() = runTest {
        database.vouchDao().deleteAll()
        database.personDao().deleteAll()
    }

    private fun createViewModel(personName: String? = null): SettledSpaceViewModel {
        val args = mutableMapOf<String, Any?>()
        if (personName != null) args["personName"] = personName
        val savedStateHandle = SavedStateHandle(args)
        return SettledSpaceViewModel(savedStateHandle, repository)
    }

    @Test
    fun testGlobalSettledSpace() = runTest {
        // Vouch 1: Settled Given (2000)
        val v1 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L, given = true),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 2000L, settled = true))
            )
        )
        // Vouch 2: Settled Received (1000)
        val v2 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L, given = false),
                persons = listOf(PersonWithAmount(Person(name = "Bob"), 1000L, settled = true))
            )
        )
        // Vouch 3: Active Unsettled (5000)
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 5000L, given = true),
                persons = listOf(PersonWithAmount(Person(name = "Charlie"), 5000L, settled = false))
            )
        )

        val viewModel = createViewModel(null)
        viewModel.loadVouches()

        val state = viewModel.uiState.first { it.vouches.size == 2 && it.settledCount == 2 }
        assertEquals(2, state.settledCount)
        assertEquals(2, state.vouches.size)
        assertEquals(2000L, state.totalSettledGiven)
        assertEquals(1000L, state.totalSettledReceived)
        assertEquals(3000L, state.totalSettledVolume) // 2000 + 1000
    }

    @Test
    fun testPersonSpecificSettledSpace() = runTest {
        // Vouch with Alice settled, Bob unsettled
        val v1 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 3000L, given = true),
                persons = listOf(
                    PersonWithAmount(Person(name = "Alice"), 1500L, settled = true),
                    PersonWithAmount(Person(name = "Bob"), 1500L, settled = false)
                )
            )
        )

        val viewModel = createViewModel("Alice")
        viewModel.loadVouches()

        val state = viewModel.uiState.first { it.vouches.size == 1 && it.settledCount == 1 }
        assertEquals(1, state.settledCount)
        assertEquals(1, state.vouches.size)
        assertEquals(1500L, state.totalSettledGiven)
        assertEquals(0L, state.totalSettledReceived)
        assertEquals(1500L, state.totalSettledVolume)

        // Switching selected person to Bob (Bob is unsettled)
        viewModel.setSelectedPerson("Bob")
        val bobState = viewModel.uiState.first { it.selectedPersonName == "Bob" }
        assertEquals(0, bobState.settledCount)
        assertEquals(0, bobState.vouches.size)

        // Clear person filter to global
        viewModel.setSelectedPerson(null)
        val globalState = viewModel.uiState.first { it.selectedPersonName == null }
        // Global requires fully settled, since Bob is unsettled, v1 is not fully settled
        assertEquals(0, globalState.settledCount)
    }

    @Test
    fun testFilterAndSortToggles() = runTest {
        val viewModel = createViewModel()

        assertEquals(VouchTypeFilter.ALL, viewModel.uiState.value.showOnlyGiven)
        viewModel.toggleShowOnlyGiven()
        assertEquals(
            VouchTypeFilter.GIVEN,
            viewModel.uiState.first { it.showOnlyGiven == VouchTypeFilter.GIVEN }.showOnlyGiven
        )
        viewModel.toggleShowOnlyGiven()
        assertEquals(
            VouchTypeFilter.RECEIVED,
            viewModel.uiState.first { it.showOnlyGiven == VouchTypeFilter.RECEIVED }.showOnlyGiven
        )
        viewModel.toggleShowOnlyGiven()
        assertEquals(
            VouchTypeFilter.ALL,
            viewModel.uiState.first { it.showOnlyGiven == VouchTypeFilter.ALL }.showOnlyGiven
        )

        assertEquals(VouchSortOrder.DATE, viewModel.uiState.value.sortedBy)
        viewModel.toggleSortedBy()
        assertEquals(
            VouchSortOrder.AMOUNT,
            viewModel.uiState.first { it.sortedBy == VouchSortOrder.AMOUNT }.sortedBy
        )
        viewModel.toggleSortedBy()
        assertEquals(
            VouchSortOrder.PERSON,
            viewModel.uiState.first { it.sortedBy == VouchSortOrder.PERSON }.sortedBy
        )
        viewModel.toggleSortedBy()
        assertEquals(
            VouchSortOrder.DATE,
            viewModel.uiState.first { it.sortedBy == VouchSortOrder.DATE }.sortedBy
        )
    }

    @Test
    fun testSearchQuery() = runTest {
        val v1 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L, description = "Birthday Gift", given = true),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 2000L, settled = true))
            )
        )
        val v2 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L, description = "Train Ticket", given = false),
                persons = listOf(PersonWithAmount(Person(name = "Bob"), 1000L, settled = true))
            )
        )

        val viewModel = createViewModel(null)
        viewModel.loadVouches()
        viewModel.uiState.first { it.vouches.size == 2 }

        // Search description
        viewModel.setSearchQuery("Birthday")
        val bdayState =
            viewModel.uiState.first { it.searchQuery == "Birthday" && it.vouches.size == 1 }
        assertEquals("Birthday Gift", bdayState.vouches[0].vouch.description)

        // Search person
        viewModel.setSearchQuery("Bob")
        val bobState = viewModel.uiState.first { it.searchQuery == "Bob" && it.vouches.size == 1 }
        assertEquals("Bob", bobState.vouches[0].persons[0].person.name)

        // Clear query
        viewModel.setSearchQuery("")
        val clearedState = viewModel.uiState.first { it.searchQuery == "" && it.vouches.size == 2 }
        assertEquals(2, clearedState.vouches.size)
    }
}
