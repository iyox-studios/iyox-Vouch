package com.iyox.vouch.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Settings : Screen("settings", "Settings")
    object AddVouch : Screen("add_vouch", "Add Vouch")
}
