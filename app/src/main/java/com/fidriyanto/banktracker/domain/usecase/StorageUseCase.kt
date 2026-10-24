package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.StorageRepository
import com.fidriyanto.banktracker.domain.model.ServerCheck
import com.fidriyanto.banktracker.domain.model.ServerConfig
import com.fidriyanto.banktracker.domain.model.StorageMode
import com.fidriyanto.banktracker.domain.model.StorageSettings
import com.fidriyanto.banktracker.domain.model.normalizeServerAddress
import javax.inject.Inject

// ac: setup-storage-choice - reads and stores where the app keeps the data, and tests a server
// address and token before setup stores them.
class StorageUseCase @Inject constructor(
    private val storage: StorageRepository,
) {
    suspend fun get(): StorageSettings = storage.get()

    suspend fun choose(mode: StorageMode, server: ServerConfig?) = storage.choose(mode, server)

    // Tests the address the user typed. The token is sent as it is.
    suspend fun check(address: String, token: String): ServerCheck {
        val url = normalizeServerAddress(address) ?: return ServerCheck.BadAddress
        return storage.check(ServerConfig(url, token.trim()))
    }
}
