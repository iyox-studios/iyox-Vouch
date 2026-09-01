package com.iyox.vouch.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchPersonCrossRef
import com.iyox.vouch.core.model.VouchPersonRow
import com.iyox.vouch.core.model.VouchPersonSettlement
import kotlinx.coroutines.flow.Flow

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
    suspend fun getById(id: Long): Vouch?

    @Insert
    suspend fun insert(vouch: Vouch): Long

    @Update
    suspend fun update(vouch: Vouch)

    @Delete
    suspend fun delete(vouch: Vouch)

    @Query("DELETE FROM Vouch WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM Vouch")
    suspend fun deleteAll()

    @Insert
    suspend fun insertCrossRefs(refs: List<VouchPersonCrossRef>)

    @Query("DELETE FROM vouch_person_cross_ref WHERE vouchId = :vouchId")
    suspend fun deleteCrossRefsByVouchId(vouchId: Long)

    @Query(
        """
        SELECT p.*, vpc.amount AS amount, vpc.settled AS settled
        FROM Person p
        INNER JOIN vouch_person_cross_ref vpc ON p.id = vpc.personId
        WHERE vpc.vouchId = :vouchId
        """
    )
    suspend fun getPersonsForVouch(vouchId: Long): List<PersonWithAmount>

    @Query(
        """
        SELECT personId, settled
        FROM vouch_person_cross_ref
        WHERE vouchId = :vouchId
        """
    )
    suspend fun getSettlementsForVouch(vouchId: Long): List<VouchPersonSettlement>

    @Query(
        """
        UPDATE vouch_person_cross_ref
        SET settled = :settled
        WHERE vouchId = :vouchId AND personId = :personId
        """
    )
    suspend fun updateCrossRefSettlement(vouchId: Long, personId: Long, settled: Boolean)

    @Query(
        """
        UPDATE vouch_person_cross_ref
        SET settled = :settled
        WHERE personId = :personId
        """
    )
    suspend fun updateCrossRefsSettledByPersonId(personId: Long, settled: Boolean)

    @Query(
        """
        UPDATE vouch_person_cross_ref
        SET settled = :settled
        WHERE personId = :personId AND vouchId IN (:vouchIds)
        """
    )
    suspend fun updateCrossRefsSettledForPerson(personId: Long, vouchIds: List<Long>, settled: Boolean)

    @Query(
        """
        UPDATE vouch_person_cross_ref
        SET settled = :settled
        WHERE vouchId = :vouchId
        """
    )
    suspend fun updateCrossRefsSettledByVouchId(vouchId: Long, settled: Boolean)
}
