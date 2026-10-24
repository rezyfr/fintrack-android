package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.repository.RuleRepository
import com.fidriyanto.banktracker.domain.model.RuleInput
import com.fidriyanto.banktracker.domain.model.RuleOutcome
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class ApplyRulesUseCase @Inject constructor(
    private val rules: RuleRepository,
) {
    suspend operator fun invoke(input: RuleInput): RuleOutcome = RuleEngine.apply(rules.observe().first(), input)

    // A captured transaction with the rules applied. Null means a rule says to ignore it.
    suspend fun forCapture(parsed: ParsedTransaction, zone: ZoneId = ZoneId.systemDefault()): ParsedTransaction? {
        val at = Instant.ofEpochMilli(parsed.timestampMs).atZone(zone).toLocalDateTime()
        val out = invoke(RuleInput(parsed.item, parsed.amount, parsed.txType, parsed.wallet, at))
        if (out.ignore) return null
        return parsed.copy(
            item = out.description ?: parsed.item,
            category = out.category ?: parsed.category,
            subcategory = if (out.category != null) out.subcategory else parsed.subcategory,
        )
    }
}
