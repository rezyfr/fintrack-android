package com.fidriyanto.banktracker.categorization

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.email.MerchantNormalizer
import javax.inject.Inject
import javax.inject.Singleton

data class ResolvedCategory(val category: String, val description: String, val flagged: Boolean = false)

@Singleton
class CategoryResolver @Inject constructor(
    private val claude: ClaudeCategorizor
) {
    suspend fun resolve(
        tx: ParsedTransaction,
        promptPayThreshold: Double,
        claudeApiKey: String
    ): ResolvedCategory {
        val normalized = MerchantNormalizer.normalize(tx.merchant)

        RuleBasedMatcher.match(normalized)?.let {
            return ResolvedCategory(it, tx.merchant)
        }

        if (PromptPayHeuristic.isLikelyTransferOut(tx.channel, tx.amount, promptPayThreshold)) {
            return ResolvedCategory("Transfer Out", tx.merchant, flagged = true)
        }

        val (category, description) = claude.categorize(tx.merchant, tx.amount, tx.channel, claudeApiKey)
        return ResolvedCategory(category, description)
    }
}
