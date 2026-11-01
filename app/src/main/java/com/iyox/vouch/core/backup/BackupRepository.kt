package com.iyox.vouch.core.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchPersonCrossRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

sealed interface BackupImportResult {
    data class BackupSuccess(val count: Int) : BackupImportResult
    data class SingleSuccess(val exportVouch: ExportVouch) : BackupImportResult
}

class InvalidSignatureException(message: String = "Invalid app signature") : IllegalArgumentException(message)
class UnsupportedBackupTypeException(type: String) : IllegalArgumentException("Unsupported backup type: $type")

private data class ParsedBackup(
    val type: String,
    val version: String,
    val vouches: List<ExportVouch>,
    val singleVouch: ExportVouch?,
    val uniquePersonCount: Int,
    val cleanJsonText: String
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val vouchRepository: VouchRepository
) {

    private val json: Json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun readPreview(uri: Uri): Result<BackupPreview> = withContext(Dispatchers.IO) {
        try {
            val inputStream = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open input stream for URI: $uri", e)
                return@withContext Result.failure(e)
            } ?: return@withContext Result.failure(IllegalStateException("Failed to open input stream"))

            val rawText = inputStream.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }

            val parsedResult = parsePayload(rawText)
            if (parsedResult.isFailure) {
                return@withContext Result.failure(parsedResult.exceptionOrNull()!!)
            }

            val parsed = parsedResult.getOrThrow()
            Result.success(
                BackupPreview(
                    uri = uri.toString(),
                    type = parsed.type,
                    version = parsed.version,
                    vouchCount = parsed.vouches.size,
                    personCount = parsed.uniquePersonCount,
                    cachedPayload = parsed.cleanJsonText
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "readPreview error for $uri", e)
            Result.failure(e)
        }
    }

    suspend fun exportData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val allVouchesWithPersons = vouchRepository.getAll()
            val exportVouches = allVouchesWithPersons.map { vouchWithPerson ->
                ExportVouch(
                    vouch = vouchWithPerson.vouch,
                    persons = vouchWithPerson.persons.map { personWithAmount ->
                        ExportPersonAmount(
                            name = personWithAmount.person.name,
                            amount = personWithAmount.amount,
                            settled = personWithAmount.settled,
                            isCustom = personWithAmount.isCustom,
                            isSelf = personWithAmount.isSelf
                        )
                    }
                )
            }

            val dataElement = json.encodeToJsonElement(exportVouches)
            val envelope = IyoxEnvelope(
                type = IyoxEnvelope.TYPE_BACKUP,
                data = dataElement
            )
            val jsonText = json.encodeToString(envelope)

            val outputStream = try {
                context.contentResolver.openOutputStream(uri, "wt")
            } catch (_: Exception) {
                null
            } ?: context.contentResolver.openOutputStream(uri)
              ?: return@withContext Result.failure(IllegalStateException("Could not open output stream"))

            outputStream.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonText)
                    writer.flush()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "exportData error for $uri", e)
            Result.failure(e)
        }
    }

    suspend fun importFromPreview(preview: BackupPreview): Result<BackupImportResult> = withContext(Dispatchers.IO) {
        if (!preview.cachedPayload.isNullOrBlank()) {
            val parsedResult = parsePayload(preview.cachedPayload)
            if (parsedResult.isFailure) {
                return@withContext Result.failure(parsedResult.exceptionOrNull()!!)
            }
            applyParsedBackup(parsedResult.getOrThrow())
        } else {
            importData(Uri.parse(preview.uri))
        }
    }

    suspend fun importData(uri: Uri): Result<BackupImportResult> = withContext(Dispatchers.IO) {
        try {
            val inputStream = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open input stream for URI: $uri", e)
                return@withContext Result.failure(e)
            } ?: return@withContext Result.failure(IllegalStateException("Failed to open input stream"))

            val rawText = inputStream.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }

            val parsedResult = parsePayload(rawText)
            if (parsedResult.isFailure) {
                return@withContext Result.failure(parsedResult.exceptionOrNull()!!)
            }
            applyParsedBackup(parsedResult.getOrThrow())
        } catch (e: Exception) {
            Log.e(TAG, "importData error for $uri", e)
            Result.failure(e)
        }
    }

    private fun parsePayload(rawJsonText: String): Result<ParsedBackup> {
        val cleanJson = rawJsonText.removePrefix("\uFEFF").trim()
        if (cleanJson.isEmpty()) {
            return Result.failure(IllegalArgumentException("Backup file is empty"))
        }

        val rootElement: JsonElement = try {
            json.parseToJsonElement(cleanJson)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Invalid JSON format", e))
        }

        val type: String
        val version: String
        val dataElement: JsonElement

        if (rootElement is JsonObject) {
            val rootObj = rootElement

            val signature = when {
                rootObj.containsKey("_app_signature") -> rootObj["_app_signature"]?.asStringOrNull()
                rootObj.containsKey("app_signature") -> rootObj["app_signature"]?.asStringOrNull()
                rootObj.containsKey("signature") -> rootObj["signature"]?.asStringOrNull()
                else -> null
            }

            if (signature != null && signature != IyoxEnvelope.APP_SIGNATURE) {
                return Result.failure(InvalidSignatureException("Invalid app signature: $signature"))
            }

            type = when {
                rootObj.containsKey("type") -> rootObj["type"]?.asStringOrNull() ?: IyoxEnvelope.TYPE_BACKUP
                else -> IyoxEnvelope.TYPE_BACKUP
            }

            if (type != IyoxEnvelope.TYPE_BACKUP && type != IyoxEnvelope.TYPE_SINGLE) {
                return Result.failure(UnsupportedBackupTypeException(type))
            }

            version = when {
                rootObj.containsKey("version") -> rootObj["version"]?.asStringOrNull() ?: IyoxEnvelope.VERSION_1_0
                else -> IyoxEnvelope.VERSION_1_0
            }

            dataElement = when {
                rootObj.containsKey("data") -> {
                    val d = rootObj["data"]
                    if (d != null && d is JsonPrimitive && d !is JsonNull) {
                        try {
                            json.parseToJsonElement(d.content)
                        } catch (_: Exception) {
                            d
                        }
                    } else {
                        d ?: rootObj
                    }
                }
                rootObj.containsKey("vouches") -> rootObj["vouches"] ?: rootObj
                else -> rootObj
            }
        } else if (rootElement is JsonArray) {
            type = IyoxEnvelope.TYPE_BACKUP
            version = IyoxEnvelope.VERSION_1_0
            dataElement = rootElement
        } else {
            return Result.failure(IllegalArgumentException("Invalid JSON format: expected object or array"))
        }

        return when (type) {
            IyoxEnvelope.TYPE_BACKUP -> {
                val vouches = mutableListOf<ExportVouch>()
                if (dataElement is JsonArray) {
                    for (elem in dataElement) {
                        parseExportVouch(elem)?.let { vouches.add(it) }
                    }
                } else if (dataElement is JsonObject) {
                    parseExportVouch(dataElement)?.let { vouches.add(it) }
                }

                val uniquePersons = vouches.flatMap { it.persons.orEmpty() }
                    .mapNotNull { it.name.trim().takeIf { n -> n.isNotEmpty() } }
                    .distinct()
                    .size

                Result.success(
                    ParsedBackup(
                        type = type,
                        version = version,
                        vouches = vouches,
                        singleVouch = null,
                        uniquePersonCount = uniquePersons,
                        cleanJsonText = cleanJson
                    )
                )
            }
            IyoxEnvelope.TYPE_SINGLE -> {
                val singleVouch = if (dataElement is JsonObject) {
                    parseExportVouch(dataElement)
                } else if (dataElement is JsonArray && dataElement.isNotEmpty()) {
                    parseExportVouch(dataElement[0])
                } else null

                if (singleVouch == null) {
                    return Result.failure(IllegalArgumentException("Invalid or empty single vouch payload"))
                }

                val uniquePersons = singleVouch.persons.orEmpty()
                    .mapNotNull { it.name.trim().takeIf { n -> n.isNotEmpty() } }
                    .distinct()
                    .size

                Result.success(
                    ParsedBackup(
                        type = type,
                        version = version,
                        vouches = listOf(singleVouch),
                        singleVouch = singleVouch,
                        uniquePersonCount = uniquePersons,
                        cleanJsonText = cleanJson
                    )
                )
            }
            else -> Result.failure(UnsupportedBackupTypeException(type))
        }
    }

    private fun parseExportVouch(element: JsonElement): ExportVouch? {
        if (element !is JsonObject) return null
        val obj = element

        val vouchObj = if (obj["vouch"] is JsonObject) {
            obj["vouch"] as JsonObject
        } else {
            obj
        }

        val amount = parseAmount(vouchObj["amount"]) ?: 0L
        val description = vouchObj["description"]?.asStringOrNull() ?: ""
        val given = parseBoolean(vouchObj["given"], default = true)
        val timestamp = parseTimestamp(vouchObj["timestamp"])
        val id = vouchObj["id"]?.asLongOrNull() ?: 0L

        val vouch = Vouch(
            id = id,
            amount = amount,
            description = description,
            given = given,
            timestamp = timestamp
        )

        val personsElement = when {
            obj.containsKey("persons") -> obj["persons"]
            vouchObj.containsKey("persons") -> vouchObj["persons"]
            else -> null
        }

        val persons = mutableListOf<ExportPersonAmount>()
        if (personsElement is JsonArray) {
            for (pElem in personsElement) {
                if (pElem is JsonObject) {
                    val pObj = pElem
                    val name = when {
                        pObj.containsKey("name") -> pObj["name"]?.asStringOrNull()
                        pObj.containsKey("personName") -> pObj["personName"]?.asStringOrNull()
                        pObj.containsKey("person_name") -> pObj["person_name"]?.asStringOrNull()
                        pObj["person"] is JsonObject -> {
                            (pObj["person"] as JsonObject)["name"]?.asStringOrNull()
                        }
                        else -> null
                    }?.trim().orEmpty()

                    if (name.isNotEmpty()) {
                        val pAmount = parseAmount(pObj["amount"]) ?: amount
                        val settled = parseBoolean(pObj["settled"], default = false)
                        val isCustom = parseBoolean(pObj["isCustom"], default = false)
                        val isSelf = parseBoolean(pObj["isSelf"], default = false)
                        persons.add(ExportPersonAmount(name = name, amount = pAmount, settled = settled, isCustom = isCustom, isSelf = isSelf))
                    }
                } else if (pElem is JsonPrimitive && pElem !is JsonNull) {
                    val name = pElem.content.trim()
                    if (name.isNotEmpty()) {
                        persons.add(ExportPersonAmount(name = name, amount = amount, settled = false))
                    }
                }
            }
        }

        return ExportVouch(vouch = vouch, persons = persons)
    }

    private fun parseAmount(element: JsonElement?): Long? {
        if (element == null || element is JsonNull) return null
        return try {
            if (element is JsonPrimitive) {
                val rawStr = element.content.trim()
                if (rawStr.contains(".")) {
                    val parts = rawStr.split(".")
                    val whole = parts[0].toLongOrNull() ?: 0L
                    val frac = (parts.getOrNull(1) ?: "").padEnd(2, '0').take(2).toLongOrNull() ?: 0L
                    (whole * 100L) + frac
                } else {
                    rawStr.toLongOrNull()
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTimestamp(element: JsonElement?): Long {
        if (element == null || element is JsonNull) return System.currentTimeMillis()
        return try {
            if (element is JsonPrimitive) {
                val str = element.content.trim()
                str.toLongOrNull() ?: parseDateString(str) ?: System.currentTimeMillis()
            } else {
                System.currentTimeMillis()
            }
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun parseDateString(str: String): Long? {
        val patterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(str)
                if (date != null) return date.time
            } catch (_: Exception) {
                // Try next pattern
            }
        }
        return null
    }

    private fun parseBoolean(element: JsonElement?, default: Boolean): Boolean {
        if (element == null || element is JsonNull) return default
        return try {
            if (element is JsonPrimitive) {
                val str = element.content.trim().lowercase(Locale.ROOT)
                when {
                    str == "true" || str == "1" || str == "yes" -> true
                    str == "false" || str == "0" || str == "no" -> false
                    str.toIntOrNull() != null -> str.toInt() != 0
                    else -> default
                }
            } else default
        } catch (_: Exception) {
            default
        }
    }

    private fun JsonElement.asStringOrNull(): String? =
        (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

    private fun JsonElement.asLongOrNull(): Long? =
        (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content?.toLongOrNull()

    private suspend fun applyParsedBackup(parsed: ParsedBackup): Result<BackupImportResult> {
        return try {
            when (parsed.type) {
                IyoxEnvelope.TYPE_BACKUP -> {
                    val vouches = parsed.vouches
                    database.withTransaction {
                        database.vouchDao().deleteAll()
                        database.personDao().deleteAll()

                        val allNames = vouches.flatMap { it.persons.orEmpty() }
                            .mapNotNull { it.name.trim().takeIf { n -> n.isNotEmpty() } }
                            .distinct()

                        val personMap = if (allNames.isNotEmpty()) {
                            database.personDao().insertAll(allNames.map { Person(name = it) })
                            allNames.chunked(900).flatMap { database.personDao().getByNames(it) }
                                .associateBy { it.name }
                        } else {
                            emptyMap()
                        }

                        val allCrossRefs = mutableListOf<VouchPersonCrossRef>()
                        vouches.forEach { export ->
                            val newVouchId = database.vouchDao()
                                .insert(export.vouch.copy(id = 0L))

                            val seenPersonIds = mutableSetOf<Long>()
                            val crossRefs = export.persons.orEmpty().mapNotNull { personAmount ->
                                val normalizedName = personAmount.name.trim()
                                val storedPerson = personMap[normalizedName] ?: return@mapNotNull null
                                if (!seenPersonIds.add(storedPerson.id)) return@mapNotNull null
                                VouchPersonCrossRef(
                                    vouchId = newVouchId,
                                    personId = storedPerson.id,
                                    amount = personAmount.amount,
                                    settled = personAmount.settled,
                                    isCustom = personAmount.isCustom,
                                    isSelf = personAmount.isSelf
                                )
                            }
                            allCrossRefs.addAll(crossRefs)
                        }

                        if (allCrossRefs.isNotEmpty()) {
                            database.vouchDao().insertCrossRefs(allCrossRefs)
                        }
                    }

                    Result.success(BackupImportResult.BackupSuccess(vouches.size))
                }
                IyoxEnvelope.TYPE_SINGLE -> {
                    val singleVouch = parsed.singleVouch
                        ?: return Result.failure(IllegalArgumentException("Invalid or empty single vouch payload"))

                    database.withTransaction {
                        val allNames = singleVouch.persons.orEmpty()
                            .mapNotNull { it.name.trim().takeIf { n -> n.isNotEmpty() } }
                            .distinct()

                        val personMap = if (allNames.isNotEmpty()) {
                            database.personDao().insertAll(allNames.map { Person(name = it) })
                            allNames.chunked(900).flatMap { database.personDao().getByNames(it) }
                                .associateBy { it.name }
                        } else {
                            emptyMap()
                        }

                        val newVouchId = database.vouchDao()
                            .insert(singleVouch.vouch.copy(id = 0L))

                        val seenPersonIds = mutableSetOf<Long>()
                        val crossRefs = singleVouch.persons.orEmpty().mapNotNull { personAmount ->
                            val normalizedName = personAmount.name.trim()
                            val storedPerson = personMap[normalizedName] ?: return@mapNotNull null
                            if (!seenPersonIds.add(storedPerson.id)) return@mapNotNull null
                            VouchPersonCrossRef(
                                vouchId = newVouchId,
                                personId = storedPerson.id,
                                amount = personAmount.amount,
                                settled = personAmount.settled,
                                isCustom = personAmount.isCustom,
                                isSelf = personAmount.isSelf
                            )
                        }

                        if (crossRefs.isNotEmpty()) {
                            database.vouchDao().insertCrossRefs(crossRefs)
                        }
                    }

                    Result.success(BackupImportResult.SingleSuccess(singleVouch))
                }
                else -> {
                    Result.failure(UnsupportedBackupTypeException(parsed.type))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyParsedBackup error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                database.vouchDao().deleteAll()
                database.personDao().deleteAll()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllData error", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "BackupRepository"
    }
}
