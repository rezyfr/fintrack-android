package com.fidriyanto.banktracker.domain

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.usecase.CardBillingMath
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CardBillingMathTest {

    private fun tx(date: String, amount: Double, item: String = "x", type: String = "expense", wallet: String = "BCA_CC", id: Long = 1) =
        TransactionUiModel(id, item, "Other", amount, date, wallet, type, TransactionStatus.SYNCED)

    @Test fun latestCutoff_usesThisMonthOncePassed() {
        assertEquals(LocalDate.of(2026, 9, 3), CardBillingMath.latestCutoff(3, LocalDate.of(2026, 9, 23)))
    }

    @Test fun latestCutoff_usesLastMonthWhenAhead() {
        assertEquals(LocalDate.of(2026, 8, 11), CardBillingMath.latestCutoff(11, LocalDate.of(2026, 9, 5)))
    }

    // due day after cutoff, same month
    @Test fun dueDate_sameMonth() {
        assertEquals(LocalDate.of(2026, 10, 19), CardBillingMath.dueDateAfter(LocalDate.of(2026, 10, 3), 19))
    }

    // due day on/before cutoff rolls to next month
    @Test fun dueDate_nextMonth() {
        assertEquals(LocalDate.of(2026, 10, 1), CardBillingMath.dueDateAfter(LocalDate.of(2026, 9, 11), 1))
    }

    @Test fun installmentPortion_sumsOnlyTaggedInWindow() {
        val rows = listOf(
            tx("2026-09-25", 1081060.0, "CICILAN BCA KE 03 DARI 03"),
            tx("2026-09-25", 25920.0, "TIKET.COM J 010/012"),
            tx("2026-09-20", 500000.0, "Naga groceries"),
            tx("2026-08-30", 999.0, "CICILAN prior window"),
        )
        assertEquals(1081060.0 + 25920.0, CardBillingMath.installmentPortion(rows, "2026-09-03", "2026-10-03"), 0.01)
    }

    // Verified against the Sep BCA statement: bill 3,825,917, installment 2,794,654 -> minimum 2,846,217
    @Test fun minimum_bca_installmentPlusFivePercent() {
        val m = CardBillingMath.minimumPayment(3825917.0, 2794654.0, 5.0, true)
        assertEquals(2846217L, CardBillingMath.round(m))
    }

    // Verified: Mandiri bill 20,953,446 -> minimum 1,047,672 (statement rounds to 1,047,680)
    @Test fun minimum_mandiri_plainFivePercent() {
        val m = CardBillingMath.minimumPayment(20953446.0, 0.0, 5.0, false)
        assertEquals(1047672L, CardBillingMath.round(m))
    }

    @Test fun minimum_neverExceedsBalance() {
        assertEquals(100000.0, CardBillingMath.minimumPayment(100000.0, 100000.0, 5.0, true), 0.01)
    }

    @Test fun balanceAsOf_fifoAndCutoff() {
        val rows = listOf(
            tx("2026-06-01", 100000.0, "Charge", "expense", id = 1),
            tx("2026-06-15", 40000.0, "Payment", "income", id = 2),
            tx("2026-07-20", 30000.0, "Later", "expense", id = 3),
        )
        // as of 30 Jun: 100k charge minus 40k payment = 60k; the July charge is excluded
        assertEquals(60000.0, CardBillingMath.balanceAsOf(rows, "BCA_CC", "2026-06-30"), 0.01)
    }
}
