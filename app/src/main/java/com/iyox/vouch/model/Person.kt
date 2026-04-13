package com.iyox.vouch.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import java.math.BigDecimal


@Entity
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, val name: String
)

data class PersonWithTotal(
    @Embedded val person: Person, val totalAmount: BigDecimal?, val vouchCount: Int
)

@Dao
interface PersonDao {
    @Query("SELECT * FROM Person WHERE name LIKE '%' || :query || '%'")
    suspend fun findPersonsByName(query: String): List<Person>

    @Query("SELECT * FROM Person")
    suspend fun getAllPersons(): List<Person>

    @Query("SELECT Person.*, COALESCE(SUM(CASE WHEN Vouch.given = 0 THEN -Vouch.amount ELSE Vouch.amount END), 0) AS totalAmount, COUNT(Vouch.id) AS vouchCount FROM Person LEFT JOIN Vouch ON Person.name = Vouch.personName AND Vouch.settled = 0 GROUP BY Person.id")
    suspend fun getPersonsWithTotalAll(): List<PersonWithTotal>

    @Query("SELECT Person.*, COALESCE(SUM(CASE WHEN Vouch.given = 0 THEN -Vouch.amount ELSE Vouch.amount END), 0) AS totalAmount, COUNT(Vouch.id) AS vouchCount FROM Person LEFT JOIN Vouch ON Person.name = Vouch.personName AND Vouch.settled = 0 WHERE Person.name LIKE '%' || :query || '%' GROUP BY Person.id")
    suspend fun getPersonsWithTotalFiltered(query: String): List<PersonWithTotal>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(person: Person): Long

    @Delete
    suspend fun delete(person: Person)

    @Query("DELETE FROM Person WHERE name = :personName")
    suspend fun deleteByName(personName: String)

    @Query("SELECT COUNT(*) FROM Vouch WHERE personName = :personName")
    suspend fun getVouchCount(personName: String): Int
}