package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.domain.model.AskMessage
import com.fidriyanto.banktracker.domain.model.LlmTool

interface LlmDataSource {
    // One answer from the on-device model. Fails when no model file is installed.
    suspend fun generate(
        system: String,
        prompt: String,
        history: List<AskMessage> = emptyList(),
        tools: List<LlmTool> = emptyList(),
    ): Result<String>
}
