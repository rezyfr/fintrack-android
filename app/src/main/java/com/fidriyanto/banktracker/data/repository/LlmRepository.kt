package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.AskMessage
import com.fidriyanto.banktracker.domain.model.LlmTool

interface LlmRepository {
    suspend fun generate(
        system: String,
        prompt: String,
        history: List<AskMessage> = emptyList(),
        tools: List<LlmTool> = emptyList(),
    ): Result<String>
}
