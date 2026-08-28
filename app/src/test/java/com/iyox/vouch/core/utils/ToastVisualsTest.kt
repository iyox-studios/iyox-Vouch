package com.iyox.vouch.core.utils

import androidx.compose.material3.SnackbarDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ToastVisualsTest {

    @Test
    fun testToastTypeEnum() {
        assertEquals(2, ToastType.entries.size)
        assertEquals(ToastType.INFO, ToastType.valueOf("INFO"))
        assertEquals(ToastType.ERROR, ToastType.valueOf("ERROR"))
    }

    @Test
    fun testVouchSnackbarVisuals_defaults() {
        val visuals = VouchSnackbarVisuals(message = "Operation done")
        assertEquals("Operation done", visuals.message)
        assertNull(visuals.actionLabel)
        assertFalse(visuals.withDismissAction)
        assertEquals(SnackbarDuration.Short, visuals.duration)
        assertEquals(ToastType.INFO, visuals.type)
    }

    @Test
    fun testVouchSnackbarVisuals_custom() {
        val visuals = VouchSnackbarVisuals(
            message = "Failed to save",
            actionLabel = "Retry",
            withDismissAction = true,
            duration = SnackbarDuration.Long,
            type = ToastType.ERROR
        )
        assertEquals("Failed to save", visuals.message)
        assertEquals("Retry", visuals.actionLabel)
        assertEquals(true, visuals.withDismissAction)
        assertEquals(SnackbarDuration.Long, visuals.duration)
        assertEquals(ToastType.ERROR, visuals.type)
    }
}
