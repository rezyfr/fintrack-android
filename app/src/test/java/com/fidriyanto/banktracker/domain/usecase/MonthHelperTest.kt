package com.fidriyanto.banktracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

// ac: insights-cycle-stepper — the trend shows the six pay cycles that end with the selected one
class MonthHelperTest {
    @Test
    fun `trend cycles end with the cycle in progress at offset 0`() {
        val cycles = trendCycles(0)
        assertEquals(6, cycles.size)
        assertEquals(PayCycle.cycleRange(5).fromIso, cycles.first().fromIso)
        assertEquals(PayCycle.current().toIso, cycles.last().toIso)
    }

    @Test
    fun `trend cycles end with the selected cycle at a later offset`() {
        val cycles = trendCycles(2)
        assertEquals(PayCycle.cycleRange(7).fromIso, cycles.first().fromIso)
        assertEquals(PayCycle.cycleRange(2).toIso, cycles.last().toIso)
    }
}
