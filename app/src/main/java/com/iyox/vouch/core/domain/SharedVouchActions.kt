package com.iyox.vouch.core.domain

import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.VouchWithPerson
import javax.inject.Inject

class SharedVouchActions @Inject constructor(
    private val repository: VouchRepository
) {
    suspend fun addVouch(vouchWithPerson: VouchWithPerson) {
        repository.insert(vouchWithPerson)
    }

    suspend fun deleteVouch(vouchId: Long) {
        repository.delete(vouchId)
    }

    suspend fun deletePersonFromVouch(vouchId: Long, personId: Long) {
        repository.deletePersonFromVouch(vouchId, personId)
    }

    suspend fun restoreVouch(vouchWithPerson: VouchWithPerson) {
        repository.restoreVouch(vouchWithPerson)
    }

    suspend fun settleVouch(vouch: VouchWithPerson) {
        repository.setVouchSettled(vouch.vouch.id, true)
    }

    suspend fun unSettleVouch(vouch: VouchWithPerson) {
        repository.setVouchSettled(vouch.vouch.id, false)
    }

    suspend fun setVouchPersonSettlement(vouchId: Long, personId: Long, settled: Boolean) {
        repository.setPersonSettlement(vouchId, personId, settled)
    }
}
