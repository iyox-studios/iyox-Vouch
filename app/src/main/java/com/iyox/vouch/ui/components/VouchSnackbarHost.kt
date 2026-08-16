package com.iyox.vouch.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.iyox.vouch.core.utils.ToastType
import com.iyox.vouch.core.utils.VouchSnackbarVisuals

@Composable
fun VouchSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        val visuals = data.visuals as? VouchSnackbarVisuals
        val isError = visuals?.type == ToastType.ERROR

        if (isError) {
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                actionColor = MaterialTheme.colorScheme.error,
                dismissActionContentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        } else {
            Snackbar(
                snackbarData = data
            )
        }
    }
}
