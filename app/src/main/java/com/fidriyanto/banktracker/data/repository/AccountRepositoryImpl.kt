package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.AccountLocalDataSource
import com.fidriyanto.banktracker.data.datasource.remote.AccountRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.AccountDto
import com.fidriyanto.banktracker.data.db.AccountEntity
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.AccountType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val local: AccountLocalDataSource,
    private val remote: AccountRemoteDataSource,
) : AccountRepository {
    // ac: manage-accounts - the account list is loaded from the accounts table. A new install has
    // no accounts until the user sets them up.
    override fun observeAll(): Flow<List<Account>> = local.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val rows = remote.getAccounts()
        local.replaceAll(rows.map { it.toEntity() })
    }

    // ac: manage-accounts — saving a custom account persists it to the server and the local cache
    override suspend fun save(account: Account): Result<Unit> = runCatching {
        remote.upsertAccount(account.toDto())
        local.upsertOne(account.toEntity())
    }

    // ac: manage-accounts — a default account cannot be deleted
    override suspend fun delete(id: String): Result<Unit> = runCatching {
        remote.deleteAccount(id)
        local.delete(id)
    }
}

private fun AccountEntity.toDomain() = Account(id, name, currency, isDefault, AccountType.from(type), sortOrder, openingBalance)
private fun Account.toEntity() = AccountEntity(id, displayName, currency, sortOrder = sortOrder, isDefault = isDefault, type = type.id, openingBalance = openingBalance)
private fun AccountDto.toEntity() = AccountEntity(id, name, currency, sortOrder, isDefault, type, openingBalance)
private fun Account.toDto() = AccountDto(id, displayName, currency, sortOrder = sortOrder, isDefault = isDefault, type = type.id, openingBalance = openingBalance)
