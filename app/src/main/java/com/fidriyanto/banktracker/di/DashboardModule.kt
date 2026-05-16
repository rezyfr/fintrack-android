package com.fidriyanto.banktracker.di

import com.fidriyanto.banktracker.sheets.MonthlyOverviewFetcher
import com.fidriyanto.banktracker.sheets.MonthlyOverviewFetcherImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {
    @Binds @Singleton
    abstract fun bindMonthlyOverviewFetcher(impl: MonthlyOverviewFetcherImpl): MonthlyOverviewFetcher
}
