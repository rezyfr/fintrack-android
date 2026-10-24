package com.fidriyanto.banktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatementTextTest {
    @Test fun aSlashStepAtTheEndIsCutOff() {
        assertEquals(StatementText("TIKET.COM 0%3", 5, 12), splitStatementStep("TIKET.COM 0%3 005/012"))
    }

    @Test fun aKeStepAtTheEndIsCutOff() {
        assertEquals(StatementText("SHOPEE", 30, 36), splitStatementStep("SHOPEE ke-30/36"))
    }

    @Test fun aKeDariStepAtTheEndIsCutOff() {
        assertEquals(StatementText("LAPTOP", 3, 6), splitStatementStep("LAPTOP KE 03 DARI 06"))
    }

    @Test fun textWithoutAStepStaysTheSame() {
        assertEquals(StatementText("COFFEE SHOP"), splitStatementStep("  COFFEE SHOP "))
    }

    @Test fun aStepAboveItsTotalIsADateAndStays() {
        val s = splitStatementStep("PAYMENT 25/12")
        assertEquals("PAYMENT 25/12", s.text)
        assertNull(s.step)
    }

    @Test fun aStepInTheMiddleStays() {
        val s = splitStatementStep("PLAN 003/012 STORE")
        assertEquals("PLAN 003/012 STORE", s.text)
        assertNull(s.step)
    }

    @Test fun aLineThatIsOnlyAStepStays() {
        assertNull(splitStatementStep("005/012").step)
    }
}
