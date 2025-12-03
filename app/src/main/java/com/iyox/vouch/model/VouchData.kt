package com.iyox.vouch.model

import androidx.room.*
import java.sql.Timestamp

data class VouchData(
    val id: Int = 0,
    val amount: Int = 0,
    val description: String = "",
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
)

@Entity
data class VouchDataEntity(
    @PrimaryKey val id: Int = 0,
    val amount: Int = 0,
    val description: String = "",
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

