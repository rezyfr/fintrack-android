package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.AccountDto

interface AccountRemoteDataSource {
    suspend fun getAccounts(): List<AccountDto>
    suspend fun upsertAccount(account: AccountDto)
    suspend fun deleteAccount(id: String)
}
