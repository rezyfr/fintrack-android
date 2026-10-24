package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.fidriyanto.banktracker.domain.model.Rule
import com.google.gson.annotations.SerializedName

// The whole rule list and the time of its last edit in milliseconds. The server keeps one copy.
data class RulesDocDto(
    @SerializedName("rules") val rules: List<Rule> = emptyList(),
    @SerializedName("updated_at_ms") val updatedAtMs: Long = 0L,
)
