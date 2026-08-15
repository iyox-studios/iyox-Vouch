package com.iyox.vouch.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonDao
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchDao
import com.iyox.vouch.core.model.VouchPersonCrossRef
import java.sql.Timestamp

class TimestampConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Timestamp? = value?.let { Timestamp(it) }

    @TypeConverter
    fun timestampToLong(timestamp: Timestamp?): Long? = timestamp?.time
}

@Database(
    entities = [Vouch::class, Person::class, VouchPersonCrossRef::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(TimestampConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vouchDataDao(): VouchDao
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}