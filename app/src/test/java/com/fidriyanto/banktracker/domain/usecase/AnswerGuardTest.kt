package com.fidriyanto.banktracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerGuardTest {
    private val results = listOf("Spending by category from 2026-10-01 to 2026-10-13:\nIDR: Groceries Rp 1,137,850, Bills Rp 655,000\nTHB: Food & Drink ฿2,167.12")

    @Test fun numbers_have_no_separators_and_small_numbers_are_ignored() {
        assertEquals(setOf("1137850", "655000", "2026"), AnswerGuard.numbers("Rp 1,137,850 and Rp655,000 in 2026, top 3 of 10"))
        assertEquals(setOf("2167.12"), AnswerGuard.numbers("฿2,167.12"))
    }

    @Test fun amounts_copied_exactly_are_accepted() {
        assertTrue(AnswerGuard.accept("Your biggest are Groceries at Rp 1,137,850 and Bills at Rp 655,000.", results))
        assertTrue(AnswerGuard.accept("Food & Drink was ฿2,167.12 from 2026-10-01.", results))
        assertTrue(AnswerGuard.accept("You spent the most on groceries.", results))
    }

    @Test fun amounts_copied_wrongly_or_made_up_are_rejected() {
        assertFalse(AnswerGuard.accept("Groceries at Rp1,1,850, Bills at Rp6555,000.", results))
        assertFalse(AnswerGuard.accept("Groceries was Rp 1,137,000.", results))
    }
}
