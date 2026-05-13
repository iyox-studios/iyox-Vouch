package com.iyox.vouch.core.model

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import java.math.BigDecimal

@Entity(indices = [Index(value = ["name"], unique = true)])
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, val name: String
)

data class PersonWithAmount(
    @Embedded val person: Person,
    val amount: BigDecimal
)

@Dao
interface PersonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(person: Person): Long

    @Query("SELECT * FROM Person WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Person?

    @Query("DELETE FROM Person")
    suspend fun deleteAll()

    @Query(
        """
        DELETE FROM Person
        WHERE id NOT IN (SELECT DISTINCT personId FROM vouch_person_cross_ref)
        """
    )
    suspend fun deletePersonsWithoutVouches()
}