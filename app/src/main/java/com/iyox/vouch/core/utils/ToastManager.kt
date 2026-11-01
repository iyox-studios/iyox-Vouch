package com.iyox.vouch.core.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.iyox.vouch.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class ToastType {
    INFO,
    ERROR
}

data class VouchSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val type: ToastType = ToastType.INFO
) : SnackbarVisuals

class ToastManager(
    val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope,
    private val context: Context? = null,
    private val haptic: HapticFeedback? = null
) {
    fun show(
        message: String,
        type: ToastType = ToastType.INFO,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                VouchSnackbarVisuals(
                    message = message,
                    duration = duration,
                    type = type
                )
            )
        }
    }

    fun showInfo(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onAction: (() -> Unit)? = null
    ) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val visuals = VouchSnackbarVisuals(
                message = message,
                actionLabel = actionLabel,
                duration = duration,
                type = ToastType.INFO
            )
            val result = snackbarHostState.showSnackbar(visuals)
            if (result == SnackbarResult.ActionPerformed) {
                onAction?.invoke()
            }
        }
    }

    fun showInfo(
        @StringRes messageRes: Int,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onAction: (() -> Unit)? = null
    ) {
        val message = context?.getString(messageRes) ?: return
        showInfo(message, actionLabel, duration, onAction)
    }

    fun showError(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        haptic?.performHapticFeedback(HapticFeedbackType.Reject)
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                VouchSnackbarVisuals(
                    message = message,
                    duration = duration,
                    type = ToastType.ERROR
                )
            )
        }
    }

    fun showError(
        @StringRes messageRes: Int,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        val message = context?.getString(messageRes) ?: return
        showError(message, duration)
    }

    fun <T> executeWithUndo(
        message: String,
        actionLabel: String? = null,
        errorMessage: String? = null,
        performAction: suspend () -> T,
        undoAction: suspend (T) -> Unit
    ) {
        val resolvedActionLabel = when {
            actionLabel != null -> actionLabel
            context != null -> context.getString(R.string.action_undo)
            else -> "Undo"
        }
        val defaultError = when {
            errorMessage != null -> errorMessage
            context != null -> context.getString(R.string.msg_operation_failed)
            else -> "Operation failed"
        }
        coroutineScope.launch {
            try {
                val resultData = performAction() ?: return@launch
                snackbarHostState.currentSnackbarData?.dismiss()
                val visuals = VouchSnackbarVisuals(
                    message = message,
                    actionLabel = resolvedActionLabel,
                    duration = SnackbarDuration.Short,
                    type = ToastType.INFO
                )
                val snackbarResult = snackbarHostState.showSnackbar(visuals)
                if (snackbarResult == SnackbarResult.ActionPerformed) {
                    haptic?.performHapticFeedback(HapticFeedbackType.Confirm)
                    try {
                        undoAction(resultData)
                    } catch (e: Exception) {
                        showError(defaultError)
                    }
                }
            } catch (e: Exception) {
                showError(defaultError)
            }
        }
    }
}

@Composable
fun rememberToastManager(): ToastManager {
    return LocalToastManager.current
}
