package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionFetchDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionFetchDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.offline.OfflineTransactionFetchDataSource
import com.fidriyanto.banktracker.data.db.TransactionEntity
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - sends each call to TransactionFetchDataSourceImpl or OfflineTransactionFetchDataSource, by the storage the user chose.
@Singleton
class TransactionFetchRouter @Inject constructor(
    storage: StorageSettingsDataSource,
    server: TransactionFetchDataSourceImpl,
    offline: OfflineTransactionFetchDataSource,
) : TransactionFetchDataSource {
    private val pick = StorageSwitch<TransactionFetchDataSource>(storage, server, offline)

    override suspend fun fetch(
        month: String?, wallet: String?, txType: String?, category: String?,
        search: String?, amountMin: Double?, amountMax: Double?,
        dateFrom: String?, dateTo: String?, involvingWallet: String?,
    ): Result<List<TransactionEntity>> = pick().fetch(month, wallet, txType, category, search, amountMin, amountMax, dateFrom, dateTo, involvingWallet)

    override suspend fun recentTransferRates(fromWallets: List<String>, toWallets: List<String>, limit: Int): Result<List<Double>> = pick().recentTransferRates(fromWallets, toWallets, limit)
}
