package com.iyox.vouch.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchPersonCrossRef

@Database(
    entities = [Vouch::class, Person::class, VouchPersonCrossRef::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vouchDao(): VouchDao
    abstract fun personDao(): PersonDao

    // Backwards compatibility alias
    fun vouchDataDao(): VouchDao = vouchDao()

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