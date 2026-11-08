package com.iyox.vouch.core.utils

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceHapticFeedbackTest {
    @Test
    fun feedbackFollowsPreferenceChangesForExistingConsumers() {
        var enabled = true
        val received = mutableListOf<HapticFeedbackType>()
        val delegate = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                received += hapticFeedbackType
            }
        }
        val haptic = PreferenceHapticFeedback(delegate) { enabled }

        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        enabled = false
        haptic.performHapticFeedback(HapticFeedbackType.Reject)
        haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
        enabled = true
        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)

        assertEquals(
            listOf(HapticFeedbackType.Confirm, HapticFeedbackType.ToggleOn),
            received
        )
    }
}
