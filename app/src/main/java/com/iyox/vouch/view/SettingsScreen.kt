package com.iyox.vouch.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val feedbackTypes = listOf(
            HapticFeedbackType.ContextClick,
            HapticFeedbackType.LongPress,
            HapticFeedbackType.Reject,
            HapticFeedbackType.Confirm,
            HapticFeedbackType.GestureEnd,
            HapticFeedbackType.GestureThresholdActivate,
            HapticFeedbackType.KeyboardTap,
            HapticFeedbackType.ContextClick,
            HapticFeedbackType.SegmentFrequentTick,
            HapticFeedbackType.TextHandleMove,
            HapticFeedbackType.ToggleOff,
            HapticFeedbackType.ToggleOn,
            HapticFeedbackType.SegmentTick,
            HapticFeedbackType.VirtualKey,
        )

        feedbackTypes.forEachIndexed { _, type ->
            Button(
                onClick = { haptics.performHapticFeedback(type) },
            ) { Text(type.toString()) }
        }
    }
}

