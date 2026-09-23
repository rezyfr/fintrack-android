package com.fidriyanto.banktracker.domain

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.usecase.BudgetAttribution
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetAttributionTest {

    private fun line(id: Long, name: String, currency: String, wallets: List<String>?, pattern: String?, cats: List<String>?, order: Int) =
        BudgetLine(id, name, "flex", currency, 0.0, wallets, pattern, cats, order)

    private fun tx(amount: Double, item: String, wallet: String, category: String = "Other", type: String = "expense", id: Long = 1) =
        TransactionUiModel(id, item, category, amount, "2026-09-10", wallet, type, TransactionStatus.SYNCED)

    @Test fun firstMatchWinsBySortOrder() {
        val lines = listOf(
            line(1, "Transport (Bangkok)", "THB", listOf("MANDIRI_CC"), "GRAB\\.COM|BOLT", listOf("Transport"), 0),
            line(2, "Transport (Indonesia)", "IDR", listOf("BCA", "MANDIRI_CC"), "Grab\\* A-", null, 1),
        )
        // A Bangkok Grab on the card -> THB line, converted to THB at 535
        val rows = listOf(tx(53500.0, "WWW.GRAB.COM BANGKOK TH", "MANDIRI_CC", "Transport"))
        val spend = BudgetAttribution.spendByLine(rows, lines, 535.0)
        assertEquals(100.0, spend[1]!!, 0.01)  // 53,500 / 535 = 100 THB
        assertEquals(null, spend[2])
    }

    @Test fun categoryMatchWhenNoPattern() {
        val lines = listOf(line(9, "Groceries (Indonesia)", "IDR", listOf("BCA"), null, listOf("Groceries"), 0))
        val rows = listOf(tx(250000.0, "Random shop", "BCA", "Groceries"))
        assertEquals(250000.0, BudgetAttribution.spendByLine(rows, lines, 535.0)[9]!!, 0.01)
    }

    @Test fun ignoresIncomeAndWalletMismatch() {
        val lines = listOf(line(1, "Food", "THB", listOf("BBL"), null, listOf("Food & Drink"), 0))
        val rows = listOf(
            tx(500.0, "Salary", "BBL", "Food & Drink", type = "income"),
            tx(300.0, "Kebab", "BCA", "Food & Drink"),   // wrong wallet
        )
        assertEquals(0, BudgetAttribution.spendByLine(rows, lines, 535.0).size)
    }

    @Test fun idrLineFromThbWalletConverts() {
        val lines = listOf(line(5, "Something IDR", "IDR", listOf("BBL"), "Kebab", null, 0))
        val rows = listOf(tx(100.0, "Kebab", "BBL"))  // 100 THB -> 53,500 IDR
        assertEquals(53500.0, BudgetAttribution.spendByLine(rows, lines, 535.0)[5]!!, 0.01)
    }
}
