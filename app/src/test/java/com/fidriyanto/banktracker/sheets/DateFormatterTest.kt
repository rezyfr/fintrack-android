package com.fidriyanto.banktracker.sheets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateFormatterTest {
    @Test fun `toExpensesDate formats without leading zeros`() {
        assertEquals("5/3/2026", DateFormatter.toExpensesDate(LocalDate.of(2026, 3, 5)))
    }
    @Test fun `toExpensesDate day 11`() {
        assertEquals("11/5/2026", DateFormatter.toExpensesDate(LocalDate.of(2026, 5, 11)))
    }
    @Test fun `toIncomeDate has leading zeros`() {
        assertEquals("26/01/2026", DateFormatter.toIncomeDate(LocalDate.of(2026, 1, 26)))
    }
    @Test fun `parseEmailDate parses standard format`() {
        assertEquals(LocalDate.of(2026, 5, 10), DateFormatter.parseEmailDate("10 May 2026 at 17:52:34 (Thailand time)"))
    }
    @Test fun `parseEmailDate returns null on garbage`() {
        assertNull(DateFormatter.parseEmailDate("not a date"))
    }
    @Test fun `parseEmailDate parses single digit day`() {
        assertEquals(LocalDate.of(2026, 5, 9), DateFormatter.parseEmailDate("9 May 2026 at 14:49:06 (Thailand time)"))
    }
}
