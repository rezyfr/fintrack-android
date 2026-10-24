package com.fidriyanto.banktracker.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fidriyanto.banktracker.domain.model.CurrencyOverride
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val HOME_CODE_KEY = stringPreferencesKey("currency_home_code")
private val HOME_LABEL_KEY = stringPreferencesKey("currency_home_label")
private val CYCLE_DAY_KEY = intPreferencesKey("currency_cycle_start_day")
private val OVERRIDES_KEY = stringPreferencesKey("currency_overrides")

// The keys of the version that kept one foreign currency. They are read when the newer key is absent.
private val FOREIGN_CODE_KEY = stringPreferencesKey("currency_foreign_code")
private val FOREIGN_LABEL_KEY = stringPreferencesKey("currency_foreign_label")
private val FALLBACK_RATE_KEY = stringPreferencesKey("currency_fallback_rate")

@Singleton
class CurrencySettingsDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : CurrencySettingsDataSource {
    private val defaults = CurrencySettings()
    private val gson = Gson()
    private val listType = object : TypeToken<List<CurrencyOverride>>() {}.type

    // ac: configurable-currency-pair - reads fall back to the shipped IDR home currency and THB "Thailand"
    // until the user saves settings, so an existing installation is unaffected until it opts in
    override fun observe(): Flow<CurrencySettings> = dataStore.data.map { prefs ->
        CurrencySettings(
            homeCode = prefs[HOME_CODE_KEY] ?: defaults.homeCode,
            homeLabel = prefs[HOME_LABEL_KEY] ?: defaults.homeLabel,
            cycleStartDay = prefs[CYCLE_DAY_KEY] ?: defaults.cycleStartDay,
            overrides = prefs[OVERRIDES_KEY]?.let { runCatching { gson.fromJson<List<CurrencyOverride>>(it, listType) }.getOrNull() }
                ?: legacyOverrides(prefs)
                ?: defaults.overrides,
        )
    }

    // The earlier version kept one foreign currency. A saved empty code meant one currency.
    private fun legacyOverrides(prefs: Preferences): List<CurrencyOverride>? {
        val code = prefs[FOREIGN_CODE_KEY] ?: return null
        if (code.isBlank()) return emptyList()
        return listOf(CurrencyOverride(code, prefs[FOREIGN_LABEL_KEY]?.ifBlank { null } ?: code, prefs[FALLBACK_RATE_KEY]?.toDoubleOrNull()))
    }

    override suspend fun save(settings: CurrencySettings) {
        dataStore.edit { prefs ->
            prefs[HOME_CODE_KEY] = settings.homeCode
            prefs[HOME_LABEL_KEY] = settings.homeLabel
            prefs[CYCLE_DAY_KEY] = settings.cycleStartDay
            prefs[OVERRIDES_KEY] = gson.toJson(settings.overrides)
        }
    }
}
