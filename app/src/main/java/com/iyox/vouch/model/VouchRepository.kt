package com.iyox.vouch.model
import kotlinx.coroutines.delay

class VouchRepository {
    suspend fun fetchUserData(): VouchData {
        delay(2000) // simulate API delay
        return VouchData(id = 1, amount = 100, description = "Sample Vouch Data")
    }
}