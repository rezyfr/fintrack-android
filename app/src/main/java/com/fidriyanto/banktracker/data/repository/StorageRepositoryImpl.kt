package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.remote.ServerProbeRemoteDataSource
import com.fidriyanto.banktracker.domain.model.ServerCheck
import com.fidriyanto.banktracker.domain.model.ServerConfig
import com.fidriyanto.banktracker.domain.model.StorageMode
import com.fidriyanto.banktracker.domain.model.StorageSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val settings: StorageSettingsDataSource,
    private val probe: ServerProbeRemoteDataSource,
) : StorageRepository {
    override suspend fun get(): StorageSettings = settings.get()

    override suspend fun choose(mode: StorageMode, server: ServerConfig?) =
        settings.save(mode, server.takeIf { mode == StorageMode.SERVER })

    override suspend fun check(server: ServerConfig): ServerCheck = probe.check(server)
}
