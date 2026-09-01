package com.iyox.vouch.core.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.VouchPersonCrossRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

sealed interface BackupImportResult {
    data class BackupSuccess(val count: Int) : BackupImportResult
    data class SingleSuccess(val exportVouch: ExportVouch) : BackupImportResult
}

class InvalidSignatureException(message: String = "Invalid app signature") : IllegalArgumentException(message)
class UnsupportedBackupTypeException(type: String) : IllegalArgumentException("Unsupported backup type: $type")

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
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Failed to open input stream"))

            inputStream.use { stream ->
                InputStreamReader(stream).use { reader ->
                    val envelope = try {
                        gson.fromJson(reader, IyoxEnvelope::class.java)
                    } catch (e: Exception) {
                        return@withContext Result.failure(IllegalArgumentException("Invalid JSON format", e))
                    }

                    if (envelope == null || envelope.appSignature != IyoxEnvelope.APP_SIGNATURE) {
                        return@withContext Result.failure(InvalidSignatureException("Invalid app signature: ${envelope?.appSignature}"))
                    }

                    when (envelope.type) {
                        IyoxEnvelope.TYPE_BACKUP -> {
                            val itemType = object : TypeToken<List<ExportVouch>>() {}.type
                            val vouches: List<ExportVouch> = gson.fromJson(envelope.data, itemType)
                                ?: emptyList()

                            val uniquePersons = vouches.flatMap { it.persons }
                                .map { it.name.trim() }
                                .filter { it.isNotBlank() }
                                .distinct()
                                .size

                            Result.success(
                                BackupPreview(
                                    uri = uri,
                                    type = envelope.type,
                                    version = envelope.version,
                                    vouchCount = vouches.size,
                                    personCount = uniquePersons
                                )
                            )
                        }
                        IyoxEnvelope.TYPE_SINGLE -> {
                            val singleVouch = gson.fromJson(envelope.data, ExportVouch::class.java)
                            val uniquePersons = singleVouch?.persons
                                ?.map { it.name.trim() }
                                ?.filter { it.isNotBlank() }
                                ?.distinct()
                                ?.size ?: 0

                            Result.success(
                                BackupPreview(
                                    uri = uri,
                                    type = envelope.type,
                                    version = envelope.version,
                                    vouchCount = 1,
                                    personCount = uniquePersons
                                )
                            )
                        }
                        else -> {
                            Result.failure(UnsupportedBackupTypeException(envelope.type))
                        }
                    }
                }
            }
        } catch (e: Exception) {
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
                            settled = personWithAmount.settled
                        )
                    }
                )
            }

            // 1. Convert the list to a Gson JsonElement
            val dataElement = gson.toJsonTree(exportVouches)

            // 2. Wrap it in our custom envelope
            val envelope = IyoxEnvelope(
                type = IyoxEnvelope.TYPE_BACKUP,
                data = dataElement
            )

            // 3. Convert the whole envelope to a JSON string
            val json = gson.toJson(envelope)

            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Could not open output stream"))

            outputStream.use { stream ->
                OutputStreamWriter(stream).use { writer ->
                    writer.write(json)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importData(uri: Uri): Result<BackupImportResult> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Failed to open input stream"))

            inputStream.use { stream ->
                InputStreamReader(stream).use { reader ->
                    // 1. Read the file into the Envelope first
                    val envelope = try {
                        gson.fromJson(reader, IyoxEnvelope::class.java)
                    } catch (e: Exception) {
                        return@withContext Result.failure(IllegalArgumentException("Invalid JSON format", e))
                    }

                    // 2. Validate the file belongs to our app
                    if (envelope == null || envelope.appSignature != IyoxEnvelope.APP_SIGNATURE) {
                        return@withContext Result.failure(InvalidSignatureException("Invalid app signature: ${envelope?.appSignature}"))
                    }

                    // 3. Handle based on the type
                    when (envelope.type) {
                        IyoxEnvelope.TYPE_BACKUP -> {
                            val itemType = object : TypeToken<List<ExportVouch>>() {}.type
                            val vouches: List<ExportVouch> = gson.fromJson(envelope.data, itemType)
                                ?: emptyList()

                            database.withTransaction {
                                database.vouchDataDao().deleteAll()
                                database.personDao().deleteAll()

                                val allNames = vouches.flatMap { it.persons }
                                    .map { it.name.trim() }
                                    .filter { it.isNotBlank() }
                                    .distinct()

                                if (allNames.isNotEmpty()) {
                                    database.personDao().insertAll(allNames.map { Person(name = it) })
                                }
                                val personMap = database.personDao().getByNames(allNames).associateBy { it.name }

                                val allCrossRefs = mutableListOf<VouchPersonCrossRef>()
                                vouches.forEach { export ->
                                    val newVouchId = database.vouchDao()
                                        .insert(export.vouch.copy(id = 0L))

                                    val crossRefs = export.persons.mapNotNull { personAmount ->
                                        val normalizedName = personAmount.name.trim()
                                        val storedPerson = personMap[normalizedName] ?: return@mapNotNull null
                                        VouchPersonCrossRef(
                                            vouchId = newVouchId,
                                            personId = storedPerson.id,
                                            amount = personAmount.amount,
                                            settled = personAmount.settled
                                        )
                                    }
                                    allCrossRefs.addAll(crossRefs)
                                }

                                if (allCrossRefs.isNotEmpty()) {
                                    database.vouchDao().insertCrossRefs(allCrossRefs)
                                }
                            }

                            vouchRepository.notifyDataChanged()
                            Result.success(BackupImportResult.BackupSuccess(vouches.size))
                        }
                        IyoxEnvelope.TYPE_SINGLE -> {
                            val singleVouch = gson.fromJson(envelope.data, ExportVouch::class.java)
                                ?: return@withContext Result.failure(IllegalArgumentException("Invalid or empty single vouch payload"))

                            database.withTransaction {
                                val allNames = singleVouch.persons
                                    .map { it.name.trim() }
                                    .filter { it.isNotBlank() }
                                    .distinct()

                                if (allNames.isNotEmpty()) {
                                    database.personDao().insertAll(allNames.map { Person(name = it) })
                                }
                                val personMap = database.personDao().getByNames(allNames).associateBy { it.name }

                                val newVouchId = database.vouchDao()
                                    .insert(singleVouch.vouch.copy(id = 0L))

                                val crossRefs = singleVouch.persons.mapNotNull { personAmount ->
                                    val normalizedName = personAmount.name.trim()
                                    val storedPerson = personMap[normalizedName] ?: return@mapNotNull null
                                    VouchPersonCrossRef(
                                        vouchId = newVouchId,
                                        personId = storedPerson.id,
                                        amount = personAmount.amount,
                                        settled = personAmount.settled
                                    )
                                }

                                if (crossRefs.isNotEmpty()) {
                                    database.vouchDao().insertCrossRefs(crossRefs)
                                }
                            }

                            vouchRepository.notifyDataChanged()
                            Result.success(BackupImportResult.SingleSuccess(singleVouch))
                        }
                        else -> {
                            Result.failure(UnsupportedBackupTypeException(envelope.type))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                database.vouchDataDao().deleteAll()
                database.personDao().deleteAll()
            }
            vouchRepository.notifyDataChanged()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
