package com.iyox.vouch.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.sql.Timestamp

class TimestampConverterTest {

    private val converter = TimestampConverter()

    @Test
    fun testFromTimestamp_null() {
        assertNull(converter.fromTimestamp(null))
    }

    @Test
    fun testFromTimestamp_valid() {
        val millis = 1700000000000L
        val timestamp = converter.fromTimestamp(millis)
        assertEquals(Timestamp(millis), timestamp)
        assertEquals(millis, timestamp?.time)
    }

    @Test
    fun testFromTimestamp_zero() {
        val timestamp = converter.fromTimestamp(0L)
        assertEquals(Timestamp(0L), timestamp)
    }

    @Test
    fun testTimestampToLong_null() {
        assertNull(converter.timestampToLong(null))
    }

    @Test
    fun testTimestampToLong_valid() {
        val millis = 1700000000000L
        val timestamp = Timestamp(millis)
        assertEquals(millis, converter.timestampToLong(timestamp))
    }

    @Test
    fun testRoundtripConversion() {
        val originalMillis = 1712345678901L
        val timestamp = converter.fromTimestamp(originalMillis)
        val resultMillis = converter.timestampToLong(timestamp)
        assertEquals(originalMillis, resultMillis)
    }
}
