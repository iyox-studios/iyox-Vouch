package com.iyox.vouch.navigation

/** Stable launcher actions, independent of serialized navigation routes. */
enum class VouchShortcut(val action: String) {
    ADD_VOUCH("com.iyox.vouch.action.ADD_VOUCH"),
    ADD_GROUP_VOUCH("com.iyox.vouch.action.ADD_GROUP_VOUCH"),
    VIEW_PERSON("com.iyox.vouch.action.VIEW_PERSON"),
    SHARE_PERSON("com.iyox.vouch.action.SHARE_PERSON"),
    SETTLE_PERSON("com.iyox.vouch.action.SETTLE_PERSON");

    companion object {
        const val EXTRA_PERSON_NAME = "com.iyox.vouch.extra.PERSON_NAME"
        fun fromAction(action: String?): VouchShortcut? = entries.firstOrNull { it.action == action }
    }
}
