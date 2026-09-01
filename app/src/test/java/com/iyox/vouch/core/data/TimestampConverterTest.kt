package com.iyox.vouch.core.data

import com.iyox.vouch.core.model.Vouch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VouchTimestampTest {

    @Test
    fun testDefaultTimestamp_isRecent() {
        val before = System.currentTimeMillis()
        val vouch = Vouch(amount = 1000L)
        val after = System.currentTimeMillis()

        assertTrue(vouch.timestamp in before..after)
    }

    @Test
    fun testExplicitTimestamp() {
        val explicitTime = 1700000000000L
        val vouch = Vouch(amount = 1000L, timestamp = explicitTime)

        assertEquals(explicitTime, vouch.timestamp)
    }
}
