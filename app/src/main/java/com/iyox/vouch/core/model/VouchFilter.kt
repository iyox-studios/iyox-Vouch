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
    AMOUNT,
    PERSON;

    fun next(supportsPerson: Boolean = true): VouchSortOrder = when (this) {
        DATE -> AMOUNT
        AMOUNT -> if (supportsPerson) PERSON else DATE
        PERSON -> DATE
    }
}
