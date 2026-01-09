package com.iyox.vouch.navigation

sealed class Screen(val route: String, val title: String) {
    object Vouches : Screen("vouches", "Vouches")
    object Settings : Screen("settings", "Settings")
    object AddVouch : Screen("add_vouch", "Add Vouch")
}
