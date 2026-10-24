package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.db.WalletNet
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerBalancesTest {
    @Test fun aBalanceIsTheOpeningPlusItsOwnFlowPlusWhatItReceived() {
        val balances = ledgerBalances(
            openingBalances = mapOf("A" to 100.0, "B" to 0.0, "C" to 5.0),
            ownFlow = listOf(WalletNet("A", -30.0), WalletNet("B", 10.0), WalletNet(null, 99.0)),
            incoming = listOf(WalletNet("B", 25.0), WalletNet("X", 7.0)),
        )
        assertEquals(mapOf("A" to 70.0, "B" to 35.0, "C" to 5.0), balances)
    }
}
