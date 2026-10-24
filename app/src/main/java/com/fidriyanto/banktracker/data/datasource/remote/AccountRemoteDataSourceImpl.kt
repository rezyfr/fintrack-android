package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.AccountDto
import javax.inject.Inject

class AccountRemoteDataSourceImpl @Inject constructor(
    private val service: AccountApiService,
) : AccountRemoteDataSource {
    override suspend fun getAccounts(): List<AccountDto> = service.getAccounts()

    override suspend fun upsertAccount(account: AccountDto) {
        val response = service.putAccount(account.id, account)
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
    }

    override suspend fun deleteAccount(id: String) {
        val response = service.deleteAccount(id)
        if (!response.isSuccessful && response.code() != 404) error("API error: HTTP ${response.code()}")
    }
}
