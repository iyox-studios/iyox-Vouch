package com.iyox.vouch.core.backup

import android.net.Uri
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.iyox.vouch.core.model.Vouch

data class IyoxEnvelope(
    @SerializedName("_app_signature")
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
    }
}

data class ExportPersonAmount(
    @SerializedName("name")
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
    val persons: List<ExportPersonAmount>
)

data class BackupPreview(
    val uri: Uri,
    val type: String,
    val version: String,
    val vouchCount: Int,
    val personCount: Int
)
