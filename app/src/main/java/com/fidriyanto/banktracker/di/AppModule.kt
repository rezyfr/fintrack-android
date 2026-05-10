package com.fidriyanto.banktracker.di

import android.content.Context
import androidx.room.Room
import com.fidriyanto.banktracker.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "banktracker.db").build()

    @Provides fun provideTransactionDao(db: AppDatabase) = db.transactionDao()
    @Provides fun provideCategoryCacheDao(db: AppDatabase) = db.categoryCacheDao()
    @Provides fun provideProcessedRefDao(db: AppDatabase) = db.processedRefDao()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
