package com.fidriyanto.banktracker.di

import com.fidriyanto.banktracker.data.datasource.MonthlyOverviewLocalDataSource
import com.fidriyanto.banktracker.data.datasource.MonthlyOverviewLocalDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.MonthlyOverviewRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.MonthlyOverviewRemoteDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.TransactionFetchDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionFetchDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.TransactionLocalDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionLocalDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.TransactionSyncDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionSyncDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.remote.BudgetRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.BudgetRemoteDataSourceImpl
import com.fidriyanto.banktracker.data.repository.BudgetRepository
import com.fidriyanto.banktracker.data.repository.BudgetRepositoryImpl
import com.fidriyanto.banktracker.data.repository.DashboardRepository
import com.fidriyanto.banktracker.data.repository.DashboardRepositoryImpl
import com.fidriyanto.banktracker.data.repository.TransactionFetchRepository
import com.fidriyanto.banktracker.data.repository.TransactionFetchRepositoryImpl
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds @Singleton abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds @Singleton abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository
    @Binds @Singleton abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository
    @Binds @Singleton abstract fun bindTransactionFetchRepository(impl: TransactionFetchRepositoryImpl): TransactionFetchRepository
    @Binds @Singleton abstract fun bindTransactionLocalDataSource(impl: TransactionLocalDataSourceImpl): TransactionLocalDataSource
    @Binds @Singleton abstract fun bindTransactionSyncDataSource(impl: TransactionSyncDataSourceImpl): TransactionSyncDataSource
    @Binds @Singleton abstract fun bindMonthlyOverviewLocalDataSource(impl: MonthlyOverviewLocalDataSourceImpl): MonthlyOverviewLocalDataSource
    @Binds @Singleton abstract fun bindMonthlyOverviewRemoteDataSource(impl: MonthlyOverviewRemoteDataSourceImpl): MonthlyOverviewRemoteDataSource
    @Binds @Singleton abstract fun bindBudgetRemoteDataSource(impl: BudgetRemoteDataSourceImpl): BudgetRemoteDataSource
    @Binds @Singleton abstract fun bindTransactionFetchDataSource(impl: TransactionFetchDataSourceImpl): TransactionFetchDataSource
}
