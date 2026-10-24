package com.fidriyanto.banktracker.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFIX = "bank_account_"

@Singleton
class BankParserDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : BankParserDataSource {
    override fun observe(): Flow<Map<String, String>> = dataStore.data.map { prefs ->
        prefs.asMap().entries
            .filter { it.key.name.startsWith(PREFIX) }
            .associate { it.key.name.removePrefix(PREFIX) to it.value.toString() }
    }

    override suspend fun save(bankId: String, accountId: String) {
        dataStore.edit { it[stringPreferencesKey(PREFIX + bankId)] = accountId }
    }

    override suspend fun replaceAll(entries: Map<String, String>) {
        dataStore.edit { prefs ->
            prefs.asMap().keys.filter { it.name.startsWith(PREFIX) }.forEach { prefs.remove(it) }
            entries.forEach { (bankId, accountId) -> prefs[stringPreferencesKey(PREFIX + bankId)] = accountId }
        }
    }
}
