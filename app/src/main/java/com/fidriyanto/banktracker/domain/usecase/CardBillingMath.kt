package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import java.time.LocalDate
import kotlin.math.roundToLong

// Pure statement-cycle math, mirroring web/src/utils/cardBilling.js so both platforms agree.
object CardBillingMath {

    // ac: android-card-statement-view — the statement closes on the most recent cutoff day
    fun latestCutoff(cutoffDay: Int, today: LocalDate = LocalDate.now()): LocalDate {
        val day = cutoffDay.coerceAtMost(today.lengthOfMonth())
        val thisMonth = today.withDayOfMonth(day)
        return if (thisMonth.isAfter(today)) {
            val prev = today.minusMonths(1)
            prev.withDayOfMonth(cutoffDay.coerceAtMost(prev.lengthOfMonth()))
        } else thisMonth
    }

    // ac: android-card-statement-view — the due date is the due day in the first month after the cutoff
    fun dueDateAfter(cutoff: LocalDate, dueDay: Int): LocalDate {
        var due = cutoff.withDayOfMonth(dueDay.coerceAtMost(cutoff.lengthOfMonth()))
        while (!due.isAfter(cutoff)) {
            val next = due.plusMonths(1)
            due = next.withDayOfMonth(dueDay.coerceAtMost(next.lengthOfMonth()))
        }
        return due
    }

    private val installmentRegex = Regex("""\b\d{2,3}/\d{2,3}\b|cicilan""", RegexOption.IGNORE_CASE)

    // ac: android-card-statement-view — installment charges billed inside this statement window
    fun installmentPortion(rows: List<TransactionUiModel>, fromExclusive: String, toInclusive: String): Double =
        rows.filter {
            it.txType == "expense" && it.dateIso > fromExclusive && it.dateIso <= toInclusive &&
                installmentRegex.containsMatchIn(it.item)
        }.sumOf { it.amount }

    // ac: android-card-statement-view — percentage of the balance, plus installments in full when the
    // card bills them that way
    fun minimumPayment(balance: Double, installment: Double, minPercent: Double, minFullInstallments: Boolean): Double {
        val pct = minPercent / 100.0
        return if (minFullInstallments) {
            val revolving = (balance - installment).coerceAtLeast(0.0)
            (installment + revolving * pct).coerceAtMost(balance)
        } else balance * pct
    }

    // Unpaid balance as of a cutoff, FIFO: a payment or credit settles the oldest charges first and
    // can only settle charges dated on or before its own date.
    fun balanceAsOf(rows: List<TransactionUiModel>, wallet: String, cutoffIso: String): Double {
        val events = rows
            .filter { it.dateIso <= cutoffIso }
            .filter { it.txType == "expense" || it.txType == "income" || (it.txType == "transfer" && it.wallet != wallet) }
            .sortedWith(compareBy({ it.dateIso }, { it.id }))
        val charges = ArrayList<DoubleArray>() // [remaining]
        for (ev in events) {
            if (ev.txType == "expense") {
                charges.add(doubleArrayOf(ev.amount))
            } else {
                var pool = ev.amount
                for (c in charges) {
                    if (pool <= 0) break
                    if (c[0] <= 0) continue
                    val applied = minOf(pool, c[0])
                    c[0] -= applied; pool -= applied
                }
            }
        }
        return charges.sumOf { it[0] }
    }

    fun daysUntil(due: LocalDate, today: LocalDate = LocalDate.now()): Int =
        (due.toEpochDay() - today.toEpochDay()).toInt()

    fun round(v: Double): Long = v.roundToLong()
}
