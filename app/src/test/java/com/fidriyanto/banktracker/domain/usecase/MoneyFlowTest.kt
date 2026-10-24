package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFlowTest {
    private val zones = mapOf("BCA" to "IDR", "MANDIRI" to "IDR", "BBL" to "THB", "INVESTMENT" to "INV")
    private val zoneOf: (String?) -> String = { zones[it] ?: "IDR" }

    private fun tx(type: String, amount: Double, wallet: String, toWallet: String? = null, toAmount: Double? = null) =
        TransactionUiModel(
            id = 1, item = "x", category = "c", amount = amount, dateIso = "2026-09-01", wallet = wallet,
            txType = type, status = TransactionStatus.SYNCED, toWallet = toWallet, toAmount = toAmount,
        )

    @Test fun incomeAndExpenseCountInTheirOwnZone() {
        val rows = listOf(tx("income", 100.0, "BCA"), tx("expense", 30.0, "BCA"), tx("expense", 5.0, "BBL"))
        assertEquals(MoneyFlow(100.0, 30.0), moneyFlow(rows, "IDR", zoneOf))
        assertEquals(MoneyFlow(0.0, 5.0), moneyFlow(rows, "THB", zoneOf))
    }

    @Test fun transferInsideOneZoneIsIgnored() {
        val rows = listOf(tx("transfer", 50.0, "BCA", "MANDIRI"))
        assertEquals(MoneyFlow(0.0, 0.0), moneyFlow(rows, "IDR", zoneOf))
    }

    @Test fun crossCurrencyTransferIsMoneyOutOfSourceAndMoneyInOfDestinationAtTheReceivedAmount() {
        val rows = listOf(tx("transfer", 5350.0, "BCA", "BBL", toAmount = 10.0))
        assertEquals(MoneyFlow(0.0, 5350.0), moneyFlow(rows, "IDR", zoneOf))
        assertEquals(MoneyFlow(10.0, 0.0), moneyFlow(rows, "THB", zoneOf))
    }

    @Test fun movingCashIntoInvestmentsIsMoneyOut() {
        val rows = listOf(tx("transfer", 200.0, "BCA", "INVESTMENT"))
        assertEquals(MoneyFlow(0.0, 200.0), moneyFlow(rows, "IDR", zoneOf))
    }

    // ac: insights-wallet-flow — one wallet as its own zone counts every transfer into or out of it
    @Test fun singleWalletZoneCountsSameCurrencyTransfers() {
        val walletZoneOf: (String?) -> String = { if (it == "BCA") "BCA" else "OTHER" }
        val rows = listOf(
            tx("income", 100.0, "BCA"),
            tx("expense", 30.0, "BCA"),
            tx("transfer", 50.0, "BCA", "MANDIRI"),
            tx("transfer", 10.0, "BBL", "BCA", toAmount = 5350.0),
        )
        assertEquals(MoneyFlow(5450.0, 80.0), moneyFlow(rows, "BCA", walletZoneOf))
    }
}
