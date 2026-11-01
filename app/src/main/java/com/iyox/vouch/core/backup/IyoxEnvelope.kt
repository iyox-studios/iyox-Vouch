package com.iyox.vouch.core.backup

import com.iyox.vouch.core.model.Vouch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class IyoxEnvelope(
    @SerialName("_app_signature")
    @JsonNames("app_signature", "signature")
    val appSignature: String = APP_SIGNATURE,
    @SerialName("type")
    val type: String = TYPE_BACKUP,
    @SerialName("version")
    val version: String = VERSION_1_0,
    @SerialName("data")
    val data: JsonElement
) {
    companion object {
        const val APP_SIGNATURE = "com.iyox.vouch"
        const val VERSION_1_0 = "1.0"
        const val TYPE_BACKUP = "backup"
        const val TYPE_SINGLE = "single"
        const val FILE_EXTENSION = "iyxv"
        const val MIME_TYPE = "application/x-iyxv"
        const val DEFAULT_BACKUP_FILE_NAME = "vouch_backup.iyxv"

        fun generateBackupFileName(date: Date = Date()): String {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(date)
            return "vouch_backup_$timestamp.$FILE_EXTENSION"
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExportPersonAmount(
    @SerialName("name")
    @JsonNames("personName", "person_name")
    val name: String,
    @SerialName("amount")
    val amount: Long,
    @SerialName("settled")
    val settled: Boolean = false,
    @SerialName("isCustom")
    val isCustom: Boolean = false,
    @SerialName("isSelf")
    val isSelf: Boolean = false
)

@Serializable
data class ExportVouch(
    @SerialName("vouch")
    val vouch: Vouch,
    @SerialName("persons")
    val persons: List<ExportPersonAmount> = emptyList()
)

data class BackupPreview(
    val uri: String,
    val type: String,
    val version: String,
    val vouchCount: Int,
    val personCount: Int,
    val cachedPayload: String? = null
)
