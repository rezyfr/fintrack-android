package com.fidriyanto.banktracker.domain.model

// A function the on-device model may call. The data layer turns it into the library's tool type.
data class LlmTool(
    val name: String,
    val description: String,
    val params: List<LlmToolParam>,
    val run: suspend (Map<String, String>) -> String,
)

data class LlmToolParam(
    val name: String,
    val description: String,
    val required: Boolean = false,
)
