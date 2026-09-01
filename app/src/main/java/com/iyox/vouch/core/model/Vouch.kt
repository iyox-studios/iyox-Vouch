package com.iyox.vouch.core.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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

@Dao
interface VouchDao {
    @Query("SELECT * FROM Vouch")
    fun getAllFlow(): Flow<List<Vouch>>

    @Query("SELECT * FROM Vouch")
    suspend fun getAll(): List<Vouch>

    @Query(
        """
        SELECT vpc.vouchId AS vouchId, p.id AS id, p.name AS name, vpc.amount AS amount, vpc.settled AS settled
        FROM Person p
        INNER JOIN vouch_person_cross_ref vpc ON p.id = vpc.personId
        """
    )
    fun getAllPersonRowsFlow(): Flow<List<VouchPersonRow>>

    @Query(
        """
        SELECT vpc.vouchId AS vouchId, p.id AS id, p.name AS name, vpc.amount AS amount, vpc.settled AS settled
        FROM Person p
        INNER JOIN vouch_person_cross_ref vpc ON p.id = vpc.personId
        """
    )
    suspend fun getAllPersonRows(): List<VouchPersonRow>

    @Query(
        """
        SELECT v.*
        FROM Vouch v
        INNER JOIN vouch_person_cross_ref vpc ON v.id = vpc.vouchId
        INNER JOIN Person p ON vpc.personId = p.id
        WHERE p.name = :name
        ORDER BY v.timestamp DESC
        """
    )
    fun getVouchesByPersonNameFlow(name: String): Flow<List<Vouch>>

    @Query(
        """
        SELECT v.*
        FROM Vouch v
        INNER JOIN vouch_person_cross_ref vpc ON v.id = vpc.vouchId
        INNER JOIN Person p ON vpc.personId = p.id
        WHERE p.name = :name
        ORDER BY v.timestamp DESC
        """
    )
    suspend fun getVouchesByPersonName(name: String): List<Vouch>

    @Query(
        """
        SELECT vpc.vouchId AS vouchId, p.id AS id, p.name AS name, vpc.amount AS amount, vpc.settled AS settled
        FROM Person p
        INNER JOIN vouch_person_cross_ref vpc ON p.id = vpc.personId
        WHERE vpc.vouchId IN (
            SELECT vpc2.vouchId FROM vouch_person_cross_ref vpc2
            INNER JOIN Person p2 ON vpc2.personId = p2.id
            WHERE p2.name = :name
        )
        """
    )
    fun getPersonRowsForVouchesOfPersonFlow(name: String): Flow<List<VouchPersonRow>>

    @Query(
        """
        SELECT vpc.vouchId AS vouchId, p.id AS id, p.name AS name, vpc.amount AS amount, vpc.settled AS settled
        FROM Person p
        INNER JOIN vouch_person_cross_ref vpc ON p.id = vpc.personId
        WHERE vpc.vouchId IN (
            SELECT vpc2.vouchId FROM vouch_person_cross_ref vpc2
            INNER JOIN Person p2 ON vpc2.personId = p2.id
            WHERE p2.name = :name
        )
        """
    )
    suspend fun getPersonRowsForVouchesOfPerson(name: String): List<VouchPersonRow>

    @Query("SELECT * FROM Vouch WHERE id = :id")
    suspend fun getById(id: Int): Vouch?

    @Insert
    suspend fun insert(vouch: Vouch): Long

    @Update
    suspend fun update(vouch: Vouch)

    @Delete
    suspend fun delete(vouch: Vouch)

    @Query("DELETE FROM Vouch WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM Vouch")
    suspend fun deleteAll()

    @Insert
    suspend fun insertCrossRefs(refs: List<VouchPersonCrossRef>)

    @Query("DELETE FROM vouch_person_cross_ref WHERE vouchId = :vouchId")
    suspend fun deleteCrossRefsByVouchId(vouchId: Int)

    @Query(
        """
        SELECT p.*, vpc.amount AS amount, vpc.settled AS settled
        FROM Person p
        INNER JOIN vouch_person_cross_ref vpc ON p.id = vpc.personId
        WHERE vpc.vouchId = :vouchId
        """
    )
    suspend fun getPersonsForVouch(vouchId: Int): List<PersonWithAmount>

    @Query(
        """
        SELECT personId, settled
        FROM vouch_person_cross_ref
        WHERE vouchId = :vouchId
        """
    )
    suspend fun getSettlementsForVouch(vouchId: Int): List<VouchPersonSettlement>

    @Query(
        """
        UPDATE vouch_person_cross_ref
        SET settled = :settled
        WHERE vouchId = :vouchId AND personId = :personId
        """
    )
    suspend fun updateCrossRefSettlement(vouchId: Int, personId: Int, settled: Boolean)

    @Query(
        """
        UPDATE vouch_person_cross_ref
        SET settled = :settled
        WHERE personId = :personId
        """
    )
    suspend fun updateCrossRefsSettledByPersonId(personId: Int, settled: Boolean)

    @Query(
        """
        UPDATE vouch_person_cross_ref
        SET settled = :settled
        WHERE personId = :personId AND vouchId IN (:vouchIds)
        """
    )
    suspend fun updateCrossRefsSettledForPerson(personId: Int, vouchIds: List<Int>, settled: Boolean)

    @Query(
        """
        UPDATE vouch_person_cross_ref
        SET settled = :settled
        WHERE vouchId = :vouchId
        """
    )
    suspend fun updateCrossRefsSettledByVouchId(vouchId: Int, settled: Boolean)
}
