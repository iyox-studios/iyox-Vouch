package com.iyox.vouch.model

import android.icu.util.Currency
import androidx.room.*
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
data class Vouch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: BigDecimal,
    val currency: Currency,
    val description: String = "",
    val personID: Int? = null,
    val given: Boolean = true,
    val settled: Boolean = false,
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
)

@Dao
interface VouchDataDao {
    @Query("SELECT * FROM Vouch")
    suspend fun getAll(): List<Vouch>

    @Insert
    suspend fun insert(vouch: Vouch)

    @Delete
    suspend fun delete(vouch: Vouch)

    @Update
    suspend fun update(vouch: Vouch)
}

