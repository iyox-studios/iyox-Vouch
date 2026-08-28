package com.iyox.vouch.navigation

import com.iyox.vouch.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRoutesTest {

    @Test
    fun testScreenRoutesAndTitles() {
        assertEquals("onboarding", Screen.Onboarding.route)
        assertEquals(R.string.screen_onboarding, Screen.Onboarding.titleRes)

        assertEquals("vouches", Screen.Vouches.route)
        assertEquals(R.string.screen_vouches, Screen.Vouches.titleRes)

        assertEquals("persons", Screen.Persons.route)
        assertEquals(R.string.screen_people, Screen.Persons.titleRes)

        assertEquals("settings", Screen.Settings.route)
        assertEquals(R.string.screen_settings, Screen.Settings.titleRes)

        assertEquals("add_group_vouch", Screen.AddGroupVouch.route)
        assertEquals(R.string.title_add_group_vouch, Screen.AddGroupVouch.titleRes)

        assertEquals("view_person/{personName}", Screen.ViewPerson.route)
        assertEquals(R.string.title_view_person, Screen.ViewPerson.titleRes)

        assertEquals("edit_vouch/{vouchId}", Screen.EditVouch.route)
        assertEquals(R.string.title_edit_vouch, Screen.EditVouch.titleRes)

        assertEquals("edit_group_vouch/{vouchId}", Screen.EditGroupVouch.route)
        assertEquals(R.string.title_edit_vouch, Screen.EditGroupVouch.titleRes)
    }

    @Test
    fun testEditVouchRoutes() {
        assertEquals("edit_vouch/42", Screen.EditVouch.createRoute(42))
        assertEquals("edit_vouch/0", Screen.EditVouch.createRoute(0))
        assertEquals("edit_group_vouch/100", Screen.EditGroupVouch.createRoute(100))
    }

    @Test
    fun testSettledSpaceRoutes_nullOrBlank() {
        assertEquals("settled_space", Screen.SettledSpace.createRoute(null))
        assertEquals("settled_space", Screen.SettledSpace.createRoute(""))
        assertEquals("settled_space", Screen.SettledSpace.createRoute("   "))
    }

    @Test
    fun testAddVouchRoutes_nullOrBlank() {
        assertEquals("add_vouch", Screen.AddVouch.createRoute(null))
        assertEquals("add_vouch", Screen.AddVouch.createRoute(""))
        assertEquals("add_vouch", Screen.AddVouch.createRoute("   "))
    }
}
