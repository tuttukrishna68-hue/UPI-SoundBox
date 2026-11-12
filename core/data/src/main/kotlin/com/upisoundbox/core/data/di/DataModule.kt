package com.upisoundbox.core.data.di

import android.content.Context
import com.upisoundbox.core.data.db.PaymentDao
import com.upisoundbox.core.data.db.SoundboxDatabase
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.data.repository.PaymentRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSoundboxDatabase(@ApplicationContext context: Context): SoundboxDatabase {
        return SoundboxDatabase.getInstance(context, useEncryption = true)
    }

    @Provides
    @Singleton
    fun providePaymentDao(database: SoundboxDatabase): PaymentDao {
        return database.paymentDao()
    }

    @Provides
    @Singleton
    fun provideSoundboxPreferences(@ApplicationContext context: Context): SoundboxPreferences {
        return SoundboxPreferences(context)
    }

    @Provides
    @Singleton
    fun providePaymentRepository(paymentDao: PaymentDao): PaymentRepository {
        return PaymentRepositoryImpl(paymentDao)
    }
}

