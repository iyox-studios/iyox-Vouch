package com.iyox.vouch.feature.vouch

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SingleVouchValidationTest {

    private lateinit var application: Application
    private lateinit var database: AppDatabase
    private lateinit var repositoryScope: CoroutineScope
    private lateinit var repository: VouchRepository
    private lateinit var personRepository: PersonRepository
    private lateinit var currencyManager: CurrencyManager

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        repository = VouchRepository(database, database.vouchDao(), database.personDao(), repositoryScope)
        personRepository = PersonRepository(database.personDao(), repositoryScope)
        currencyManager = CurrencyManager(application)
    }

    @After
    fun tearDown() {
        repositoryScope.cancel()
        database.close()
    }

    private fun createViewModel(): SingleVouchViewModel {
        return SingleVouchViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            personRepository = personRepository,
            currencyManager = currencyManager
        )
    }

    @Test
    fun testBlankAmountSetsAmountError() = runTest {
        val viewModel = createViewModel()
        viewModel.onAmountChange("")
        viewModel.onPersonChange("Alice")

        val result = viewModel.saveVouchDirect()

        assertNull(result)
        assertEquals(R.string.error_amount_required, viewModel.uiState.value.amountError)
        assertNull(viewModel.uiState.value.personNameError)
        assertFalse(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun testZeroAmountSetsAmountError() = runTest {
        val viewModel = createViewModel()
        viewModel.onAmountChange("0")
        viewModel.onPersonChange("Alice")

        val result = viewModel.saveVouchDirect()

        assertNull(result)
        assertEquals(R.string.error_amount_required, viewModel.uiState.value.amountError)
        assertNull(viewModel.uiState.value.personNameError)
        assertFalse(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun testValidateFormDirect() {
        val viewModel = createViewModel()
        viewModel.onAmountChange("")
        viewModel.onPersonChange("")
        val invalidResult = viewModel.validateForm()
        assertFalse(invalidResult)
        assertEquals(R.string.error_amount_required, viewModel.uiState.value.amountError)
        assertEquals(R.string.error_person_name_required, viewModel.uiState.value.personNameError)

        viewModel.onAmountChange("15.00")
        viewModel.onPersonChange("Alice")
        val validResult = viewModel.validateForm()
        assertTrue(validResult)
        assertNull(viewModel.uiState.value.amountError)
        assertNull(viewModel.uiState.value.personNameError)
    }

    @Test
    fun testOnPersonSelectedClearsPersonNameError() {
        val viewModel = createViewModel()
        viewModel.onAmountChange("15.00")
        viewModel.onPersonChange("")

        val valid = viewModel.validateForm()
        assertFalse(valid)
        assertEquals(R.string.error_person_name_required, viewModel.uiState.value.personNameError)

        viewModel.onPersonSelected(Person(name = "Bob"))
        assertNull(viewModel.uiState.value.personNameError)
        assertEquals("Bob", viewModel.uiState.value.personName)
    }

    @Test
    fun testBlankPersonNameSetsPersonNameError() = runTest {
        val viewModel = createViewModel()
        viewModel.onAmountChange("15.00")
        viewModel.onPersonChange("   ")

        val result = viewModel.saveVouchDirect()

        assertNull(result)
        assertNull(viewModel.uiState.value.amountError)
        assertEquals(R.string.error_person_name_required, viewModel.uiState.value.personNameError)
        assertFalse(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun testTypingClearsErrors() = runTest {
        val viewModel = createViewModel()
        viewModel.onAmountChange("")
        viewModel.onPersonChange("")

        viewModel.saveVouchDirect()
        assertEquals(R.string.error_amount_required, viewModel.uiState.value.amountError)
        assertEquals(R.string.error_person_name_required, viewModel.uiState.value.personNameError)

        viewModel.onAmountChange("20.00")
        assertNull(viewModel.uiState.value.amountError)

        viewModel.onPersonChange("Bob")
        assertNull(viewModel.uiState.value.personNameError)
    }

    @Test
    fun testValidVouchSavesSuccessfully() = runTest {
        val viewModel = createViewModel()
        viewModel.onAmountChange("25.50")
        viewModel.onPersonChange("Charlie")

        val result = viewModel.saveVouchDirect()

        assertTrue(viewModel.uiState.value.saveSuccess)
        assertNull(viewModel.uiState.value.amountError)
        assertNull(viewModel.uiState.value.personNameError)
        assertEquals(2550L, result?.vouch?.amount)
        assertEquals("Charlie", result?.persons?.firstOrNull()?.person?.name)
    }
}
