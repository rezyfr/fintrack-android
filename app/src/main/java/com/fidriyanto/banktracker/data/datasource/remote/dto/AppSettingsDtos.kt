package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

// One non-home currency: its book name and its starting rate (home units per unit, absent when unset).
data class CurrencyDto(
    @SerializedName("code") val code: String,
    @SerializedName("label") val label: String,
    @SerializedName("rate") val rate: Double? = null,
)

// The app settings. The server keeps one copy.
data class AppSettingsDto(
    @SerializedName("home_code") val homeCode: String,
    @SerializedName("home_label") val homeLabel: String,
    @SerializedName("cycle_start_day") val cycleStartDay: Int = 1,
    @SerializedName("currencies") val currencies: List<CurrencyDto> = emptyList(),
)
