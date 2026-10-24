package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.CustomBankRepository
import com.fidriyanto.banktracker.domain.model.CustomBankMatcher
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

// ac: custom-bank-patterns - reads a notification with the user's own bank patterns. The first enabled
// pattern that matches wins, in list order. Null means no pattern matched, or the account of the pattern is gone.
class ParseCustomBankUseCase @Inject constructor(
    private val patterns: CustomBankRepository,
    private val accounts: AccountRepository,
) {
    suspend operator fun invoke(title: String?, text: String, timestampMs: Long): ParsedTransaction? {
        val (pattern, read) = CustomBankMatcher.firstMatch(patterns.all(), title, text) ?: return null
        val account = accounts.observeAll().first().firstOrNull { it.id == pattern.walletId } ?: return null
        return ParsedTransaction(
            item = read.merchant ?: pattern.name,
            amount = read.amount,
            date = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).toLocalDate(),
            referenceNo = "",
            timestampMs = timestampMs,
            wallet = account.id,
            category = "Other",
            txType = pattern.txType,
            rawFields = mapOf("source" to "custom"),
        )
    }
}
