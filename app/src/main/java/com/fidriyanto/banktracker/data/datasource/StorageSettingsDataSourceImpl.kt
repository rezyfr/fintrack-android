package com.fidriyanto.banktracker.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fidriyanto.banktracker.data.model.BuiltInServer
import com.fidriyanto.banktracker.domain.model.ServerConfig
import com.fidriyanto.banktracker.domain.model.StorageMode
import com.fidriyanto.banktracker.domain.model.StorageSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

// The storage choice lives in the app's DataStore. A phone that never chose one, such as an install from
// before offline mode existed, uses the server built into the app, so it keeps working as before.
@Singleton
class StorageSettingsDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val builtIn: BuiltInServer,
) : StorageSettingsDataSource {
    @Volatile private var cache: StorageSettings? = null

    override suspend fun get(): StorageSettings = read(dataStore.data.first()).also { cache = it }

    // Only the HTTP interceptor calls this, on an OkHttp thread, so the one blocking read is safe.
    override fun current(): StorageSettings = cache ?: runBlocking { get() }

    override suspend fun save(mode: StorageMode, server: ServerConfig?) {
        dataStore.edit { prefs ->
            prefs[MODE] = mode.name
            if (server == null) {
                prefs.remove(URL)
                prefs.remove(TOKEN)
            } else {
                prefs[URL] = server.baseUrl.trimEnd('/')
                prefs[TOKEN] = server.token
            }
        }
        get()
    }

    private fun read(prefs: Preferences): StorageSettings {
        val fallback = builtIn.config
        return when (prefs[MODE]?.let { runCatching { StorageMode.valueOf(it) }.getOrNull() }) {
            null -> StorageSettings(if (fallback != null) StorageMode.SERVER else StorageMode.LOCAL, fallback, chosen = false)
            StorageMode.LOCAL -> StorageSettings(StorageMode.LOCAL, null, chosen = true)
            StorageMode.SERVER -> {
                val saved = prefs[URL]?.let { ServerConfig(it, prefs[TOKEN].orEmpty()) }
                StorageSettings(StorageMode.SERVER, saved ?: fallback, chosen = true)
            }
        }
    }

    private companion object {
        val MODE = stringPreferencesKey("storage_mode")
        val URL = stringPreferencesKey("server_url")
        val TOKEN = stringPreferencesKey("server_token")
    }
}
