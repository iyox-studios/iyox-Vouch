package com.iyox.vouch.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(person: Person): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(persons: List<Person>): List<Long>

    @Update
    suspend fun update(person: Person)

    @Query("UPDATE Person SET name = :newName WHERE id = :personId")
    suspend fun updateName(personId: Int, newName: String)

    @Query("SELECT * FROM Person WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Person?

    @Query("SELECT * FROM Person WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Person?

    @Query("SELECT * FROM Person WHERE name IN (:names)")
    suspend fun getByNames(names: List<String>): List<Person>

    @Query("DELETE FROM Person")
    suspend fun deleteAll()

    @Query(
        """
        DELETE FROM Person
        WHERE NOT EXISTS (
            SELECT 1 FROM vouch_person_cross_ref WHERE personId = Person.id
        )
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
        WHERE p.name = :name
        GROUP BY p.id
        LIMIT 1
        """
    )
    suspend fun getPersonTotalByName(name: String): PersonTotal?

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
    fun getPersonsTotalAllFlow(): Flow<List<PersonTotal>>

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
               COUNT(DISTINCT vpc.vouchId) AS vouchCount
        FROM Person p
        LEFT JOIN vouch_person_cross_ref vpc ON p.id = vpc.personId
        LEFT JOIN Vouch v ON vpc.vouchId = v.id
        GROUP BY p.id
        ORDER BY vouchCount DESC, p.name ASC
        """
    )
    suspend fun getPersonsByTotalVouches(): List<PersonTotal>
}
