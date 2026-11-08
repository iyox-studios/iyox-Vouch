package com.iyox.vouch.navigation

/** Stable launcher actions, independent of serialized navigation routes. */
enum class VouchShortcut(val action: String) {
    ADD_VOUCH("com.iyox.vouch.action.ADD_VOUCH"),
    ADD_GROUP_VOUCH("com.iyox.vouch.action.ADD_GROUP_VOUCH");

    companion object {
        fun fromAction(action: String?): VouchShortcut? = entries.firstOrNull { it.action == action }
    }
}
