package com.fidriyanto.banktracker.categorization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleBasedMatcherTest {
    @Test fun `matches exact key`() {
        assertEquals("Transport", RuleBasedMatcher.match("BTS TIM TVM"))
    }
    @Test fun `matches partial key`() {
        assertEquals("Food & Drink", RuleBasedMatcher.match("TRUE MONEY"))
    }
    @Test fun `returns null for unknown`() {
        assertNull(RuleBasedMatcher.match("UNKNOWN MERCHANT XYZ"))
    }
    @Test fun `matches SUSHIRO`() {
        assertEquals("Food & Drink", RuleBasedMatcher.match("SUSHIRO"))
    }
    @Test fun `matches CASH ATM`() {
        assertEquals("Other", RuleBasedMatcher.match("CASH ATM WD"))
    }
}
