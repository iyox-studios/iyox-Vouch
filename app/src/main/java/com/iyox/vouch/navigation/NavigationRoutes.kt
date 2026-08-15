package com.iyox.vouch.navigation

import android.net.Uri.encode
import androidx.annotation.StringRes
import com.iyox.vouch.R

sealed class Screen(val route: String, @StringRes val titleRes: Int) {
    object Vouches : Screen("vouches", R.string.screen_vouches)
    object Persons : Screen("persons", R.string.screen_people)
    object Settings : Screen("settings", R.string.screen_settings)
    object AddVouch : Screen("add_vouch?personName={personName}", R.string.title_add_vouch) {
        const val personNameArg = "personName"

        fun createRoute(personName: String? = null): String {
            return if (personName.isNullOrBlank()) {
                "add_vouch"
            } else {
                "add_vouch?personName=${encode(personName)}"
            }
        }
    }
    object AddGroupVouch : Screen("add_group_vouch", R.string.title_add_group_vouch)
    object ViewPerson : Screen("view_person/{personName}", R.string.title_view_person) {
        fun createRoute(personName: String) = "view_person/${encode(personName)}"
    }

    object EditVouch : Screen("edit_vouch/{vouchId}", R.string.title_edit_vouch) {
        fun createRoute(vouchId: Int) = "edit_vouch/$vouchId"
    }

    object EditGroupVouch : Screen("edit_group_vouch/{vouchId}", R.string.title_edit_vouch) {
        fun createRoute(vouchId: Int) = "edit_group_vouch/$vouchId"
    }
}
