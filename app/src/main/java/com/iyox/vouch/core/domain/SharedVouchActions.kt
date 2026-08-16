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

    suspend fun deletePersonFromVouch(vouchId: Int, personId: Int) {
        repository.deletePersonFromVouch(vouchId, personId)
        onDataChanged()
    }

    suspend fun restoreVouch(vouchWithPerson: VouchWithPerson) {
        repository.restoreVouch(vouchWithPerson)
        onDataChanged()
    }

    // TODO: make this one method and use id instead of vouch and also use this everywhere instead of update
    suspend fun settleVouch(vouch: VouchWithPerson) {
        repository.setVouchSettled(vouch.vouch.id, true)
        onDataChanged()
    }

    suspend fun unSettleVouch(vouch: VouchWithPerson) {
        repository.setVouchSettled(vouch.vouch.id, false)
        onDataChanged()
    }

    suspend fun setVouchPersonSettlement(vouchId: Int, personId: Int, settled: Boolean) {
        repository.setPersonSettlement(vouchId, personId, settled)
        onDataChanged()
    }
}
