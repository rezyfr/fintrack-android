package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AiSettingsRepository
import com.fidriyanto.banktracker.data.repository.CategoryRepository
import com.fidriyanto.banktracker.data.repository.LlmRepository
import com.fidriyanto.banktracker.domain.model.CategorySuggestion
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val MIN_DESCRIPTION = 3

class SuggestCategoryUseCase @Inject constructor(
    private val llm: LlmRepository,
    private val categories: CategoryRepository,
    private val settings: AiSettingsRepository,
) {
    // A category and subcategory for what the user typed, or null when the model is off or unsure.
    suspend operator fun invoke(description: String): CategorySuggestion? {
        val text = description.trim()
        if (text.length < MIN_DESCRIPTION || !settings.observe().first().suggestionsEnabled) return null
        val cats = categories.observeAll().first()
        val reply = llm.generate(AiPrompts.suggestion(cats), text).getOrNull() ?: return null
        return AiReplyParser.parseSuggestion(reply, cats)
    }
}
