package com.iyox.vouch.view

import androidx.compose.runtime.Composable
import com.iyox.vouch.navigation.AppNavigation
import com.iyox.vouch.ui.theme.IyoxVouchTheme

@Composable
fun MainView() {
    IyoxVouchTheme { AppNavigation() }
}

