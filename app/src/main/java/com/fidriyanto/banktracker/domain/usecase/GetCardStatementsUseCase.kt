package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.CardBillingRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import com.fidriyanto.banktracker.domain.model.CardStatement
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class GetCardStatementsUseCase @Inject constructor(
    private val billingRepository: CardBillingRepository,
    private val walletBalanceRepository: WalletBalanceRepository,
    private val transactionRepository: TransactionRepository,
) {
    // ac: android-card-statement-view — one statement summary per stored card_billing record.
    // The outstanding balance is the synced wallet balance (payments are already netted there);
    // the installment portion for the minimum comes from this statement's own charges.
    suspend fun getStatements(): Result<List<CardStatement>> = runCatching {
        val billings = billingRepository.getAll().getOrThrow()
        val balances = walletBalanceRepository.getBalances().getOrElse { emptyList() }
            .associate { it.id to it.balance }
        billings.map { b ->
            val cutoff = CardBillingMath.latestCutoff(b.cutoffDay)
            val cutoffIso = cutoff.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val prevIso = CardBillingMath.latestCutoff(b.cutoffDay, cutoff.minusDays(1))
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
            // ac: card-due-reflects-payments — the live synced balance nets all payments but also
            // includes charges made after the cutoff (next statement). Subtract those so the amount
            // shown is what is still owed on the current statement, which drops as payments land.
            val liveBalance = abs(balances[b.wallet] ?: 0.0)
            val allRows = transactionRepository
                .fetch(month = null, wallet = b.wallet, txType = null)
                .getOrElse { emptyList() }
            val postCutoffCharges = allRows
                .filter { it.txType == "expense" && it.dateIso > cutoffIso }
                .sumOf { it.amount }
            val balance = (liveBalance - postCutoffCharges).coerceAtLeast(0.0)
            val rows = allRows.filter { it.dateIso <= cutoffIso }
            val installment = CardBillingMath.installmentPortion(rows, prevIso, cutoffIso)
            val minimum = CardBillingMath.minimumPayment(balance, installment, b.minPercent, b.minFullInstallments)
            val due = CardBillingMath.dueDateAfter(cutoff, b.dueDay)
            CardStatement(
                wallet = b.wallet,
                cutoffIso = cutoffIso,
                dueIso = due.format(DateTimeFormatter.ISO_LOCAL_DATE),
                balance = balance,
                installment = installment,
                minimum = minimum,
                daysUntilDue = CardBillingMath.daysUntil(due),
            )
        }
    }
}
