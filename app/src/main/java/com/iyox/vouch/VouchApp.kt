package com.iyox.vouch

import android.app.UiModeManager
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.core.utils.LocalSnackbarHostState
import com.iyox.vouch.core.utils.LocalToastManager
import com.iyox.vouch.core.utils.ToastManager
import com.iyox.vouch.feature.settings.SettingsViewModel
import com.iyox.vouch.feature.settings.ToastEvent
import com.iyox.vouch.navigation.AppNavigation
import com.iyox.vouch.ui.components.ImportConfirmationDialog
import com.iyox.vouch.ui.components.VouchSnackbarHost
import com.iyox.vouch.ui.theme.IyoxVouchTheme

@Composable
fun VouchApp(
    pendingImportUri: Uri? = null,
    onImportUriConsumed: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val toastManager = remember(snackbarHostState, coroutineScope) {
        ToastManager(snackbarHostState, coroutineScope)
    }
    val settingsViewModel: SettingsViewModel = viewModel()
    val uiState by settingsViewModel.uiState.collectAsState()
    val pendingImportPreview by settingsViewModel.pendingImportPreview.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.toastEvents.collect { event ->
            when (event) {
                is ToastEvent.Info -> toastManager.showInfo(context.getString(event.messageRes))
                is ToastEvent.Error -> toastManager.showError(context.getString(event.messageRes))
            }
        }
    }

    LaunchedEffect(pendingImportUri) {
        if (pendingImportUri != null) {
            settingsViewModel.requestImport(pendingImportUri)
            onImportUriConsumed()
        }
    }

    val darkTheme = when (uiState.theme) {
        UiModeManager.MODE_NIGHT_YES -> true
        UiModeManager.MODE_NIGHT_NO -> false
        else -> isSystemInDarkTheme()
    }

    IyoxVouchTheme(
        darkTheme = darkTheme,
        dynamicColor = uiState.dynamicColors
    ) {
        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            LocalToastManager provides toastManager
        ) {
            AppNavigation({ VouchSnackbarHost(snackbarHostState) })

            pendingImportPreview?.let { preview ->
                ImportConfirmationDialog(
                    preview = preview,
                    onConfirm = {
                        settingsViewModel.confirmImport(preview)
                    },
                    onDismiss = {
                        settingsViewModel.dismissImportDialog()
                    }
                )
            }
        }
    }
}
