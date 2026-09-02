package com.iyox.vouch.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationRoutesTest {

    @Test
    fun testSerializableRoutes() {
        assertNotNull(OnboardingRoute)
        assertNotNull(VouchesRoute)
        assertNotNull(PersonsRoute)
        assertNotNull(SettingsRoute)

        val settled = SettledSpaceRoute(personName = "Alice")
        assertEquals("Alice", settled.personName)
        val settledDefault = SettledSpaceRoute()
        assertNull(settledDefault.personName)

        val single = SingleVouchRoute(vouchId = 42L, personName = "Bob")
        assertEquals(42L, single.vouchId)
        assertEquals("Bob", single.personName)
        val singleDefault = SingleVouchRoute()
        assertNull(singleDefault.vouchId)
        assertNull(singleDefault.personName)

        val group = GroupVouchRoute(vouchId = 100L)
        assertEquals(100L, group.vouchId)
        val groupDefault = GroupVouchRoute()
        assertNull(groupDefault.vouchId)

        val person = ViewPersonRoute(personName = "Charlie")
        assertEquals("Charlie", person.personName)
    }
}
