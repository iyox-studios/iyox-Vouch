package com.iyox.vouch.core.data

import android.content.Context
import android.icu.util.Currency
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonDao
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchDataDao
import java.math.BigDecimal
import java.sql.Timestamp

class TimestampConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Timestamp? = value?.let { Timestamp(it) }

    @TypeConverter
    fun timestampToLong(timestamp: Timestamp?): Long? = timestamp?.time
}

class CurrencyConverter {
    @TypeConverter
    fun fromCurrency(currency: Currency): String = currency.currencyCode

    @TypeConverter
    fun toCurrency(currencyCode: String): Currency = try {
        Currency.getInstance(currencyCode)
    } catch (e: Exception) {
        Currency.getInstance("USD")
    }
}

class BigDecimalConverter {
    @TypeConverter
    fun fromBigDecimal(bigDecimal: BigDecimal): String = bigDecimal.toString()

    @TypeConverter
    fun toBigDecimal(string: String): BigDecimal = BigDecimal(string)
}

@Database(entities = [Vouch::class, Person::class], version = 2, exportSchema = false)
@TypeConverters(TimestampConverter::class, CurrencyConverter::class, BigDecimalConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vouchDataDao(): VouchDataDao
    abstract fun personDao(): PersonDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vouch_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}