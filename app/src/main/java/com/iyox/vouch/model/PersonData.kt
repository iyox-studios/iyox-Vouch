package com.iyox.vouch.model

import java.math.BigDecimal

data class PersonData(
    val name: String,
    val totalAmount: BigDecimal //TODO: different currencies (generally everything here)
    //val correspondingVouches: List<VouchData>
)