package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.LlmDataSource
import com.fidriyanto.banktracker.domain.model.AskMessage
import com.fidriyanto.banktracker.domain.model.LlmTool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmRepositoryImpl @Inject constructor(
    private val llm: LlmDataSource,
) : LlmRepository {
    override suspend fun generate(system: String, prompt: String, history: List<AskMessage>, tools: List<LlmTool>) =
        llm.generate(system, prompt, history, tools)
}
