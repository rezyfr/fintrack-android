package com.fidriyanto.banktracker.ui.settings.installmentplans

import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.model.InstallmentMatcher
import com.fidriyanto.banktracker.domain.model.NewInstallment
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.splitStatementStep
import java.time.LocalDate

enum class PlanError { NAME, ACCOUNT, AMOUNT, TOTAL, PAID, DUE_DAY, STATEMENT }

// What the editor holds while the user fills a plan in. The numbers are text so a half-typed number is kept.
data class InstallmentPlanDraft(
    val name: String = "",
    val walletId: String = "",
    val amount: String = "",
    val total: String = "",
    val paid: String = "0",
    val dueDay: Int = LocalDate.now().dayOfMonth,
    // Null until the user edits it. Until then the statement text is the name.
    val statement: String? = null,
) {
    val amountValue: Double? get() = amount.replace(",", "").toDoubleOrNull()?.takeIf { it > 0.0 }
    val totalValue: Int? get() = total.toIntOrNull()?.takeIf { it >= 1 }
    val paidValue: Int? get() = paid.toIntOrNull()?.takeIf { p -> p >= 0 && (totalValue?.let { p <= it } ?: true) }
    val statementText: String get() = (statement ?: name).trim()

    // The first error of step 1 (which plan), or null
    fun planError(): PlanError? = when {
        name.isBlank() -> PlanError.NAME
        walletId.isBlank() -> PlanError.ACCOUNT
        else -> null
    }

    // The first error of step 2 (the payments), or null
    fun paymentsError(): PlanError? = when {
        amountValue == null -> PlanError.AMOUNT
        totalValue == null -> PlanError.TOTAL
        paidValue == null -> PlanError.PAID
        dueDay !in 1..31 -> PlanError.DUE_DAY
        statementText.isBlank() -> PlanError.STATEMENT
        else -> null
    }

    fun error(): PlanError? = planError() ?: paymentsError()

    // The status is set again by the use case. It is here so the preview and the request agree.
    fun toNew(): NewInstallment {
        val total = totalValue ?: 1
        val paid = paidValue ?: 0
        return NewInstallment(
            merchant = name.trim(), merchantPattern = statementText, wallet = walletId,
            installmentAmount = amountValue ?: 0.0, totalInstallments = total, currentStep = paid,
            dueDay = dueDay, status = if (paid >= total) "completed" else "active",
        )
    }

    // The plan as the list will show it, or null while the numbers are not valid
    fun preview(): Installment? {
        if (paymentsError() != null) return null
        val n = toNew()
        return Installment(
            id = 0, merchant = n.merchant, merchantPattern = n.merchantPattern, wallet = n.wallet,
            installmentAmount = n.installmentAmount, totalInstallments = n.totalInstallments,
            currentStep = n.currentStep, dueDay = n.dueDay, status = n.status,
        )
    }

    // The last day of the plan, or null when every step is paid or the numbers are not valid
    fun estimatedFinish(): LocalDate? = preview()?.takeIf { !it.isCompleted }?.estimatedFinish()

    // True when another active plan on the account has the same statement text and amount
    fun collides(plans: List<Installment>): Boolean {
        val amount = amountValue ?: return false
        return InstallmentMatcher.collides(plans, walletId, statementText, amount)
    }

    // A tapped payment fills the amount and the statement text. A name the user typed stays.
    // A step at the end of the text moves to the total and to the steps already paid.
    fun withPayment(tx: TransactionUiModel): InstallmentPlanDraft {
        val split = splitStatementStep(tx.item)
        return copy(
            name = name.ifBlank { split.text }, statement = split.text,
            amount = tx.amount.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() },
            total = split.total?.toString() ?: total, paid = split.step?.toString() ?: paid,
        )
    }
}
