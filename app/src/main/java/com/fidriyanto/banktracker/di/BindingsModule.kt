package com.fidriyanto.banktracker.di

import com.fidriyanto.banktracker.data.datasource.routing.AccountRouter
import com.fidriyanto.banktracker.data.datasource.routing.AppSettingsRouter
import com.fidriyanto.banktracker.data.datasource.routing.BudgetLineRouter
import com.fidriyanto.banktracker.data.datasource.routing.CardBillingRouter
import com.fidriyanto.banktracker.data.datasource.routing.CategoryRouter
import com.fidriyanto.banktracker.data.datasource.routing.InstallmentRouter
import com.fidriyanto.banktracker.data.datasource.routing.RuleRouter
import com.fidriyanto.banktracker.data.datasource.routing.TransactionFetchRouter
import com.fidriyanto.banktracker.data.datasource.routing.TransactionSyncRouter
import com.fidriyanto.banktracker.data.datasource.routing.WalletBalanceRouter
import com.fidriyanto.banktracker.data.datasource.BackupFileDataSource
import com.fidriyanto.banktracker.data.datasource.BackupFileDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.LedgerBackupDataSource
import com.fidriyanto.banktracker.data.datasource.offline.LedgerBackupDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.remote.ServerProbeRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.ServerProbeRemoteDataSourceImpl
import com.fidriyanto.banktracker.data.repository.BackupRepository
import com.fidriyanto.banktracker.data.repository.BackupRepositoryImpl
import com.fidriyanto.banktracker.data.repository.StorageRepository
import com.fidriyanto.banktracker.data.repository.StorageRepositoryImpl
import com.fidriyanto.banktracker.data.datasource.PendingInstallmentStepDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.PendingInstallmentStepDataSource
import com.fidriyanto.banktracker.data.datasource.WalletBalanceDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionFetchDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionLocalDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionLocalDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.TransactionSyncDataSource
import com.fidriyanto.banktracker.data.datasource.remote.CardBillingRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.BudgetLineRemoteDataSource
import com.fidriyanto.banktracker.data.repository.CardBillingRepository
import com.fidriyanto.banktracker.data.repository.CardBillingRepositoryImpl
import com.fidriyanto.banktracker.data.repository.BudgetLineRepository
import com.fidriyanto.banktracker.data.repository.BudgetLineRepositoryImpl
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepositoryImpl
import com.fidriyanto.banktracker.data.datasource.InstallmentDataSource
import com.fidriyanto.banktracker.data.repository.InstallmentRepository
import com.fidriyanto.banktracker.data.repository.InstallmentRepositoryImpl
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepositoryImpl
import com.fidriyanto.banktracker.data.datasource.AccountLocalDataSource
import com.fidriyanto.banktracker.data.datasource.AccountLocalDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.CategoryLocalDataSource
import com.fidriyanto.banktracker.data.datasource.CategoryLocalDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.remote.AccountRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.CategoryRemoteDataSource
import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.AccountRepositoryImpl
import com.fidriyanto.banktracker.data.repository.CategoryRepository
import com.fidriyanto.banktracker.data.repository.CategoryRepositoryImpl
import com.fidriyanto.banktracker.data.datasource.CurrencySettingsDataSource
import com.fidriyanto.banktracker.data.datasource.CurrencySettingsDataSourceImpl
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepositoryImpl
import com.fidriyanto.banktracker.data.datasource.AiModelFileDataSource
import com.fidriyanto.banktracker.data.datasource.AiModelFileDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.AiSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.AiSettingsDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.AutoAddedDataSource
import com.fidriyanto.banktracker.data.datasource.AutoAddedDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.BankParserDataSource
import com.fidriyanto.banktracker.data.datasource.CustomBankDataSource
import com.fidriyanto.banktracker.data.datasource.CustomBankDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.ScheduleDataSource
import com.fidriyanto.banktracker.data.datasource.ScheduleDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.BankParserDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.RuleDataSource
import com.fidriyanto.banktracker.data.datasource.RuleDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.LlmDataSource
import com.fidriyanto.banktracker.data.datasource.LlmDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.remote.AiModelRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.AppSettingsRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.RuleRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.AiModelRemoteDataSourceImpl
import com.fidriyanto.banktracker.data.repository.AiModelRepository
import com.fidriyanto.banktracker.data.repository.AiModelRepositoryImpl
import com.fidriyanto.banktracker.data.repository.AiSettingsRepository
import com.fidriyanto.banktracker.data.repository.AiSettingsRepositoryImpl
import com.fidriyanto.banktracker.data.repository.AutoAddedRepository
import com.fidriyanto.banktracker.data.repository.AutoAddedRepositoryImpl
import com.fidriyanto.banktracker.data.repository.BankParserRepository
import com.fidriyanto.banktracker.data.repository.CustomBankRepository
import com.fidriyanto.banktracker.data.repository.CustomBankRepositoryImpl
import com.fidriyanto.banktracker.data.repository.ScheduleRepository
import com.fidriyanto.banktracker.data.repository.ScheduleRepositoryImpl
import com.fidriyanto.banktracker.data.repository.BankParserRepositoryImpl
import com.fidriyanto.banktracker.data.repository.RuleRepository
import com.fidriyanto.banktracker.data.repository.RuleRepositoryImpl
import com.fidriyanto.banktracker.data.repository.LlmRepository
import com.fidriyanto.banktracker.data.repository.LlmRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds @Singleton abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds @Singleton abstract fun bindTransactionLocalDataSource(impl: TransactionLocalDataSourceImpl): TransactionLocalDataSource
    @Binds @Singleton abstract fun bindTransactionSyncDataSource(impl: TransactionSyncRouter): TransactionSyncDataSource
    @Binds @Singleton abstract fun bindTransactionFetchDataSource(impl: TransactionFetchRouter): TransactionFetchDataSource
    @Binds @Singleton abstract fun bindWalletBalanceDataSource(impl: WalletBalanceRouter): WalletBalanceDataSource
    @Binds @Singleton abstract fun bindWalletBalanceRepository(impl: WalletBalanceRepositoryImpl): WalletBalanceRepository
    @Binds @Singleton abstract fun bindInstallmentDataSource(impl: InstallmentRouter): InstallmentDataSource
    @Binds @Singleton abstract fun bindInstallmentRepository(impl: InstallmentRepositoryImpl): InstallmentRepository
    @Binds @Singleton abstract fun bindPendingInstallmentStepDataSource(impl: PendingInstallmentStepDataSourceImpl): PendingInstallmentStepDataSource

    @Binds @Singleton
    abstract fun bindCardBillingRemoteDataSource(impl: CardBillingRouter): CardBillingRemoteDataSource

    @Binds @Singleton
    abstract fun bindCardBillingRepository(impl: CardBillingRepositoryImpl): CardBillingRepository

    @Binds @Singleton
    abstract fun bindBudgetLineRemoteDataSource(impl: BudgetLineRouter): BudgetLineRemoteDataSource

    @Binds @Singleton
    abstract fun bindBudgetLineRepository(impl: BudgetLineRepositoryImpl): BudgetLineRepository

    @Binds @Singleton
    abstract fun bindAccountLocalDataSource(impl: AccountLocalDataSourceImpl): AccountLocalDataSource

    @Binds @Singleton
    abstract fun bindAccountRemoteDataSource(impl: AccountRouter): AccountRemoteDataSource

    @Binds @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds @Singleton
    abstract fun bindCategoryLocalDataSource(impl: CategoryLocalDataSourceImpl): CategoryLocalDataSource

    @Binds @Singleton
    abstract fun bindCategoryRemoteDataSource(impl: CategoryRouter): CategoryRemoteDataSource

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds @Singleton
    abstract fun bindCurrencySettingsDataSource(impl: CurrencySettingsDataSourceImpl): CurrencySettingsDataSource

    @Binds @Singleton
    abstract fun bindCurrencySettingsRepository(impl: CurrencySettingsRepositoryImpl): CurrencySettingsRepository

    @Binds @Singleton abstract fun bindAppSettingsRemoteDataSource(impl: AppSettingsRouter): AppSettingsRemoteDataSource
    @Binds @Singleton abstract fun bindBankParserDataSource(impl: BankParserDataSourceImpl): BankParserDataSource
    @Binds @Singleton abstract fun bindBankParserRepository(impl: BankParserRepositoryImpl): BankParserRepository
    @Binds @Singleton abstract fun bindRuleRemoteDataSource(impl: RuleRouter): RuleRemoteDataSource
    @Binds @Singleton abstract fun bindRuleDataSource(impl: RuleDataSourceImpl): RuleDataSource
    @Binds @Singleton abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository
    @Binds @Singleton abstract fun bindLlmDataSource(impl: LlmDataSourceImpl): LlmDataSource
    @Binds @Singleton abstract fun bindLlmRepository(impl: LlmRepositoryImpl): LlmRepository
    @Binds @Singleton abstract fun bindAiModelFileDataSource(impl: AiModelFileDataSourceImpl): AiModelFileDataSource
    @Binds @Singleton abstract fun bindAiModelRemoteDataSource(impl: AiModelRemoteDataSourceImpl): AiModelRemoteDataSource
    @Binds @Singleton abstract fun bindAiModelRepository(impl: AiModelRepositoryImpl): AiModelRepository
    @Binds @Singleton abstract fun bindScheduleDataSource(impl: ScheduleDataSourceImpl): ScheduleDataSource
    @Binds @Singleton abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository
    @Binds @Singleton abstract fun bindCustomBankDataSource(impl: CustomBankDataSourceImpl): CustomBankDataSource
    @Binds @Singleton abstract fun bindCustomBankRepository(impl: CustomBankRepositoryImpl): CustomBankRepository
    @Binds @Singleton abstract fun bindAutoAddedDataSource(impl: AutoAddedDataSourceImpl): AutoAddedDataSource
    @Binds @Singleton abstract fun bindAutoAddedRepository(impl: AutoAddedRepositoryImpl): AutoAddedRepository
    @Binds @Singleton abstract fun bindAiSettingsDataSource(impl: AiSettingsDataSourceImpl): AiSettingsDataSource
    @Binds @Singleton abstract fun bindAiSettingsRepository(impl: AiSettingsRepositoryImpl): AiSettingsRepository

    // ac: setup-storage-choice - where the app keeps the data, and the server connection test
    @Binds @Singleton abstract fun bindStorageSettingsDataSource(impl: StorageSettingsDataSourceImpl): StorageSettingsDataSource
    @Binds @Singleton abstract fun bindServerProbeRemoteDataSource(impl: ServerProbeRemoteDataSourceImpl): ServerProbeRemoteDataSource
    @Binds @Singleton abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    // ac: offline-backup - the backup file of an offline install
    @Binds @Singleton abstract fun bindBackupFileDataSource(impl: BackupFileDataSourceImpl): BackupFileDataSource
    @Binds @Singleton abstract fun bindLedgerBackupDataSource(impl: LedgerBackupDataSourceImpl): LedgerBackupDataSource
    @Binds @Singleton abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
