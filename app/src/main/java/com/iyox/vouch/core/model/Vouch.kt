package com.iyox.vouch.core.model

import android.icu.util.Currency
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import java.math.BigDecimal
import java.sql.Timestamp

@Entity(indices = [Index(value = ["personName"])])
data class Vouch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: BigDecimal,
    val currency: Currency,
    val description: String = "",
    val personName: String? = null,
    val given: Boolean = true,
    val settled: Boolean = false,
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
)

@Dao
interface VouchDao {
    @Query("SELECT * FROM Vouch")
    suspend fun getAll(): List<Vouch>

    @Query("SELECT * FROM Vouch WHERE id = :id")
    suspend fun getById(id: Int): Vouch?

    @Insert
    suspend fun insert(vouch: Vouch)

    @Delete
    suspend fun delete(vouch: Vouch)

    @Update
    suspend fun update(vouch: Vouch)

    @Query("DELETE FROM Vouch")
    suspend fun deleteAll()
}
