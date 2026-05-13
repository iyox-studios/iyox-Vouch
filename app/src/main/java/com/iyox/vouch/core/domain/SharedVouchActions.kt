package com.iyox.vouch.core.domain

import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.VouchWithPerson

class SharedVouchActions(
    private val repository: VouchRepository,
    private val onDataChanged: suspend () -> Unit
) {
    suspend fun addVouch(vouchWithPerson: VouchWithPerson) {
        repository.insert(vouchWithPerson)
        onDataChanged()
    }

    suspend fun deleteVouch(vouchID: Int) {
        repository.delete(vouchID)
        onDataChanged()
    }

    // TODO: make this one method and use id instead of vouch and also use this everywhere instead of update
    suspend fun settleVouch(vouch: VouchWithPerson) {
        repository.update(vouch.copy(vouch = vouch.vouch.copy(settled = true)))
        onDataChanged()
    }

    suspend fun unSettleVouch(vouch: VouchWithPerson) {
        repository.update(vouch.copy(vouch = vouch.vouch.copy(settled = false)))
        onDataChanged()
    }
}
