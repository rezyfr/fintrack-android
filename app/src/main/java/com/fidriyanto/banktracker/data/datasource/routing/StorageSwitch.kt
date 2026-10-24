package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.domain.model.StorageMode

// ac: offline-mode - picks the server or the offline version of a data source by the storage the user
// chose. It asks on every call, so a choice made in setup applies at once.
class StorageSwitch<T>(
    private val storage: StorageSettingsDataSource,
    private val server: T,
    private val offline: T,
) {
    suspend operator fun invoke(): T = if (storage.get().mode == StorageMode.LOCAL) offline else server
}
