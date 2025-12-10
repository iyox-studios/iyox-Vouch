package com.iyox.vouch.model

import android.icu.util.Currency
import androidx.room.*
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
data class VouchDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: BigDecimal,
    val description: String = "",
    val given : Boolean = true,
    val currency: Currency,
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
)

@Dao
interface VouchDataDao {
    @Query("SELECT * FROM VouchDataEntity")
    suspend fun getAll(): List<VouchDataEntity>

    @Insert
    suspend fun insert(vouchData: VouchDataEntity)

    @Delete
    suspend fun delete(vouchData: VouchDataEntity)
}

