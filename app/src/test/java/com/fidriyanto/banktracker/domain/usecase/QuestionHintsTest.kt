package com.fidriyanto.banktracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuestionHintsTest {
    @Test fun a_number_before_or_after_the_word_is_the_limit() {
        assertEquals(3, QuestionHints.limit("What are my 3 biggest expenses this month?"))
        assertEquals(5, QuestionHints.limit("show the top 5 shops"))
        assertEquals(10, QuestionHints.limit("my 10 most expensive purchases"))
        assertEquals(2, QuestionHints.limit("Largest 2 expenses last week"))
    }

    @Test fun no_number_gives_nothing() {
        assertNull(QuestionHints.limit("What was my biggest expense?"))
        assertNull(QuestionHints.limit("How much did I spend in the last 7 days?"))
        assertNull(QuestionHints.limit("top shops"))
    }
}
