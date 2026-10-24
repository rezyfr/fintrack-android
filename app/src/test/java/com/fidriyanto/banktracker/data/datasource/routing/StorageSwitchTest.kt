package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.domain.model.StorageMode
import com.fidriyanto.banktracker.domain.model.StorageSettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageSwitchTest {
    @Test fun theSwitchAsksForTheModeOnEveryCall() = runBlocking {
        val storage = mockk<StorageSettingsDataSource>()
        val pick = StorageSwitch(storage, server = "server", offline = "offline")
        coEvery { storage.get() } returns StorageSettings(StorageMode.LOCAL, null, chosen = true)
        assertEquals("offline", pick())
        coEvery { storage.get() } returns StorageSettings(StorageMode.SERVER, null, chosen = true)
        assertEquals("server", pick())
    }
}
