package com.iyox.vouch.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VouchModelExtensionsTest {

    @Test
    fun testIsSettled_emptyPersons() {
        val vouch = VouchWithPerson(
            vouch = Vouch(id = 1, amount = 1000L),
            persons = emptyList()
        )
        assertFalse(vouch.isSettled())
    }

    @Test
    fun testIsSettled_allSettled() {
        val vouch = VouchWithPerson(
            vouch = Vouch(id = 1, amount = 2000L),
            persons = listOf(
                PersonWithAmount(person = Person(id = 1, name = "Alice"), amount = 1000L, settled = true),
                PersonWithAmount(person = Person(id = 2, name = "Bob"), amount = 1000L, settled = true)
            )
        )
        assertTrue(vouch.isSettled())
    }

    @Test
    fun testIsSettled_someUnsettled() {
        val vouch = VouchWithPerson(
            vouch = Vouch(id = 1, amount = 2000L),
            persons = listOf(
                PersonWithAmount(person = Person(id = 1, name = "Alice"), amount = 1000L, settled = true),
                PersonWithAmount(person = Person(id = 2, name = "Bob"), amount = 1000L, settled = false)
            )
        )
        assertFalse(vouch.isSettled())
    }

    @Test
    fun testIsSettled_noneSettled() {
        val vouch = VouchWithPerson(
            vouch = Vouch(id = 1, amount = 1000L),
            persons = listOf(
                PersonWithAmount(person = Person(id = 1, name = "Alice"), amount = 1000L, settled = false)
            )
        )
        assertFalse(vouch.isSettled())
    }

    @Test
    fun testIsSettledForPerson_matchingAndSettled() {
        val vouch = VouchWithPerson(
            vouch = Vouch(id = 1, amount = 2000L),
            persons = listOf(
                PersonWithAmount(person = Person(id = 1, name = "Alice"), amount = 1000L, settled = true),
                PersonWithAmount(person = Person(id = 2, name = "Bob"), amount = 1000L, settled = false)
            )
        )
        assertTrue(vouch.isSettledForPerson("Alice"))
        assertFalse(vouch.isSettledForPerson("Bob"))
    }

    @Test
    fun testIsSettledForPerson_matchingAndUnsettled() {
        val vouch = VouchWithPerson(
            vouch = Vouch(id = 1, amount = 1000L),
            persons = listOf(
                PersonWithAmount(person = Person(id = 1, name = "Alice"), amount = 1000L, settled = false)
            )
        )
        assertFalse(vouch.isSettledForPerson("Alice"))
    }

    @Test
    fun testIsSettledForPerson_personNotFound() {
        val vouch = VouchWithPerson(
            vouch = Vouch(id = 1, amount = 1000L),
            persons = listOf(
                PersonWithAmount(person = Person(id = 1, name = "Alice"), amount = 1000L, settled = true)
            )
        )
        assertFalse(vouch.isSettledForPerson("Charlie"))
        assertFalse(vouch.isSettledForPerson(""))
    }
}
