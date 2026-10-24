package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.WalletBalanceDataSource
import com.fidriyanto.banktracker.data.datasource.WalletBalanceDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.offline.OfflineWalletBalanceDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceAtDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - sends each call to WalletBalanceDataSourceImpl or OfflineWalletBalanceDataSource, by the storage the user chose.
@Singleton
class WalletBalanceRouter @Inject constructor(
    storage: StorageSettingsDataSource,
    server: WalletBalanceDataSourceImpl,
    offline: OfflineWalletBalanceDataSource,
) : WalletBalanceDataSource {
    private val pick = StorageSwitch<WalletBalanceDataSource>(storage, server, offline)

    override suspend fun getBalances(): List<WalletBalanceDto> = pick().getBalances()

    override suspend fun getBalancesAt(dates: List<String>): List<WalletBalanceAtDto> = pick().getBalancesAt(dates)
}
