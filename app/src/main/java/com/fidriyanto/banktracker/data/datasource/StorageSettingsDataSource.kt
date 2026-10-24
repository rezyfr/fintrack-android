package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.domain.model.ServerConfig
import com.fidriyanto.banktracker.domain.model.StorageMode
import com.fidriyanto.banktracker.domain.model.StorageSettings

interface StorageSettingsDataSource {
    // The storage the app uses now.
    suspend fun get(): StorageSettings

    // The same without suspending, for the HTTP layer. It reads the phone once when nothing was read yet.
    fun current(): StorageSettings

    suspend fun save(mode: StorageMode, server: ServerConfig?)
}
