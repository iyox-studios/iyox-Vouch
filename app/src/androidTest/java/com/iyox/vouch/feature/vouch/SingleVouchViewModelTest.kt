package com.iyox.vouch.feature.vouch

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.PersonRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SingleVouchViewModelTest {

    private lateinit var application: Application
    private lateinit var repository: VouchRepository
    private lateinit var personRepository: PersonRepository
    private lateinit var currencyManager: CurrencyManager
    private lateinit var database: AppDatabase

    @Before
    fun setUp() = runTest {
        application = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(application)
        repository = VouchRepository(database, database.vouchDataDao(), database.personDao())
        personRepository = PersonRepository(database.personDao())
        currencyManager = CurrencyManager(application)

        database.vouchDataDao().deleteAll()
        database.personDao().deleteAll()
    }

    @After
    fun tearDown() = runTest {
        database.vouchDataDao().deleteAll()
        database.personDao().deleteAll()
    }

    private fun createViewModel(
        vouchId: Long? = null,
        personName: String? = null
    ): SingleVouchViewModel {
        val args = mutableMapOf<String, Any?>()
        if (vouchId != null) args["vouchId"] = vouchId
        if (personName != null) args["personName"] = personName
        val savedStateHandle = SavedStateHandle(args)
        return SingleVouchViewModel(savedStateHandle, repository, personRepository, currencyManager)
    }

    @Test
    fun testCreateNewVouch_success() = runTest {
        val viewModel = createViewModel(vouchId = null, personName = "Alice")
        assertEquals("Alice", viewModel.uiState.value.personName)

        viewModel.onAmountChange("25.50")
        viewModel.onGivenChange(true)
        viewModel.onDescriptionChange(" Lunch with Alice ")

        val saved = viewModel.saveVouch()
        assertNotNull(saved)
        assertTrue(viewModel.uiState.value.saveSuccess)
        assertEquals(2550L, saved?.vouch?.amount)
        assertEquals("Lunch with Alice", saved?.vouch?.description)
        assertEquals(true, saved?.vouch?.given)
        assertEquals(1, saved?.persons?.size)
        assertEquals("Alice", saved?.persons?.get(0)?.person?.name)

        viewModel.resetSaveSuccess()
        assertFalse(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun testCreateNewVouch_invalidAmount() = runTest {
        val viewModel = createViewModel()
        viewModel.onAmountChange("abc")
        viewModel.onPersonChange("Bob")

        val saved = viewModel.saveVouch()
        assertNull(saved)
        assertFalse(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun testEditExistingVouch() = runTest {
        val existing = repository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L, description = "Old Description", given = true),
                persons = listOf(PersonWithAmount(Person(name = "Charlie"), 1000L))
            )
        )
        repository.getAll() // Populate cache

        val viewModel = createViewModel(vouchId = existing.vouch.id)
        viewModel.loadVouch(existing.vouch.id.toLong(), null)
        viewModel.uiState.first { !it.isLoading && it.personName == "Charlie" }

        viewModel.onAmountChange("30.00")
        viewModel.onDescriptionChange("New Description")
        viewModel.onGivenChange(false)

        val saved = viewModel.saveVouch()
        assertNotNull(saved)
        assertEquals(3000L, saved?.vouch?.amount)
        assertEquals("New Description", saved?.vouch?.description)
        assertEquals(false, saved?.vouch?.given)

        val fromDb = repository.getById(existing.vouch.id)
        assertEquals(3000L, fromDb?.vouch?.amount)
        assertEquals("New Description", fromDb?.vouch?.description)
    }

    @Test
    fun testOnPersonSelected() = runTest {
        val viewModel = createViewModel()
        viewModel.onPersonSelected(Person(id = 1, name = "SelectedPerson"))

        assertEquals("SelectedPerson", viewModel.uiState.value.personName)
        assertTrue(viewModel.uiState.value.personSearchResults.isEmpty())
    }
}
