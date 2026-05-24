package com.fidriyanto.banktracker.di

import com.fidriyanto.banktracker.sheets.SheetsSyncer
import com.fidriyanto.banktracker.sheets.SupabaseSyncerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SheetsModule {
    @Binds @Singleton
    abstract fun bindSheetsSyncer(impl: SupabaseSyncerImpl): SheetsSyncer
}
