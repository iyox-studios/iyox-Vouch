package com.iyox.vouch.core.model

import android.icu.util.Currency
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
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
data class Vouch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: BigDecimal,
    val currency: Currency,
    val description: String = "",
    val given: Boolean = true,
    val settled: Boolean = false,
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
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
    indices = [Index("personId"), Index("vouchId")]
)
data class VouchPersonCrossRef(
    val vouchId: Int,
    val personId: Int,
    val amount: BigDecimal
)

data class VouchWithPerson(
    @Embedded val vouch: Vouch,
    val persons: List<PersonWithAmount>
)

@Dao
interface VouchDao {
    @Query("SELECT * FROM Vouch")
    suspend fun getAll(): List<Vouch>

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
        SELECT p.*, vpc.amount AS amount
        FROM Person p
        INNER JOIN vouch_person_cross_ref vpc ON p.id = vpc.personId
        WHERE vpc.vouchId = :vouchId
        """
    )
    suspend fun getPersonsForVouch(vouchId: Int): List<PersonWithAmount>
}
