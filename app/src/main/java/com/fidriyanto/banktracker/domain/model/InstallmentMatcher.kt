package com.fidriyanto.banktracker.domain.model

import kotlin.math.abs

// ac: installment-auto-link — finds the installment a card transaction belongs to. A match needs the
// same wallet, the installment's merchant_pattern inside the item text (any case), and the same
// amount. The amount is required because two plans can share one pattern, for example two
// "TIKET.COM 0%3" plans with different amounts. Zero or several matches mean no link.
object InstallmentMatcher {
    private const val AMOUNT_TOLERANCE = 1.0
    private val slashStep = Regex("""(\d{1,3})\s*/\s*(\d{1,3})""")
    private val keDariStep = Regex("""KE\s+(\d{1,3})\s+DARI\s+(\d{1,3})""", RegexOption.IGNORE_CASE)

    // The (step, total) pairs written in a statement line, for example "009/012" or "KE 03 DARI 03".
    // A pair whose step is above its total is not a step, so dates such as "25/12" are skipped.
    fun parseSteps(item: String): List<Pair<Int, Int>> =
        (slashStep.findAll(item) + keDariStep.findAll(item))
            .mapNotNull { m ->
                val step = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val total = m.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                (step to total).takeIf { step in 1..total }
            }
            .toList()

    // ac: installment-plan-add - true when an active plan on the wallet has the same statement text and the same amount.
    // The matcher then finds two plans for one payment and links none.
    fun collides(plans: List<Installment>, wallet: String, pattern: String, amount: Double): Boolean {
        val text = pattern.trim()
        if (text.isEmpty()) return false
        return plans.any {
            !it.isCompleted && it.wallet == wallet && it.merchantPattern.isNotBlank() &&
                (it.merchantPattern.contains(text, ignoreCase = true) || text.contains(it.merchantPattern, ignoreCase = true)) &&
                abs(it.installmentAmount - amount) <= AMOUNT_TOLERANCE
        }
    }

    // ac: installment-auto-link — the plan whose wallet, pattern and amount fit the transaction
    fun match(item: String, wallet: String, amount: Double, installments: List<Installment>): InstallmentMatch? {
        val plan = installments.filter {
            !it.isCompleted && it.wallet == wallet && it.merchantPattern.isNotBlank() &&
                item.contains(it.merchantPattern, ignoreCase = true) &&
                abs(it.installmentAmount - amount) <= AMOUNT_TOLERANCE
        }.singleOrNull() ?: return null
        return advance(plan, item)
    }

    // ac: installment-auto-link — the step comes from the statement text when it has one, and is never
    // lower than the current step, so a step you already fixed by hand stays. Without a number in the
    // text, the step moves up by one. A step written for a different plan length gives no result when
    // strict. A plan the user picked by hand is not strict, so it falls back to one step up.
    fun advance(plan: Installment, item: String, strict: Boolean = true): InstallmentMatch? {
        val pairs = parseSteps(item)
        val written = pairs.firstOrNull { it.second == plan.totalInstallments }?.first
        if (pairs.isNotEmpty() && written == null && strict) return null
        val step = written ?: (plan.currentStep + 1)
        val newStep = maxOf(plan.currentStep, step).coerceAtMost(plan.totalInstallments)
        return InstallmentMatch(plan, newStep, if (newStep >= plan.totalInstallments) "completed" else "active")
    }
}
