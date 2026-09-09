package com.iyox.vouch.core.backup

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.iyox.vouch.core.model.Vouch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IyoxEnvelope(
    @SerializedName(value = "_app_signature", alternate = ["app_signature", "signature"])
    val appSignature: String = APP_SIGNATURE,
    @SerializedName("type")
    val type: String = TYPE_BACKUP,
    @SerializedName("version")
    val version: String = VERSION_1_0,
    @SerializedName("data")
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

data class ExportPersonAmount(
    @SerializedName(value = "name", alternate = ["personName", "person_name"])
    val name: String,
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("settled")
    val settled: Boolean = false
)

data class ExportVouch(
    @SerializedName("vouch")
    val vouch: Vouch,
    @SerializedName("persons")
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
