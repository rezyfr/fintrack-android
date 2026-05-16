package com.fidriyanto.banktracker.di

import android.content.Context
import androidx.room.Room
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.db.AppDatabase
import com.fidriyanto.banktracker.fake.FakeGoogleAuthManager
import com.fidriyanto.banktracker.fake.FakeSheetsSyncer
import com.fidriyanto.banktracker.sheets.SheetsSyncer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class, AuthModule::class, SheetsModule::class]
)
abstract class TestAppModule {

    @Binds @Singleton
    abstract fun bindSheetsSyncer(fake: FakeSheetsSyncer): SheetsSyncer

    @Binds @Singleton
    abstract fun bindGoogleAuthManager(fake: FakeGoogleAuthManager): GoogleAuthManager

    companion object {
        @Provides @Singleton
        fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        @Provides fun provideTransactionDao(db: AppDatabase) = db.transactionDao()
        @Provides fun provideCategoryCacheDao(db: AppDatabase) = db.categoryCacheDao()
        @Provides fun provideProcessedRefDao(db: AppDatabase) = db.processedRefDao()
        @Provides fun provideMonthlyOverviewDao(db: AppDatabase) = db.monthlyOverviewDao()

        @Provides @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient()
    }
}
