package com.fidriyanto.banktracker.domain.usecase

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class PayCycleTest {
    @Before @After fun legacyStartDay() {
        PayCycle.startDay = 26
    }

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

    @Test
    fun `start day 1 gives calendar months`() {
        PayCycle.startDay = 1
        val c = PayCycle.current(LocalDate.of(2026, 10, 11))
        assertEquals("2026-10-01", c.fromIso)
        assertEquals("2026-10-31", c.toIso)
        val previous = PayCycle.cycleRange(1, LocalDate.of(2026, 10, 11))
        assertEquals("2026-09-01", previous.fromIso)
        assertEquals("2026-09-30", previous.toIso)
    }

    @Test
    fun `start day 15 rolls over on the 15th`() {
        PayCycle.startDay = 15
        val before = PayCycle.current(LocalDate.of(2026, 10, 14))
        assertEquals("2026-09-15", before.fromIso)
        assertEquals("2026-10-14", before.toIso)
        val on = PayCycle.current(LocalDate.of(2026, 10, 15))
        assertEquals("2026-10-15", on.fromIso)
        assertEquals("2026-11-14", on.toIso)
        assertEquals(4, PayCycle.daysToPayday(LocalDate.of(2026, 10, 11)))
    }
}
