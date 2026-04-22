package com.iyox.vouch.core.utils

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UndoCoordinator(
    val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope
) {
    fun <T> executeWithUndo(
        message: String,
        actionLabel: String = "Undo",
        performAction: suspend () -> T,
        undoAction: suspend (T) -> Unit
    ) {
        coroutineScope.launch {
            val resultData = performAction()
            val snackbarResult = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short
            )
            if (snackbarResult == SnackbarResult.ActionPerformed) {
                undoAction(resultData)
            }
        }
    }
}

@Composable
fun rememberUndoCoordinator(): UndoCoordinator {
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    return remember(snackbarHostState, coroutineScope) {
        UndoCoordinator(snackbarHostState, coroutineScope)
    }
}

