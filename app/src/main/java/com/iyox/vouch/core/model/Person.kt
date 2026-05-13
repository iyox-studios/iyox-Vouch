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
    val amount: BigDecimal,
    val settled: Boolean = false
)

data class PersonTotal(
    @Embedded val person: Person,
    val amount: BigDecimal,
    val vouchCount: Int
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

    @Query(
        """
        SELECT p.*,
               COALESCE(
                   SUM(
                       CASE
                           WHEN vpc.settled = 1 THEN 0
                           WHEN v.id IS NULL THEN 0
                           WHEN v.given = 0 THEN -vpc.amount
                           ELSE vpc.amount
                       END
                   ),
                   0
               ) AS amount,
               COUNT(DISTINCT CASE WHEN vpc.settled = 0 THEN vpc.vouchId END) AS vouchCount
        FROM Person p
        LEFT JOIN vouch_person_cross_ref vpc ON p.id = vpc.personId
        LEFT JOIN Vouch v ON vpc.vouchId = v.id
        GROUP BY p.id
        """
    )
    suspend fun getPersonsTotalAll(): List<PersonTotal>

    @Query(
        """
        SELECT p.*,
               COALESCE(
                   SUM(
                       CASE
                           WHEN vpc.settled = 1 THEN 0
                           WHEN v.id IS NULL THEN 0
                           WHEN v.given = 0 THEN -vpc.amount
                           ELSE vpc.amount
                       END
                   ),
                   0
               ) AS amount,
               COUNT(DISTINCT CASE WHEN vpc.settled = 0 THEN vpc.vouchId END) AS vouchCount
        FROM Person p
        LEFT JOIN vouch_person_cross_ref vpc ON p.id = vpc.personId
        LEFT JOIN Vouch v ON vpc.vouchId = v.id
        WHERE p.name LIKE '%' || :query || '%'
        GROUP BY p.id
        """
    )
    suspend fun getPersonsTotalFiltered(query: String): List<PersonTotal>
}