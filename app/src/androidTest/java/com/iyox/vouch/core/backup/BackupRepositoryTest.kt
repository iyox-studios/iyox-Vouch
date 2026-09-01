package com.iyox.vouch.core.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {

    private lateinit var context: Context
    private lateinit var backupRepository: BackupRepository
    private lateinit var vouchRepository: VouchRepository
    private lateinit var database: AppDatabase
    private lateinit var tempDir: File

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        vouchRepository = VouchRepository(database, database.vouchDataDao(), database.personDao())
        backupRepository = BackupRepository(context, database, vouchRepository)
        tempDir = File(context.cacheDir, "backup_test").apply { mkdirs() }

        database.vouchDataDao().deleteAll()
        database.personDao().deleteAll()
        vouchRepository.notifyDataChanged()
    }

    @After
    fun tearDown() = runTest {
        database.vouchDataDao().deleteAll()
        database.personDao().deleteAll()
        vouchRepository.notifyDataChanged()
        tempDir.deleteRecursively()
    }

    @Test
    fun testExportAndImportData() = runTest {
        // 1. Insert initial data
        val v1 = vouchRepository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 2000L, description = "Lunch", given = true),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 2000L, settled = false))
            )
        )
        val v2 = vouchRepository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 3000L, description = "Groceries", given = false),
                persons = listOf(
                    PersonWithAmount(Person(name = "Bob"), 1500L, settled = true),
                    PersonWithAmount(Person(name = "Charlie"), 1500L, settled = false)
                )
            )
        )

        // 2. Export to temp file
        val exportFile = File(tempDir, "export_test.iyxv")
        val exportUri = Uri.fromFile(exportFile)

        val exportResult = backupRepository.exportData(exportUri)
        assertTrue(exportResult.isSuccess)
        assertTrue(exportFile.exists())
        assertTrue(exportFile.length() > 0)

        // 3. Read preview
        val previewResult = backupRepository.readPreview(exportUri)
        assertTrue(previewResult.isSuccess)
        val preview = previewResult.getOrNull()
        assertNotNull(preview)
        assertEquals(2, preview?.vouchCount)
        assertEquals(3, preview?.personCount) // Alice, Bob, Charlie
        assertEquals(IyoxEnvelope.TYPE_BACKUP, preview?.type)

        // 4. Clear current database
        database.vouchDataDao().deleteAll()
        database.personDao().deleteAll()
        vouchRepository.notifyDataChanged()
        assertEquals(0, vouchRepository.getAll().size)

        // 5. Import back from file
        val importResult = backupRepository.importData(exportUri)
        assertTrue(importResult.isSuccess)
        assertTrue(importResult.getOrNull() is BackupImportResult.BackupSuccess)
        assertEquals(2, (importResult.getOrNull() as BackupImportResult.BackupSuccess).count)

        // 6. Verify restored database
        val allRestored = vouchRepository.getAll()
        assertEquals(2, allRestored.size)

        val restoredLunch = allRestored.find { it.vouch.description == "Lunch" }
        assertNotNull(restoredLunch)
        assertEquals(2000L, restoredLunch?.vouch?.amount)
        assertEquals(true, restoredLunch?.vouch?.given)
        assertEquals(1, restoredLunch?.persons?.size)
        assertEquals("Alice", restoredLunch?.persons?.get(0)?.person?.name)
        assertEquals(false, restoredLunch?.persons?.get(0)?.settled)

        val restoredGroceries = allRestored.find { it.vouch.description == "Groceries" }
        assertNotNull(restoredGroceries)
        assertEquals(3000L, restoredGroceries?.vouch?.amount)
        assertEquals(false, restoredGroceries?.vouch?.given)
        assertEquals(2, restoredGroceries?.persons?.size)
        val bob = restoredGroceries?.persons?.find { it.person.name == "Bob" }
        val charlie = restoredGroceries?.persons?.find { it.person.name == "Charlie" }
        assertNotNull(bob)
        assertEquals(true, bob?.settled)
        assertNotNull(charlie)
        assertEquals(false, charlie?.settled)
    }

    @Test
    fun testReadPreview_invalidSignature() = runTest {
        val file = File(tempDir, "invalid_sig.iyxv")
        FileOutputStream(file).use {
            it.write("""
                {
                    "_app_signature": "com.fake.app",
                    "type": "backup",
                    "version": "1.0",
                    "data": []
                }
            """.trimIndent().toByteArray())
        }

        val result = backupRepository.readPreview(Uri.fromFile(file))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidSignatureException)
    }

    @Test
    fun testReadPreview_unsupportedType() = runTest {
        val file = File(tempDir, "unknown_type.iyxv")
        FileOutputStream(file).use {
            it.write("""
                {
                    "_app_signature": "com.iyox.vouch",
                    "type": "unknown_type",
                    "version": "1.0",
                    "data": []
                }
            """.trimIndent().toByteArray())
        }

        val result = backupRepository.readPreview(Uri.fromFile(file))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedBackupTypeException)
    }

    @Test
    fun testReadPreview_corruptJson() = runTest {
        val file = File(tempDir, "corrupt.iyxv")
        FileOutputStream(file).use {
            it.write("not valid json at all".toByteArray())
        }

        val result = backupRepository.readPreview(Uri.fromFile(file))
        assertTrue(result.isFailure)
    }

    @Test
    fun testDeleteAllData() = runTest {
        vouchRepository.insert(
            VouchWithPerson(
                vouch = Vouch(amount = 1000L),
                persons = listOf(PersonWithAmount(Person(name = "Alice"), 1000L))
            )
        )
        assertEquals(1, vouchRepository.getAll().size)

        val deleteResult = backupRepository.deleteAllData()
        assertTrue(deleteResult.isSuccess)
        assertEquals(0, vouchRepository.getAll().size)
    }
}
