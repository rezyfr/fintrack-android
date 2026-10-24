package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.CurrencySettingsDataSource
import com.fidriyanto.banktracker.data.datasource.remote.AppSettingsRemoteDataSource
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencySettingsRepositoryImpl @Inject constructor(
    private val local: CurrencySettingsDataSource,
    private val remote: AppSettingsRemoteDataSource,
) : CurrencySettingsRepository {
    override fun observe(): Flow<CurrencySettings> = local.observe()

    // The phone keeps the settings first. The shared row follows, so the web app and a second phone
    // read the same choices. A failed upload is retried by the next save.
    override suspend fun save(settings: CurrencySettings) {
        local.save(settings)
        runCatching { remote.save(settings) }
    }

    override suspend fun refresh(): Result<Unit> = runCatching {
        remote.get()?.let { local.save(it) }
    }
}
