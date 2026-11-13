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
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupValidationTest {
    @Test
    fun malformedPayloadsNeverReplaceExistingRecords() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val job = SupervisorJob()
        val repository = VouchRepository(database, database.vouchDao(), database.personDao(), CoroutineScope(job + Dispatchers.Default))
        val backups = BackupRepository(context, database, repository)
        val file = File(context.cacheDir, "validation.iyxv")
        val valid = """{"vouch":{"amount":100,"timestamp":1700000000000},"persons":[{"name":"Alice","amount":100}]}"""
        try {
            repository.insert(VouchWithPerson(Vouch(amount = 1234), listOf(PersonWithAmount(Person(name = "Original"), 1234))))
            val original = repository.getAll()
            val invalidEntries = listOf("null", "42", "{}", """{"vouch":null}""",
                valid.replace("\"amount\":100", "\"amount\":\"abc\""),
                valid.replace("\"amount\":100", "\"amount\":\"1.234\""),
                valid.replace("\"amount\":100", "\"amount\":\"92233720368547758.08\""),
                valid.replace("1700000000000", "\"2025-02-30\""),
                valid.replace("\"Alice\"", "\" \""),
                valid.replace("\"Alice\"", "123"),
                valid.replace("\"persons\":[", "\"persons\":[null,"),
                valid.replace("\"timestamp\":1700000000000", "\"given\":\"maybe\""),
                valid.replace("\"persons\":[{\"name\":\"Alice\",\"amount\":100}]", "\"persons\":null"))
            val payloads = invalidEntries.map { "[$valid,$it]" } + listOf(
                """{"type":"backup","data":null}""",
                """{"type":"single","data":[$valid,$valid]}""",
                """{"version":"999","data":[]}""",
                """{"type":null,"data":[]}""")
            for (payload in payloads) {
                file.writeText(payload)
                assertTrue(payload, backups.readPreview(Uri.fromFile(file)).isFailure)
                assertTrue(payload, backups.importData(Uri.fromFile(file)).isFailure)
                assertTrue(payload, backups.importFromPreview(BackupPreview(file.toURI().toString(), "backup", "1.0", 2, 1, payload)).isFailure)
                assertEquals(payload, original, repository.getAll())
            }
            file.writeText("[$valid]")
            assertTrue(backups.importData(Uri.fromFile(file)).isSuccess)
            assertEquals("Alice", repository.getAll().single().persons.single().person.name)
            // A deliberately empty backup remains a valid replacement.
            file.writeText("[]")
            assertTrue(backups.importData(Uri.fromFile(file)).isSuccess)
            assertTrue(repository.getAll().isEmpty())
        } finally {
            job.cancelAndJoin()
            database.close()
            file.delete()
        }
    }
}
