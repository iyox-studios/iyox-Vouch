package com.iyox.vouch.model

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query


@Entity
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firstName: String,
    val secondName: String?
)

@Dao
interface PersonDao {
    @Query("SELECT * FROM Person WHERE firstName LIKE '%' || :query || '%' OR secondName LIKE '%' || :query || '%'")
    suspend fun searchPersons(query: String): List<Person>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(person: Person): Long

    @Query("SELECT * FROM Person WHERE firstName = :firstName AND secondName = :secondName LIMIT 1")
    suspend fun findByName(firstName: String, secondName: String): Person?
}