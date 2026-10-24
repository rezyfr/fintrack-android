package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.CardBillingRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import com.fidriyanto.banktracker.domain.model.CardStatement
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import java.time.LocalDate
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
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    // ac: card-statement-fifo-minimum-rollover — one statement per card from the period's raw transactions.
    suspend fun getStatements(): Result<List<CardStatement>> = runCatching {
        val billings = billingRepository.getAll().getOrThrow()
        val balances = walletBalanceRepository.getBalances().getOrElse { emptyList() }
            .associate { it.id to it.balance }
        val today = LocalDate.now()
        // Fetch every wallet's rows once, because a card payment lives on the source wallet. The end date
        // a year ahead makes the fetch read every page and keeps future-dated rows (complete-range-fetch).
        val all = transactionRepository.fetch(null, null, null, dateFrom = today.minusMonths(8).format(iso), dateTo = today.plusYears(1).format(iso))
            .getOrElse { emptyList() }

        billings.map { b ->
            val cutoff = CardBillingMath.latestCutoff(b.cutoffDay)
            val cutoffIso = cutoff.format(iso)
            val prevIso = CardBillingMath.latestCutoff(b.cutoffDay, cutoff.minusDays(1)).format(iso)

            // ac: card-due-reflects-payments — anchor to the live balance and subtract debt added after
            // the cutoff (charges + cash-advance transfers-out); payments settle statement debt first.
            val charges = all.filter { it.wallet == b.wallet && it.txType == "expense" }
            val advances = all.filter { it.wallet == b.wallet && it.txType == "transfer" }
            val payments = all.filter { it.toWallet == b.wallet && it.txType == "transfer" }
            val credits = all.filter { it.wallet == b.wallet && it.txType == "income" }
            fun gt(list: List<TransactionUiModel>, isoDate: String) = list.filter { it.dateIso > isoDate }.sumOf { it.amount }
            val liveBalance = abs(balances[b.wallet] ?: 0.0)
            val postCutoffDebt = gt(charges, cutoffIso) + gt(advances, cutoffIso)
            val statementRemaining = (liveBalance - postCutoffDebt).coerceAtLeast(0.0)
            val payAfter = gt(payments, cutoffIso) + gt(credits, cutoffIso)
            // Balance at the close = remaining now plus what has since been paid toward it.
            val closeBalance = statementRemaining + payAfter
            val installment = CardBillingMath.installmentPortion(charges.filter { it.dateIso <= cutoffIso }, prevIso, cutoffIso)
            val requiredMin = CardBillingMath.minimumPayment(closeBalance, installment, b.minPercent, b.minFullInstallments)
            val minRemaining = (requiredMin - payAfter).coerceAtLeast(0.0)
            val due = CardBillingMath.dueDateAfter(cutoff, b.dueDay)

            // ac: card-statement-fifo-minimum-rollover — a settled, past-due statement rolls to the next.
            if (due.isBefore(today) && statementRemaining <= 1.0) {
                val nextCutoff = cutoff.plusMonths(1)
                val nextDue = CardBillingMath.dueDateAfter(nextCutoff, b.dueDay)
                CardStatement(
                    wallet = b.wallet, cutoffIso = nextCutoff.format(iso), dueIso = nextDue.format(iso),
                    balance = liveBalance, installment = 0.0, minimum = liveBalance,
                    minimumRemaining = 0.0, minimumPaid = false, overdue = false, upcoming = true,
                    daysUntilDue = CardBillingMath.daysUntil(nextDue),
                )
            } else {
                CardStatement(
                    wallet = b.wallet, cutoffIso = cutoffIso, dueIso = due.format(iso),
                    balance = statementRemaining, installment = installment, minimum = requiredMin,
                    minimumRemaining = minRemaining,
                    minimumPaid = requiredMin > 0.0 && minRemaining <= 1.0,
                    overdue = due.isBefore(today) && statementRemaining > 1.0,
                    upcoming = false,
                    daysUntilDue = CardBillingMath.daysUntil(due),
                )
            }
        }
    }
}
