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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroupVouchValidationTest {

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
    fun tearDown() = runBlocking {
        repositoryScope.coroutineContext[Job]!!.cancelAndJoin()
        database.close()
    }

    private fun createViewModel(): GroupVouchViewModel {
        return GroupVouchViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            personRepository = personRepository,
            currencyManager = currencyManager
        )
    }

    @Test
    fun testContinueToMembersWithInvalidAmountSetsAmountError() {
        val viewModel = createViewModel()
        viewModel.onAmountChange("")

        viewModel.continueToMembers()

        assertEquals(STEP_DETAILS, viewModel.uiState.value.step)
        assertEquals(R.string.error_amount_required, viewModel.uiState.value.amountError)
    }

    @Test
    fun testContinueToMembersWithValidAmountAdvancesToMembers() {
        val viewModel = createViewModel()
        viewModel.onAmountChange("50.00")

        viewModel.continueToMembers()

        assertEquals(STEP_MEMBERS, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.amountError)
    }

    @Test
    fun testAddDuplicateMemberDoesNotAddDuplicate() {
        val viewModel = createViewModel()
        viewModel.addMember("Alice")
        assertEquals(1, viewModel.uiState.value.members.size)

        viewModel.addMember("alice")
        assertEquals(1, viewModel.uiState.value.members.size)
    }
}
