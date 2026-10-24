package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.CustomBankRepository
import com.fidriyanto.banktracker.domain.model.CustomBankPattern
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ac: custom-bank-patterns - read, save, delete and switch on or off the user's own bank patterns
class ManageCustomBanksUseCase @Inject constructor(
    private val patterns: CustomBankRepository,
) {
    fun observe(): Flow<List<CustomBankPattern>> = patterns.observe()
    suspend fun find(id: String): CustomBankPattern? = patterns.all().firstOrNull { it.id == id }
    suspend fun save(pattern: CustomBankPattern) = patterns.upsert(pattern)
    suspend fun delete(id: String) = patterns.delete(id)

    suspend fun setEnabled(id: String, on: Boolean) {
        find(id)?.let { patterns.upsert(it.copy(enabled = on)) }
    }
}
