package com.fidriyanto.banktracker.data.datasource.offline

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerTransactionQueryTest {
    private fun query(
        month: String? = null, wallet: String? = null, txType: String? = null, category: String? = null,
        search: String? = null, amountMin: Double? = null, amountMax: Double? = null, dateFrom: String? = null,
        dateTo: String? = null, involvingWallet: String? = null, limit: Int = 200,
    ) = ledgerTransactionQuery(month, wallet, txType, category, search, amountMin, amountMax, dateFrom, dateTo, involvingWallet, limit)

    @Test fun noFilterReadsTheNewestRows() {
        val q = query(limit = 5)
        assertEquals("SELECT * FROM ledger_transactions ORDER BY date DESC, id DESC LIMIT ?", q.sql)
        assertEquals(listOf<Any>(5), q.args)
    }

    @Test fun theFiltersCombineWithAnd() {
        val q = query(month = "2026-10", wallet = "A", txType = "expense", category = "Food", amountMin = 1.0, amountMax = 9.0)
        assertEquals(
            "SELECT * FROM ledger_transactions WHERE substr(date, 1, 7) = ? AND wallet = ? AND txType = ? AND category = ? " +
                "AND amount >= ? AND amount <= ? ORDER BY date DESC, id DESC LIMIT ?",
            q.sql,
        )
        assertEquals(listOf<Any>("2026-10", "A", "expense", "Food", 1.0, 9.0, 200), q.args)
    }

    @Test fun aDateRangeReplacesTheMonth() {
        val q = query(month = "2026-10", dateFrom = "2026-09-26", dateTo = "2026-10-25")
        assertEquals("SELECT * FROM ledger_transactions WHERE date >= ? AND date <= ? ORDER BY date DESC, id DESC LIMIT ?", q.sql)
        assertEquals(listOf<Any>("2026-09-26", "2026-10-25", 200), q.args)
    }

    @Test fun anAccountMatchesBothSidesOfATransfer() {
        val q = query(involvingWallet = "B")
        assertEquals("SELECT * FROM ledger_transactions WHERE (wallet = ? OR toWallet = ?) ORDER BY date DESC, id DESC LIMIT ?", q.sql)
        assertEquals(listOf<Any>("B", "B", 200), q.args)
    }

    @Test fun theSearchTreatsWildcardsAsPlainText() {
        val q = query(search = "50%_off\\")
        assertEquals(listOf<Any>("%50\\%\\_off\\\\%", 200), q.args)
        assertEquals("SELECT * FROM ledger_transactions WHERE item LIKE ? ESCAPE '\\' ORDER BY date DESC, id DESC LIMIT ?", q.sql)
    }

    @Test fun anEmptySearchIsNoFilter() = assertEquals(listOf<Any>(200), query(search = "").args)
}
