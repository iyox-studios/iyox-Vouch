package com.iyox.vouch.core.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity
data class Vouch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Long,
    val description: String = "",
    val given: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vouch_person_cross_ref",
    primaryKeys = ["vouchId", "personId"],
    foreignKeys = [
        ForeignKey(
            entity = Vouch::class,
            parentColumns = ["id"],
            childColumns = ["vouchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["personId", "settled"]),
        Index(value = ["vouchId", "settled"]),
        Index("personId"),
        Index("vouchId")
    ]
)
data class VouchPersonCrossRef(
    val vouchId: Int,
    val personId: Int,
    val amount: Long,
    val settled: Boolean = false
)

data class VouchPersonRow(
    val vouchId: Int,
    @Embedded val person: Person,
    val amount: Long,
    val settled: Boolean
)

data class VouchWithPerson(
    @Embedded val vouch: Vouch,
    val persons: List<PersonWithAmount>
)

fun VouchWithPerson.isSettled(): Boolean =
    persons.isNotEmpty() && persons.all { it.settled }

fun VouchWithPerson.isSettledForPerson(personName: String): Boolean =
    persons.firstOrNull { it.person.name == personName }?.settled ?: false

data class VouchPersonSettlement(
    val personId: Int,
    val settled: Boolean
)
