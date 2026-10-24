package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.datasource.remote.AccountRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.AccountDto
import com.fidriyanto.banktracker.data.db.AccountDao
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - in offline mode the accounts table on the phone is the only copy. A read returns it,
// and a write does nothing here, because AccountRepository writes the same table right after.
@Singleton
class OfflineAccountDataSource @Inject constructor(
    private val dao: AccountDao,
) : AccountRemoteDataSource {
    override suspend fun getAccounts(): List<AccountDto> =
        dao.observeAll().first().map { AccountDto(it.id, it.name, it.currency, it.sortOrder, it.isDefault, it.type, it.openingBalance) }

    override suspend fun upsertAccount(account: AccountDto) = Unit

    override suspend fun deleteAccount(id: String) = Unit
}
