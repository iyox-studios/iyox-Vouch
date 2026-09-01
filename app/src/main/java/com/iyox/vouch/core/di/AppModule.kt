package com.iyox.vouch.core.di

import android.content.Context
import com.iyox.vouch.core.utils.VibrationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideVibrationManager(
        @ApplicationContext context: Context
    ): VibrationManager {
        return VibrationManager(context)
    }
}
