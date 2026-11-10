package com.iyox.vouch.navigation

import android.content.Intent
import android.os.Bundle

/** Keeps a shortcut's destination and person together across activity recreation. */
data class ShortcutRequest(val shortcut: VouchShortcut, val personName: String? = null) {
    fun saveTo(state: Bundle) {
        state.putString(KEY_SHORTCUT, shortcut.action)
        state.putString(VouchShortcut.EXTRA_PERSON_NAME, personName)
    }

    companion object {
        private const val KEY_SHORTCUT = "pending_shortcut"

        fun fromIntent(intent: Intent?): ShortcutRequest? =
            VouchShortcut.fromAction(intent?.action)?.let {
                ShortcutRequest(it, intent?.getStringExtra(VouchShortcut.EXTRA_PERSON_NAME))
            }

        fun restoreFrom(state: Bundle): ShortcutRequest? =
            VouchShortcut.fromAction(state.getString(KEY_SHORTCUT))?.let {
                ShortcutRequest(it, state.getString(VouchShortcut.EXTRA_PERSON_NAME))
            }
    }
}
