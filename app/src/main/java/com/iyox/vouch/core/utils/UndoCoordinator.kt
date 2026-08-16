package com.iyox.vouch.core.utils

import androidx.compose.runtime.Composable

@Deprecated("Use ToastManager instead", ReplaceWith("ToastManager"))
typealias UndoCoordinator = ToastManager

@Deprecated("Use rememberToastManager() instead", ReplaceWith("rememberToastManager()"))
@Composable
fun rememberUndoCoordinator(): ToastManager = rememberToastManager()
