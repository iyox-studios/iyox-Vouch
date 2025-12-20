package com.iyox.vouch.model

import android.content.Context
import android.icu.util.Currency
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.math.BigDecimal
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

class CurrencyConverter {

    @TypeConverter
    fun fromCurrency(currency: Currency): String {
        return currency.currencyCode
    }

    @TypeConverter
    fun toCurrency(currencyCode: String): Currency {
        return try {
            Currency.getInstance(currencyCode)
        } catch (e: Exception) {
            Currency.getInstance("USD")
        }
    }
}

class BigDecimalConverter {

    @TypeConverter
    fun fromBigDecimal(bigDecimal: BigDecimal): String {

        return bigDecimal.toString()
    }

    @TypeConverter
    fun toBigDecimal(string: String): BigDecimal {
        return BigDecimal(string)
    }
}

@Database(entities = [VouchDataEntity::class], version = 1, exportSchema = false)
@TypeConverters(TimestampConverter::class, CurrencyConverter::class, BigDecimalConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vouchDataDao(): VouchDataDao
}

class VouchRepository private constructor(context: Context) {

    private val database = Room.databaseBuilder(
        context.applicationContext, AppDatabase::class.java, "vouch_database"
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

    suspend fun update(vouchData: VouchDataEntity) {
        vouchDataDao.update(vouchData)
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