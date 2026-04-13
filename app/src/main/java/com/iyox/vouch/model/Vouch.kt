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
    val personName: String? = null,
    val given: Boolean = true,
    val settled: Boolean = false,
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
)

@Dao
interface VouchDataDao {
    @Query("SELECT * FROM Vouch")
    suspend fun getAll(): List<Vouch>

    @Query("SELECT * FROM Vouch WHERE id = :id")
    suspend fun getById(id: Int): Vouch?

    @Query("SELECT SUM(amount) FROM Vouch WHERE personName = :personName")
    suspend fun getSumAllVouches(personName: String): BigDecimal

    @Insert
    suspend fun insert(vouch: Vouch)

    @Delete
    suspend fun delete(vouch: Vouch)

    @Update
    suspend fun update(vouch: Vouch)
}
