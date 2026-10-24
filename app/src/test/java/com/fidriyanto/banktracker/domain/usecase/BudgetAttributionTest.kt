package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// ac: budget-by-category
class BudgetAttributionTest {
    private val wallets = mapOf("BBL" to "THB", "BCA" to "IDR", "MANDIRI_CC" to "IDR")

    private fun line(id: Long, currency: String, category: String?, subcategory: String? = null, pattern: String? = null) =
        BudgetLine(id, "line$id", "flex", currency, 1000.0, null, pattern, null, id.toInt(), category, subcategory)

    private fun tx(category: String, wallet: String, subcategory: String? = null, item: String = "x", budgetLineId: Long? = null) =
        TransactionUiModel(
            id = 1, item = item, category = category, amount = 10.0, dateIso = "2026-10-01", wallet = wallet,
            txType = "expense", status = TransactionStatus.SYNCED, subcategory = subcategory, budgetLineId = budgetLineId,
        )

    private val lines = listOf(
        line(1, "THB", "Bills", "Rent"),
        line(2, "THB", "Bills"),
        line(3, "THB", "Groceries"),
        line(4, "IDR", "Groceries"),
    )

    private fun lineOf(t: TransactionUiModel) = BudgetAttribution.lineFor(t, lines, wallets, "IDR")?.id

    @Test fun aSubcategoryLineWinsOverTheCategoryLine() {
        assertEquals(1L, lineOf(tx("Bills", "BBL", "Rent")))
        assertEquals(2L, lineOf(tx("Bills", "BBL", "Internet")))
        assertEquals(2L, lineOf(tx("Bills", "BBL")))
    }

    @Test fun theWalletCurrencyPicksTheBook() {
        assertEquals(3L, lineOf(tx("Groceries", "BBL")))
        // Bangkok spending on a rupiah card counts in the rupiah book
        assertEquals(4L, lineOf(tx("Groceries", "MANDIRI_CC")))
    }

    @Test fun aCategoryWithNoLineHasNoBudget() = assertNull(lineOf(tx("Other", "BBL")))

    @Test fun aManualAssignmentNoLongerOverridesACategoryLine() =
        assertEquals(3L, lineOf(tx("Groceries", "BBL", budgetLineId = 2)))

    @Test fun linesWithoutACategoryStillUseTheirRules() {
        val ruleLines = listOf(line(9, "THB", null, pattern = "MARKET"))
        assertEquals(9L, BudgetAttribution.lineFor(tx("Other", "BBL", item = "MARKET"), ruleLines, wallets, "IDR")?.id)
    }
}
