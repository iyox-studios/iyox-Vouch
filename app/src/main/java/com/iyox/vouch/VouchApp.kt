package com.iyox.vouch

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.iyox.vouch.core.utils.LocalSnackbarHostState
import com.iyox.vouch.navigation.AppNavigation
import com.iyox.vouch.ui.theme.IyoxVouchTheme

@Composable
fun VouchApp() {
    val snackbarHostState = remember { SnackbarHostState() }

    IyoxVouchTheme {
        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState
        ) {
            AppNavigation({ SnackbarHost(snackbarHostState) })
        }
    }
}
