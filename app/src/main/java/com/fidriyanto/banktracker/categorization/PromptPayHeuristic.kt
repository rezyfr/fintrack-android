package com.fidriyanto.banktracker.categorization

object PromptPayHeuristic {
    fun isLikelyTransferOut(channel: String, amount: Double, threshold: Double): Boolean =
        channel == "PromptPay" && amount >= threshold
}
