package com.iyox.vouch.navigation

sealed class Screen(val route: String, val title: String) {
    object Vouches : Screen("vouches", "Vouches")
    object Persons : Screen("persons", "People")
    object Settings : Screen("settings", "Settings")
    object AddVouch : Screen("add_vouch", "Add Vouch")
    object EditVouch : Screen("edit_vouch/{vouchId}", "Edit Vouch") {
        fun createRoute(vouchId: Int) = "edit_vouch/$vouchId"
    }
}
