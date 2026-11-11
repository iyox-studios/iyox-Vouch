package com.iyox.vouch.core.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupIdentityTest {
    @Test
    fun replacingBackupPreservesMatchingPeopleWithoutReusingRemovedIdentities() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repositoryJob = SupervisorJob()
        val scope = CoroutineScope(repositoryJob + Dispatchers.Default)
        val repository = VouchRepository(database, database.vouchDao(), database.personDao(), scope)
        val backups = BackupRepository(context, database, repository)
        val file = File(context.cacheDir, "widget-identity.iyxv")
        try {
            repository.insert(VouchWithPerson(Vouch(amount = 2000), listOf(
                PersonWithAmount(Person(name = "Alice"), 2000)
            )))
            val alice = database.personDao().getByName("Alice")!!
            backups.exportData(Uri.fromFile(file)).getOrThrow()
            repository.insert(VouchWithPerson(Vouch(amount = 1000), listOf(
                PersonWithAmount(Person(name = "Bob"), 1000)
            )))
            val bob = database.personDao().getByName("Bob")!!

            repeat(2) {
                backups.importData(Uri.fromFile(file)).getOrThrow()
                assertEquals(alice.id, database.personDao().getByName("Alice")!!.id)
                assertNull(database.personDao().getById(bob.id))
                assertEquals(2000L, repository.getAll().single().persons.single().amount)
            }
            val charlieId = database.personDao().insert(Person(name = "Charlie"))
            assertNotEquals(bob.id, charlieId)
        } finally {
            repositoryJob.cancelAndJoin()
            database.close()
            file.delete()
        }
    }
}
