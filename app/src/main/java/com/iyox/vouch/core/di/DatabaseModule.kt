package com.iyox.vouch.core.di

import android.content.Context
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.PersonDao
import com.iyox.vouch.core.data.VouchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideVouchDao(database: AppDatabase): VouchDao {
        return database.vouchDao()
    }

    @Provides
    fun providePersonDao(database: AppDatabase): PersonDao {
        return database.personDao()
    }
}
