package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.ServerCheck
import com.fidriyanto.banktracker.domain.model.ServerConfig
import com.fidriyanto.banktracker.domain.model.StorageMode
import com.fidriyanto.banktracker.domain.model.StorageSettings

interface StorageRepository {
    suspend fun get(): StorageSettings

    suspend fun choose(mode: StorageMode, server: ServerConfig?)

    suspend fun check(server: ServerConfig): ServerCheck
}
