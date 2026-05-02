package com.iyox.vouch.core.domain

import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Vouch

class SharedVouchActions(
    private val repository: VouchRepository,
    private val onDataChanged: suspend () -> Unit
) {
    suspend fun addVouch(vouch: Vouch) {
        repository.insert(vouch)
        onDataChanged()
    }

    suspend fun deleteVouch(vouch: Vouch) {
        repository.delete(vouch)
        onDataChanged()
    }

    suspend fun settleVouch(vouch: Vouch) {
        repository.update(vouch.copy(settled = true))
        onDataChanged()
    }

    suspend fun unSettleVouch(vouch: Vouch) {
        repository.update(vouch.copy(settled = false))
        onDataChanged()
    }
}

