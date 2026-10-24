package com.fidriyanto.banktracker.data.datasource

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.fidriyanto.banktracker.data.model.BuiltInServer
import com.fidriyanto.banktracker.domain.model.ServerConfig
import com.fidriyanto.banktracker.domain.model.StorageMode
import com.fidriyanto.banktracker.domain.model.StorageSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StorageSettingsDataSourceImplTest {
    @get:Rule val folder = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val builtIn = BuiltInServer("https://built.in.example", "built-in-test-token")

    @After fun stop() = scope.cancel()

    private fun source(server: BuiltInServer = BuiltInServer("", "")): StorageSettingsDataSourceImpl {
        val store = PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "prefs.preferences_pb") }
        return StorageSettingsDataSourceImpl(store, server)
    }

    @Test fun aBuildWithoutAServerStartsOffline() = runBlocking {
        assertEquals(StorageSettings(StorageMode.LOCAL, null, chosen = false), source().get())
    }

    @Test fun aBuildWithAServerKeepsUsingIt() = runBlocking {
        assertEquals(StorageSettings(StorageMode.SERVER, ServerConfig("https://built.in.example", "built-in-test-token"), chosen = false), source(builtIn).get())
    }

    @Test fun anOfflineChoiceWinsOverTheBuiltInServer() = runBlocking {
        val ds = source(builtIn)
        ds.save(StorageMode.LOCAL, ServerConfig("https://ignored.example", "x"))
        assertEquals(StorageSettings(StorageMode.LOCAL, null, chosen = true), ds.get())
        assertEquals(StorageMode.LOCAL, ds.current().mode)
    }

    @Test fun aTypedServerIsStoredWithoutTheTrailingSlash() = runBlocking {
        val ds = source(builtIn)
        ds.save(StorageMode.SERVER, ServerConfig("https://mine.example/", "typed-test-token"))
        assertEquals(StorageSettings(StorageMode.SERVER, ServerConfig("https://mine.example", "typed-test-token"), chosen = true), ds.get())
    }
}
