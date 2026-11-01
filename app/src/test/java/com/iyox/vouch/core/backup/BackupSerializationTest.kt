package com.iyox.vouch.core.backup

import com.iyox.vouch.core.model.Vouch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupSerializationTest {

    private lateinit var json: Json

    @Before
    fun setUp() {
        json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    @Test
    fun testExportVouchSerialization() {
        val timestamp = 1700000000000L
        val exportVouch = ExportVouch(
            vouch = Vouch(
                id = 1,
                amount = 2500L,
                description = "Dinner split",
                given = true,
                timestamp = timestamp
            ),
            persons = listOf(
                ExportPersonAmount(name = "Alice", amount = 1500L, settled = false),
                ExportPersonAmount(name = "Bob", amount = 1000L, settled = true)
            )
        )

        val jsonStr = json.encodeToString(exportVouch)
        assertTrue(jsonStr.contains("\"name\":\"Alice\""))
        assertTrue(jsonStr.contains("\"amount\":1500"))
        assertTrue(jsonStr.contains("\"settled\":false"))
        assertTrue(jsonStr.contains("\"settled\":true"))
        assertTrue(jsonStr.contains("\"description\":\"Dinner split\""))
        assertTrue(jsonStr.contains("1700000000000"))

        val deserialized = json.decodeFromString<ExportVouch>(jsonStr)
        assertEquals(exportVouch.vouch.id, deserialized.vouch.id)
        assertEquals(exportVouch.vouch.amount, deserialized.vouch.amount)
        assertEquals(exportVouch.vouch.description, deserialized.vouch.description)
        assertEquals(exportVouch.vouch.given, deserialized.vouch.given)
        assertEquals(exportVouch.vouch.timestamp, deserialized.vouch.timestamp)
        assertEquals(2, deserialized.persons.size)
        assertEquals("Alice", deserialized.persons[0].name)
        assertEquals(1500L, deserialized.persons[0].amount)
        assertFalse(deserialized.persons[0].settled)
        assertEquals("Bob", deserialized.persons[1].name)
        assertEquals(1000L, deserialized.persons[1].amount)
        assertTrue(deserialized.persons[1].settled)
    }

    @Test
    fun testEnvelopeBackupSerialization() {
        val vouches = listOf(
            ExportVouch(
                vouch = Vouch(id = 1, amount = 3000L, description = "Groceries", given = true),
                persons = listOf(ExportPersonAmount(name = "Charlie", amount = 3000L, settled = false))
            )
        )

        val dataElement = json.encodeToJsonElement(vouches)
        val envelope = IyoxEnvelope(
            appSignature = IyoxEnvelope.APP_SIGNATURE,
            type = IyoxEnvelope.TYPE_BACKUP,
            version = IyoxEnvelope.VERSION_1_0,
            data = dataElement
        )

        val envelopeJson = json.encodeToString(envelope)
        assertTrue(envelopeJson.contains(IyoxEnvelope.APP_SIGNATURE))
        assertTrue(envelopeJson.contains(IyoxEnvelope.TYPE_BACKUP))
        assertTrue(envelopeJson.contains(IyoxEnvelope.VERSION_1_0))

        val deserializedEnvelope = json.decodeFromString<IyoxEnvelope>(envelopeJson)
        assertNotNull(deserializedEnvelope)
        assertEquals(IyoxEnvelope.APP_SIGNATURE, deserializedEnvelope.appSignature)
        assertEquals(IyoxEnvelope.TYPE_BACKUP, deserializedEnvelope.type)
        assertEquals(IyoxEnvelope.VERSION_1_0, deserializedEnvelope.version)

        val deserializedVouches: List<ExportVouch> = json.decodeFromJsonElement(deserializedEnvelope.data)
        assertEquals(1, deserializedVouches.size)
        assertEquals("Groceries", deserializedVouches[0].vouch.description)
        assertEquals(3000L, deserializedVouches[0].vouch.amount)
        assertEquals("Charlie", deserializedVouches[0].persons[0].name)
    }

    @Test
    fun testEnvelopeSingleVouchSerialization() {
        val singleExport = ExportVouch(
            vouch = Vouch(id = 5, amount = 1200L, description = "Coffee", given = false),
            persons = listOf(ExportPersonAmount(name = "Dave", amount = 1200L, settled = true))
        )

        val dataElement = json.encodeToJsonElement(singleExport)
        val envelope = IyoxEnvelope(
            appSignature = IyoxEnvelope.APP_SIGNATURE,
            type = IyoxEnvelope.TYPE_SINGLE,
            version = IyoxEnvelope.VERSION_1_0,
            data = dataElement
        )

        val envelopeJson = json.encodeToString(envelope)
        val deserializedEnvelope = json.decodeFromString<IyoxEnvelope>(envelopeJson)
        assertEquals(IyoxEnvelope.TYPE_SINGLE, deserializedEnvelope.type)

        val deserializedSingle: ExportVouch = json.decodeFromJsonElement(deserializedEnvelope.data)
        assertEquals(5, deserializedSingle.vouch.id)
        assertEquals("Coffee", deserializedSingle.vouch.description)
        assertEquals("Dave", deserializedSingle.persons[0].name)
        assertTrue(deserializedSingle.persons[0].settled)
    }

    @Test
    fun testInvalidSignatureDetection() {
        val invalidEnvelope = """
            {
                "_app_signature": "com.other.app",
                "type": "backup",
                "version": "1.0",
                "data": []
            }
        """.trimIndent()

        val deserialized = json.decodeFromString<IyoxEnvelope>(invalidEnvelope)
        assertNotNull(deserialized)
        assertTrue(deserialized.appSignature != IyoxEnvelope.APP_SIGNATURE)
    }

    @Test
    fun testConstants() {
        assertEquals("com.iyox.vouch", IyoxEnvelope.APP_SIGNATURE)
        assertEquals("1.0", IyoxEnvelope.VERSION_1_0)
        assertEquals("backup", IyoxEnvelope.TYPE_BACKUP)
        assertEquals("single", IyoxEnvelope.TYPE_SINGLE)
        assertEquals("iyxv", IyoxEnvelope.FILE_EXTENSION)
        assertEquals("application/x-iyxv", IyoxEnvelope.MIME_TYPE)
        assertEquals("vouch_backup.iyxv", IyoxEnvelope.DEFAULT_BACKUP_FILE_NAME)
    }

    @Test
    fun testGenerateBackupFileName() {
        val calendar = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.SEPTEMBER, 9, 11, 59, 30)
        }
        val fileName = IyoxEnvelope.generateBackupFileName(calendar.time)
        assertEquals("vouch_backup_20260909_115930.iyxv", fileName)
    }

    @Test
    fun testAlternateSignatureDeserialization() {
        val jsonStr = """
            {
                "app_signature": "com.iyox.vouch",
                "type": "backup",
                "version": "1.0",
                "data": []
            }
        """.trimIndent()

        val deserialized = json.decodeFromString<IyoxEnvelope>(jsonStr)
        assertNotNull(deserialized)
        assertEquals(IyoxEnvelope.APP_SIGNATURE, deserialized.appSignature)
    }

    @Test
    fun testAlternatePersonNameDeserialization() {
        val jsonStr = """
            {
                "personName": "Eve",
                "amount": 500,
                "settled": false
            }
        """.trimIndent()

        val deserialized = json.decodeFromString<ExportPersonAmount>(jsonStr)
        assertNotNull(deserialized)
        assertEquals("Eve", deserialized.name)
        assertEquals(500L, deserialized.amount)
    }
}
