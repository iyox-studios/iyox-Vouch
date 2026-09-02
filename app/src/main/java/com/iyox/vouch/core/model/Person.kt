package com.iyox.vouch.core.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["name"], unique = true)])
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L, val name: String
)

data class PersonWithAmount(
    @Embedded val person: Person,
    val amount: Long,
    val settled: Boolean = false
)

data class PersonTotal(
    @Embedded val person: Person,
    val amount: Long,
    val vouchCount: Int
)