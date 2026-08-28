package com.iyox.vouch.feature.vouches

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
import java.sql.Timestamp

@RunWith(AndroidJUnit4::class)
class VouchesViewModelTest {

    private lateinit var application: Application
    private lateinit var repository: VouchRepository
    private lateinit var database: AppDatabase

    @Before
    fun setUp() = runTest {
        application = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(application)
        repository = VouchRepository.getInstance(application)

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
                vouch = Vouch(amount = 3000L, given = true, timestamp = Timestamp(1000L)),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 3000L))
            )
        )

        // Active Received: 1000
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L, given = false, timestamp = Timestamp(2000L)),
                persons = listOf(PersonWithAmount(Person(name = "Bob"), 1000L))
            )
        )

        // Settled: 5000
        val v3 = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 5000L, given = true, timestamp = Timestamp(3000L)),
                persons = listOf(PersonWithAmount(Person(name = "Charlie"), 5000L))
            )
        )
        repository.setVouchSettled(v3.vouch.id, true)

        val viewModel = VouchesViewModel(application)
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
                vouch = Vouch(amount = 1000L, given = true, timestamp = Timestamp(1000L)),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L))
            )
        )
        repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L, given = false, timestamp = Timestamp(2000L)),
                persons = listOf(PersonWithAmount(Person(name = "Bob"), 2000L))
            )
        )

        val viewModel = VouchesViewModel(application)
        viewModel.loadVouches()
        viewModel.uiState.first { it.vouches.size == 2 }

        assertEquals(0, viewModel.uiState.value.showOnlyGiven)
        assertEquals(2, viewModel.uiState.value.vouches.size)

        // Filter: Given
        viewModel.toggleShowOnlyGiven()
        val givenState = viewModel.uiState.first { it.showOnlyGiven == 1 && it.vouches.size == 1 }
        assertEquals(1, givenState.showOnlyGiven)
        assertEquals(1, givenState.vouches.size)
        assertTrue(givenState.vouches[0].vouch.given)

        // Filter: Received
        viewModel.toggleShowOnlyGiven()
        val receivedState = viewModel.uiState.first { it.showOnlyGiven == 2 && it.vouches.size == 1 }
        assertEquals(2, receivedState.showOnlyGiven)
        assertEquals(1, receivedState.vouches.size)
        assertFalse(receivedState.vouches[0].vouch.given)

        // Filter: All
        viewModel.toggleShowOnlyGiven()
        val allState = viewModel.uiState.first { it.showOnlyGiven == 0 && it.vouches.size == 2 }
        assertEquals(0, allState.showOnlyGiven)
        assertEquals(2, allState.vouches.size)

        // Sort toggle
        assertEquals(0, viewModel.uiState.value.sortedBy)
        viewModel.toggleSortedBy()
        assertEquals(1, viewModel.uiState.first { it.sortedBy == 1 }.sortedBy)
        viewModel.toggleSortedBy()
        assertEquals(2, viewModel.uiState.first { it.sortedBy == 2 }.sortedBy)
        viewModel.toggleSortedBy()
        assertEquals(0, viewModel.uiState.first { it.sortedBy == 0 }.sortedBy)
    }
}
