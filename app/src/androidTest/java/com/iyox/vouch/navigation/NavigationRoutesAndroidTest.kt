package com.iyox.vouch.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationRoutesAndroidTest {

    @Test
    fun testSettledSpaceRoute_withEncoding() {
        assertEquals("settled_space?personName=Alice", Screen.SettledSpace.createRoute("Alice"))
        assertEquals("settled_space?personName=Alice%20Smith", Screen.SettledSpace.createRoute("Alice Smith"))
        assertEquals("settled_space?personName=Alice%26Bob", Screen.SettledSpace.createRoute("Alice&Bob"))
    }

    @Test
    fun testAddVouchRoute_withEncoding() {
        assertEquals("add_vouch?personName=Bob", Screen.AddVouch.createRoute("Bob"))
        assertEquals("add_vouch?personName=Bob%20Jones", Screen.AddVouch.createRoute("Bob Jones"))
    }

    @Test
    fun testViewPersonRoute_withEncoding() {
        assertEquals("view_person/Alice", Screen.ViewPerson.createRoute("Alice"))
        assertEquals("view_person/Alice%20Smith", Screen.ViewPerson.createRoute("Alice Smith"))
        assertEquals("view_person/O'Connor", Screen.ViewPerson.createRoute("O'Connor"))
    }
}
