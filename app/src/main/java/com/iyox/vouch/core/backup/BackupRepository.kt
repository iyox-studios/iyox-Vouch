package com.iyox.vouch.core.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchPersonCrossRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.io.StringReader
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

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

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
                            isCustom = personWithAmount.isCustom
                        )
                    }
                )
            }

            val dataElement = gson.toJsonTree(exportVouches)
            val envelope = IyoxEnvelope(
                type = IyoxEnvelope.TYPE_BACKUP,
                data = dataElement
            )
            val json = gson.toJson(envelope)

            val outputStream = try {
                context.contentResolver.openOutputStream(uri, "wt")
            } catch (_: Exception) {
                null
            } ?: context.contentResolver.openOutputStream(uri)
              ?: return@withContext Result.failure(IllegalStateException("Could not open output stream"))

            outputStream.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.write(json)
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
            val reader = JsonReader(StringReader(cleanJson)).apply { isLenient = true }
            JsonParser.parseReader(reader)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Invalid JSON format", e))
        }

        val type: String
        val version: String
        val dataElement: JsonElement

        if (rootElement.isJsonObject) {
            val rootObj = rootElement.asJsonObject

            val signature = when {
                rootObj.has("_app_signature") -> rootObj.get("_app_signature").asStringOrNull()
                rootObj.has("app_signature") -> rootObj.get("app_signature").asStringOrNull()
                rootObj.has("signature") -> rootObj.get("signature").asStringOrNull()
                else -> null
            }

            if (signature != null && signature != IyoxEnvelope.APP_SIGNATURE) {
                return Result.failure(InvalidSignatureException("Invalid app signature: $signature"))
            }

            type = when {
                rootObj.has("type") -> rootObj.get("type").asStringOrNull() ?: IyoxEnvelope.TYPE_BACKUP
                else -> IyoxEnvelope.TYPE_BACKUP
            }

            if (type != IyoxEnvelope.TYPE_BACKUP && type != IyoxEnvelope.TYPE_SINGLE) {
                return Result.failure(UnsupportedBackupTypeException(type))
            }

            version = when {
                rootObj.has("version") -> rootObj.get("version").asStringOrNull() ?: IyoxEnvelope.VERSION_1_0
                else -> IyoxEnvelope.VERSION_1_0
            }

            dataElement = when {
                rootObj.has("data") -> {
                    val d = rootObj.get("data")
                    if (d != null && d.isJsonPrimitive && d.asJsonPrimitive.isString) {
                        try {
                            val reader = JsonReader(StringReader(d.asString)).apply { isLenient = true }
                            JsonParser.parseReader(reader)
                        } catch (_: Exception) {
                            d
                        }
                    } else {
                        d ?: rootObj
                    }
                }
                rootObj.has("vouches") -> rootObj.get("vouches")
                else -> rootObj
            }
        } else if (rootElement.isJsonArray) {
            type = IyoxEnvelope.TYPE_BACKUP
            version = IyoxEnvelope.VERSION_1_0
            dataElement = rootElement
        } else {
            return Result.failure(IllegalArgumentException("Invalid JSON format: expected object or array"))
        }

        return when (type) {
            IyoxEnvelope.TYPE_BACKUP -> {
                val vouches = mutableListOf<ExportVouch>()
                if (dataElement.isJsonArray) {
                    for (elem in dataElement.asJsonArray) {
                        parseExportVouch(elem)?.let { vouches.add(it) }
                    }
                } else if (dataElement.isJsonObject) {
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
                val singleVouch = if (dataElement.isJsonObject) {
                    parseExportVouch(dataElement)
                } else if (dataElement.isJsonArray && dataElement.asJsonArray.size() > 0) {
                    parseExportVouch(dataElement.asJsonArray[0])
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
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject

        val vouchObj = if (obj.has("vouch") && obj.get("vouch").isJsonObject) {
            obj.getAsJsonObject("vouch")
        } else {
            obj
        }

        val amount = parseAmount(vouchObj.get("amount")) ?: 0L
        val description = vouchObj.get("description")?.asStringOrNull() ?: ""
        val given = parseBoolean(vouchObj.get("given"), default = true)
        val timestamp = parseTimestamp(vouchObj.get("timestamp"))
        val id = vouchObj.get("id")?.asLongOrNull() ?: 0L

        val vouch = Vouch(
            id = id,
            amount = amount,
            description = description,
            given = given,
            timestamp = timestamp
        )

        val personsElement = when {
            obj.has("persons") -> obj.get("persons")
            vouchObj.has("persons") -> vouchObj.get("persons")
            else -> null
        }

        val persons = mutableListOf<ExportPersonAmount>()
        if (personsElement != null && personsElement.isJsonArray) {
            for (pElem in personsElement.asJsonArray) {
                if (pElem.isJsonObject) {
                    val pObj = pElem.asJsonObject
                    val name = when {
                        pObj.has("name") -> pObj.get("name").asStringOrNull()
                        pObj.has("personName") -> pObj.get("personName").asStringOrNull()
                        pObj.has("person_name") -> pObj.get("person_name").asStringOrNull()
                        pObj.has("person") && pObj.get("person").isJsonObject -> {
                            pObj.getAsJsonObject("person").get("name")?.asStringOrNull()
                        }
                        else -> null
                    }?.trim().orEmpty()

                    if (name.isNotEmpty()) {
                        val pAmount = parseAmount(pObj.get("amount")) ?: amount
                        val settled = parseBoolean(pObj.get("settled"), default = false)
                        val isCustom = parseBoolean(pObj.get("isCustom"), default = false)
                        persons.add(ExportPersonAmount(name = name, amount = pAmount, settled = settled, isCustom = isCustom))
                    }
                } else if (pElem.isJsonPrimitive) {
                    val name = pElem.asString.trim()
                    if (name.isNotEmpty()) {
                        persons.add(ExportPersonAmount(name = name, amount = amount, settled = false))
                    }
                }
            }
        }

        return ExportVouch(vouch = vouch, persons = persons)
    }

    private fun parseAmount(element: JsonElement?): Long? {
        if (element == null || element.isJsonNull) return null
        return try {
            if (element.isJsonPrimitive) {
                val prim = element.asJsonPrimitive
                if (prim.isNumber) {
                    val rawStr = prim.asString.trim()
                    if (rawStr.contains(".")) {
                        val parts = rawStr.split(".")
                        val whole = parts[0].toLongOrNull() ?: 0L
                        val frac = (parts.getOrNull(1) ?: "").padEnd(2, '0').take(2).toLongOrNull() ?: 0L
                        (whole * 100L) + frac
                    } else {
                        prim.asLong
                    }
                } else if (prim.isString) {
                    val str = prim.asString.trim()
                    if (str.contains(".")) {
                        val parts = str.split(".")
                        val whole = parts[0].toLongOrNull() ?: 0L
                        val frac = (parts.getOrNull(1) ?: "").padEnd(2, '0').take(2).toLongOrNull() ?: 0L
                        (whole * 100L) + frac
                    } else {
                        str.toLongOrNull()
                    }
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTimestamp(element: JsonElement?): Long {
        if (element == null || element.isJsonNull) return System.currentTimeMillis()
        return try {
            if (element.isJsonPrimitive) {
                val prim = element.asJsonPrimitive
                if (prim.isNumber) {
                    prim.asLong
                } else if (prim.isString) {
                    val str = prim.asString.trim()
                    str.toLongOrNull() ?: parseDateString(str) ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }
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
        if (element == null || element.isJsonNull) return default
        return try {
            if (element.isJsonPrimitive) {
                val prim = element.asJsonPrimitive
                if (prim.isBoolean) prim.asBoolean
                else if (prim.isNumber) prim.asInt != 0
                else if (prim.isString) prim.asString.trim().lowercase(Locale.ROOT) in listOf("true", "1", "yes")
                else default
            } else default
        } catch (_: Exception) {
            default
        }
    }

    private fun JsonElement.asStringOrNull(): String? =
        if (isJsonPrimitive && asJsonPrimitive.isString) asString else if (isJsonPrimitive) asString else null

    private fun JsonElement.asLongOrNull(): Long? =
        if (isJsonPrimitive && asJsonPrimitive.isNumber) asLong else if (isJsonPrimitive) asString.toLongOrNull() else null

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
                                    isCustom = personAmount.isCustom
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
                                isCustom = personAmount.isCustom
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
