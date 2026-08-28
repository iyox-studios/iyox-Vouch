package com.iyox.vouch.core.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.iyox.vouch.core.model.Vouch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Type
import java.sql.Timestamp

class BackupSerializationTest {

    private lateinit var gson: Gson

    @Before
    fun setUp() {
        gson = GsonBuilder()
            .registerTypeAdapter(Timestamp::class.java, object : JsonSerializer<Timestamp>, JsonDeserializer<Timestamp> {
                override fun serialize(src: Timestamp, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
                    return JsonPrimitive(src.time)
                }

                override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Timestamp {
                    return Timestamp(json.asLong)
                }
            })
            .create()
    }

    @Test
    fun testTimestampSerializationAndDeserialization() {
        val now = System.currentTimeMillis()
        val timestamp = Timestamp(now)
        val json = gson.toJson(timestamp)
        assertEquals(now.toString(), json)

        val deserialized = gson.fromJson(json, Timestamp::class.java)
        assertEquals(now, deserialized.time)
    }

    @Test
    fun testExportVouchSerialization() {
        val timestamp = Timestamp(1700000000000L)
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

        val json = gson.toJson(exportVouch)
        assertTrue(json.contains("\"name\":\"Alice\""))
        assertTrue(json.contains("\"amount\":1500"))
        assertTrue(json.contains("\"settled\":false"))
        assertTrue(json.contains("\"settled\":true"))
        assertTrue(json.contains("\"description\":\"Dinner split\""))
        assertTrue(json.contains("1700000000000"))

        val deserialized = gson.fromJson(json, ExportVouch::class.java)
        assertEquals(exportVouch.vouch.id, deserialized.vouch.id)
        assertEquals(exportVouch.vouch.amount, deserialized.vouch.amount)
        assertEquals(exportVouch.vouch.description, deserialized.vouch.description)
        assertEquals(exportVouch.vouch.given, deserialized.vouch.given)
        assertEquals(exportVouch.vouch.timestamp.time, deserialized.vouch.timestamp.time)
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

        val dataElement = gson.toJsonTree(vouches)
        val envelope = IyoxEnvelope(
            appSignature = IyoxEnvelope.APP_SIGNATURE,
            type = IyoxEnvelope.TYPE_BACKUP,
            version = IyoxEnvelope.VERSION_1_0,
            data = dataElement
        )

        val envelopeJson = gson.toJson(envelope)
        assertTrue(envelopeJson.contains(IyoxEnvelope.APP_SIGNATURE))
        assertTrue(envelopeJson.contains(IyoxEnvelope.TYPE_BACKUP))
        assertTrue(envelopeJson.contains(IyoxEnvelope.VERSION_1_0))

        val deserializedEnvelope = gson.fromJson(envelopeJson, IyoxEnvelope::class.java)
        assertNotNull(deserializedEnvelope)
        assertEquals(IyoxEnvelope.APP_SIGNATURE, deserializedEnvelope.appSignature)
        assertEquals(IyoxEnvelope.TYPE_BACKUP, deserializedEnvelope.type)
        assertEquals(IyoxEnvelope.VERSION_1_0, deserializedEnvelope.version)

        val listType = object : TypeToken<List<ExportVouch>>() {}.type
        val deserializedVouches: List<ExportVouch> = gson.fromJson(deserializedEnvelope.data, listType)
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

        val dataElement = gson.toJsonTree(singleExport)
        val envelope = IyoxEnvelope(
            appSignature = IyoxEnvelope.APP_SIGNATURE,
            type = IyoxEnvelope.TYPE_SINGLE,
            version = IyoxEnvelope.VERSION_1_0,
            data = dataElement
        )

        val envelopeJson = gson.toJson(envelope)
        val deserializedEnvelope = gson.fromJson(envelopeJson, IyoxEnvelope::class.java)
        assertEquals(IyoxEnvelope.TYPE_SINGLE, deserializedEnvelope.type)

        val deserializedSingle = gson.fromJson(deserializedEnvelope.data, ExportVouch::class.java)
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

        val deserialized = gson.fromJson(invalidEnvelope, IyoxEnvelope::class.java)
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
}
