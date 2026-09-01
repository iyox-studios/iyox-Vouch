package com.iyox.vouch.core.domain

import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.VouchWithPerson

class SharedVouchActions(
    private val repository: VouchRepository,
    private val onDataChanged: suspend () -> Unit = {}
) {
    suspend fun addVouch(vouchWithPerson: VouchWithPerson) {
        repository.insert(vouchWithPerson)
        onDataChanged()
    }

    suspend fun deleteVouch(vouchId: Long) {
        repository.delete(vouchId)
        onDataChanged()
    }

    suspend fun deletePersonFromVouch(vouchId: Long, personId: Long) {
        repository.deletePersonFromVouch(vouchId, personId)
        onDataChanged()
    }

    suspend fun restoreVouch(vouchWithPerson: VouchWithPerson) {
        repository.restoreVouch(vouchWithPerson)
        onDataChanged()
    }

    suspend fun settleVouch(vouch: VouchWithPerson) {
        repository.setVouchSettled(vouch.vouch.id, true)
        onDataChanged()
    }

    suspend fun unSettleVouch(vouch: VouchWithPerson) {
        repository.setVouchSettled(vouch.vouch.id, false)
        onDataChanged()
    }

    suspend fun setVouchPersonSettlement(vouchId: Long, personId: Long, settled: Boolean) {
        repository.setPersonSettlement(vouchId, personId, settled)
        onDataChanged()
    }
}
