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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import com.iyox.vouch.core.utils.LocalSnackbarHostState
import com.iyox.vouch.core.utils.LocalToastManager
import com.iyox.vouch.core.utils.PreferenceHapticFeedback
import com.iyox.vouch.core.utils.ToastManager
import com.iyox.vouch.feature.settings.SettingsViewModel
import com.iyox.vouch.feature.settings.ToastEvent
import com.iyox.vouch.navigation.AppNavigation
import com.iyox.vouch.navigation.ShortcutRequest
import com.iyox.vouch.ui.components.ImportConfirmationDialog
import com.iyox.vouch.ui.components.VouchSnackbarHost
import com.iyox.vouch.ui.theme.IyoxVouchTheme

@Composable
fun VouchApp(
    pendingImportUri: Uri? = null,
    onImportUriConsumed: () -> Unit = {},
    pendingShortcut: ShortcutRequest? = null,
    onShortcutConsumed: () -> Unit = {}
) {
    val appContext = LocalContext.current.applicationContext
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val uiState by settingsViewModel.uiState.collectAsState()
    val platformHaptic = LocalHapticFeedback.current
    val hapticsEnabled = rememberUpdatedState(uiState.hapticsEnabled)
    val haptic = remember(platformHaptic) {
        PreferenceHapticFeedback(platformHaptic) { hapticsEnabled.value }
    }
    val toastManager = remember(snackbarHostState, coroutineScope, appContext, haptic) {
        ToastManager(snackbarHostState, coroutineScope, appContext, haptic)
    }
    val pendingImportPreview by settingsViewModel.pendingImportPreview.collectAsState()

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.toastEvents.collect { event ->
            when (event) {
                is ToastEvent.Info -> toastManager.showInfo(event.messageRes)
                is ToastEvent.Error -> toastManager.showError(event.messageRes)
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
        colorTheme = uiState.colorTheme
    ) {
        CompositionLocalProvider(
            LocalHapticFeedback provides haptic,
            LocalSnackbarHostState provides snackbarHostState,
            LocalToastManager provides toastManager
        ) {
            AppNavigation(
                snackbarHost = { VouchSnackbarHost(snackbarHostState) },
                settingsViewModel = settingsViewModel,
                pendingShortcut = pendingShortcut,
                onShortcutConsumed = onShortcutConsumed
            )

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
