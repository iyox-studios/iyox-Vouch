package com.iyox.vouch.core.model

enum class VouchTypeFilter {
    ALL,
    GIVEN,
    RECEIVED;

    fun next(): VouchTypeFilter = when (this) {
        ALL -> GIVEN
        GIVEN -> RECEIVED
        RECEIVED -> ALL
    }
}

enum class VouchSortOrder {
    DATE,
    AMOUNT;

    fun next(): VouchSortOrder = when (this) {
        DATE -> AMOUNT
        AMOUNT -> DATE
    }
}
