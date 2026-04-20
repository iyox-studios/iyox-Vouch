package com.iyox.vouch

import androidx.compose.runtime.Composable
import com.iyox.vouch.navigation.AppNavigation
import com.iyox.vouch.ui.theme.IyoxVouchTheme

@Composable
fun VouchApp() {
    IyoxVouchTheme { AppNavigation() }
}

