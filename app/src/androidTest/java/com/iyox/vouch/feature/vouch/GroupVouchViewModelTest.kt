package com.iyox.vouch.feature.vouch

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.PersonRepository
import com.iyox.vouch.core.data.VouchRepository
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
class GroupVouchViewModelTest {

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

    private fun createViewModel(vouchId: Long? = null): GroupVouchViewModel {
        val args = mutableMapOf<String, Any?>()
        if (vouchId != null) args["vouchId"] = vouchId
        val savedStateHandle = SavedStateHandle(args)
        return GroupVouchViewModel(savedStateHandle, repository, personRepository, currencyManager)
    }

    @Test
    fun testStepNavigation() {
        val viewModel = createViewModel()
        assertEquals(1, viewModel.uiState.value.step)

        // Invalid amount cannot continue
        viewModel.onAmountChange("")
        viewModel.continueToMembers()
        assertEquals(1, viewModel.uiState.value.step)

        // Valid amount continues to step 2
        viewModel.onAmountChange("60")
        viewModel.continueToMembers()
        assertEquals(2, viewModel.uiState.value.step)

        // Back to step 1
        viewModel.backToDetails()
        assertEquals(1, viewModel.uiState.value.step)
    }

    @Test
    fun testAddMembersAndEqualSplit() {
        val viewModel = createViewModel()
        viewModel.onAmountChange("60")
        viewModel.continueToMembers()

        viewModel.addMember("Alice")
        assertEquals(1, viewModel.uiState.value.members.size)
        assertEquals(currencyManager.formatAmount(6000L), viewModel.uiState.value.members[0].amount)

        viewModel.addMember("Bob")
        assertEquals(2, viewModel.uiState.value.members.size)
        assertEquals(currencyManager.formatAmount(3000L), viewModel.uiState.value.members[0].amount)
        assertEquals(currencyManager.formatAmount(3000L), viewModel.uiState.value.members[1].amount)

        viewModel.addMember("Charlie")
        assertEquals(3, viewModel.uiState.value.members.size)
        assertEquals(currencyManager.formatAmount(2000L), viewModel.uiState.value.members[0].amount)
        assertEquals(currencyManager.formatAmount(2000L), viewModel.uiState.value.members[1].amount)
        assertEquals(currencyManager.formatAmount(2000L), viewModel.uiState.value.members[2].amount)

        // Duplicate member is ignored
        viewModel.addMember("alice")
        assertEquals(3, viewModel.uiState.value.members.size)
    }

    @Test
    fun testCustomSplitAndRebalancing() {
        val viewModel = createViewModel()
        viewModel.onAmountChange("60")
        viewModel.continueToMembers()

        viewModel.addMember("Alice")
        viewModel.addMember("Bob")
        viewModel.addMember("Charlie")

        // Set Alice's amount to "40" -> trigger rebalancing so remaining 20 is split between Bob and Charlie (10 each)
        viewModel.onMemberAmountChange("Alice", "40")
        viewModel.resetAllToEqualSplit()

        val state = viewModel.uiState.value
        val alice = state.members.find { it.personName == "Alice" }!!
        val bob = state.members.find { it.personName == "Bob" }!!
        val charlie = state.members.find { it.personName == "Charlie" }!!

        assertTrue(alice.isCustom)
        assertEquals("40", alice.amount)
        assertFalse(bob.isCustom)
        assertEquals(currencyManager.formatAmount(1000L), bob.amount)
        assertFalse(charlie.isCustom)
        assertEquals(currencyManager.formatAmount(1000L), charlie.amount)

        // Reset Alice to equal split
        viewModel.resetMemberToEqualSplit("Alice")
        val resetState = viewModel.uiState.value
        assertEquals(currencyManager.formatAmount(2000L), resetState.members[0].amount)
        assertEquals(currencyManager.formatAmount(2000L), resetState.members[1].amount)
        assertEquals(currencyManager.formatAmount(2000L), resetState.members[2].amount)
    }

    @Test
    fun testAddSelfMember() {
        val viewModel = createViewModel()
        viewModel.onAmountChange("60")
        viewModel.continueToMembers()

        viewModel.addSelfMember("Me")
        viewModel.addMember("Alice")
        viewModel.addMember("Bob")

        val state = viewModel.uiState.value
        assertEquals(3, state.members.size)
        assertTrue(state.members.any { it.isSelf && it.personName == "Me" })
        assertEquals(currencyManager.formatAmount(2000L), state.members.find { it.isSelf }?.amount)
    }

    @Test
    fun testSaveGroupVouch_validationAndTotal() = runTest {
        val viewModel = createViewModel()
        viewModel.onAmountChange("90")
        viewModel.onDescriptionChange("Group Dinner")
        viewModel.continueToMembers()

        // Less than 2 non-self members should fail
        viewModel.addSelfMember("Me")
        viewModel.addMember("Alice")
        val failSave = viewModel.saveGroupVouches()
        assertNull(failSave)
        assertFalse(viewModel.uiState.value.saveSuccess)

        // Add 2nd non-self member (Alice and Bob) -> Total 90 split among 3 (30 each).
        // Total amount for the created vouch is Alice's share (30) + Bob's share (30) = 60 (excluding Self).
        viewModel.addMember("Bob")
        val saved = viewModel.saveGroupVouches()
        assertNotNull(saved)
        assertTrue(viewModel.uiState.value.saveSuccess)
        assertEquals(6000L, saved?.vouch?.amount)
        assertEquals("Group Dinner", saved?.vouch?.description)
        assertEquals(2, saved?.persons?.size)
        assertEquals(3000L, saved?.persons?.find { it.person.name == "Alice" }?.amount)
        assertEquals(3000L, saved?.persons?.find { it.person.name == "Bob" }?.amount)
    }

    @Test
    fun testRemoveMember() {
        val viewModel = createViewModel()
        viewModel.onAmountChange("40")
        viewModel.continueToMembers()

        viewModel.addMember("Alice")
        viewModel.addMember("Bob")
        viewModel.addMember("Charlie")
        assertEquals(3, viewModel.uiState.value.members.size)

        viewModel.removeMember("Charlie")
        assertEquals(2, viewModel.uiState.value.members.size)
        assertEquals(currencyManager.formatAmount(2000L), viewModel.uiState.value.members[0].amount)
        assertEquals(currencyManager.formatAmount(2000L), viewModel.uiState.value.members[1].amount)
    }
}
