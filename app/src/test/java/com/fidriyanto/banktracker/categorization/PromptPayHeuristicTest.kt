package com.fidriyanto.banktracker.categorization

import org.junit.Assert.*
import org.junit.Test

class PromptPayHeuristicTest {
    @Test fun `matches PromptPay over threshold`() {
        assertTrue(PromptPayHeuristic.isLikelyTransferOut("PromptPay", 30000.0, 25000.0))
    }
    @Test fun `matches PromptPay exactly at threshold`() {
        assertTrue(PromptPayHeuristic.isLikelyTransferOut("PromptPay", 25000.0, 25000.0))
    }
    @Test fun `does not match small PromptPay`() {
        assertFalse(PromptPayHeuristic.isLikelyTransferOut("PromptPay", 226.0, 25000.0))
    }
    @Test fun `does not match non-PromptPay channel`() {
        assertFalse(PromptPayHeuristic.isLikelyTransferOut("BillPayment", 50000.0, 25000.0))
    }
}
