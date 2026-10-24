package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.offline.OfflineAccountDataSource
import com.fidriyanto.banktracker.data.datasource.remote.AccountRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.AccountRemoteDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.remote.dto.AccountDto
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - sends each call to AccountRemoteDataSourceImpl or OfflineAccountDataSource, by the storage the user chose.
@Singleton
class AccountRouter @Inject constructor(
    storage: StorageSettingsDataSource,
    server: AccountRemoteDataSourceImpl,
    offline: OfflineAccountDataSource,
) : AccountRemoteDataSource {
    private val pick = StorageSwitch<AccountRemoteDataSource>(storage, server, offline)

    override suspend fun getAccounts(): List<AccountDto> = pick().getAccounts()

    override suspend fun upsertAccount(account: AccountDto) = pick().upsertAccount(account)

    override suspend fun deleteAccount(id: String) = pick().deleteAccount(id)
}
