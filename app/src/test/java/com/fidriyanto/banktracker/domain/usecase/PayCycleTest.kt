package com.fidriyanto.banktracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PayCycleTest {

    @Test
    fun `payday day itself counts as zero days to payday`() {
        assertEquals(0, PayCycle.daysToPayday(LocalDate.of(2026, 9, 26)))
    }

    @Test
    fun `day before payday is one day out`() {
        assertEquals(1, PayCycle.daysToPayday(LocalDate.of(2026, 9, 25)))
    }

    @Test
    fun `day after payday counts to next month`() {
        assertEquals(29, PayCycle.daysToPayday(LocalDate.of(2026, 9, 27)))
    }

    @Test
    fun `on the payday the cycle rolls to the new one`() {
        val c = PayCycle.current(LocalDate.of(2026, 9, 26))
        assertEquals("2026-09-26", c.fromIso)
        assertEquals("2026-10-25", c.toIso)
    }
}
