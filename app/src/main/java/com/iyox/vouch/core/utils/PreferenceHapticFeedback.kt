package com.iyox.vouch.core.utils

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

class PreferenceHapticFeedback(
    private val delegate: HapticFeedback,
    private val isEnabled: () -> Boolean
) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (isEnabled()) {
            delegate.performHapticFeedback(hapticFeedbackType)
        }
    }
}
