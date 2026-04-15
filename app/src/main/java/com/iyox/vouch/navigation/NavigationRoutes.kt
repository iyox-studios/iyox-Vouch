package com.iyox.vouch.navigation

import android.net.Uri.encode

sealed class Screen(val route: String, val title: String) {
    object Vouches : Screen("vouches", "Vouches")
    object Persons : Screen("persons", "People")
    object Settings : Screen("settings", "Settings")
    object AddVouch : Screen("add_vouch", "Add Vouch")
    object ViewPerson : Screen("view_person/{personName}", "View Person") {
        fun createRoute(personName: String) = "view_person/${encode(personName)}"
    }
    object EditVouch : Screen("edit_vouch/{vouchId}", "Edit Vouch") {
        fun createRoute(vouchId: Int) = "edit_vouch/$vouchId"
    }
}
