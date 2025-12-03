package com.iyox.vouch.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.sql.Timestamp

class TimestampConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Timestamp? {
        return value?.let { Timestamp(it) }
    }

    @TypeConverter
    fun timestampToLong(timestamp: Timestamp?): Long? {
        return timestamp?.time
    }
}

@Database(entities = [VouchDataEntity::class], version = 1, exportSchema = false)
@TypeConverters(TimestampConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vouchDataDao(): VouchDataDao
}

class VouchRepository private constructor(context: Context) {

    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "vouch_database"
                ).build()
    private val vouchDataDao = database.vouchDataDao()

    suspend fun getAll(): List<VouchDataEntity> {
        return vouchDataDao.getAll()
    }

    suspend fun insert(vouchData: VouchDataEntity) {
        vouchDataDao.insert(vouchData)
    }

    suspend fun delete(vouchData: VouchDataEntity) {
        vouchDataDao.delete(vouchData)
    }

    companion object {
        @Volatile
        private var INSTANCE: VouchRepository? = null

        fun getInstance(context: Context): VouchRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = VouchRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}