package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.AiSettingsRepository
import com.fidriyanto.banktracker.data.repository.CategoryRepository
import com.fidriyanto.banktracker.data.repository.LlmRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class ParseNotificationWithAiUseCase @Inject constructor(
    private val llm: LlmRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val settings: AiSettingsRepository,
) {
    // The model reads notifications the fixed patterns do not know. Null means: not a payment,
    // no wallet for that currency, the setting is off, or the model is not installed.
    suspend operator fun invoke(title: String, text: String, timestampMs: Long): ParsedTransaction? {
        if (!settings.observe().first().notificationsEnabled) return null
        val cats = categories.observeAll().first()
        val reply = llm.generate(AiPrompts.notification(cats), "Title: $title\nText: $text").getOrNull() ?: return null
        val all = accounts.observeAll().first()
        val read = AiReplyParser.parseNotification(reply, text, cats, all.map { it.currency }.toSet()) ?: return null
        val wallets = all.filter { it.currency == read.currency }
        val wallet = (wallets.firstOrNull { it.isDefault } ?: wallets.firstOrNull()) ?: return null
        return ParsedTransaction(
            item = read.merchant.ifEmpty { title },
            amount = read.amount,
            date = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).toLocalDate(),
            referenceNo = "",
            timestampMs = timestampMs,
            wallet = wallet.id,
            category = read.category,
            txType = if (read.isIncome) "income" else "expense",
            subcategory = read.subcategory,
            rawFields = mapOf("source" to "ai"),
        )
    }
}
