package com.fidriyanto.banktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstallmentMatcherTest {
    private fun plan(
        id: Long,
        pattern: String,
        amount: Double,
        total: Int,
        step: Int,
        wallet: String = "MANDIRI_CC",
        status: String = "active",
    ) = Installment(
        id = id, merchant = "m$id", merchantPattern = pattern, wallet = wallet, installmentAmount = amount,
        totalInstallments = total, currentStep = step, dueDay = 1, status = status,
    )

    private val tiketCom = plan(7, "Tiket.com", 25920.0, 12, 8)
    private val bcaTicketA = plan(11, "TIKET.COM 0%3", 1081060.0, 3, 2, wallet = "BCA_CC")
    private val bcaTicketB = plan(12, "TIKET.COM 0%3", 1713594.0, 3, 2, wallet = "BCA_CC")

    @Test fun stepComesFromTheStatementText() {
        val m = InstallmentMatcher.match("TIKET.COM J 009/012", "MANDIRI_CC", 25920.0, listOf(tiketCom))!!
        assertEquals(9, m.newStep)
        assertEquals("active", m.newStatus)
    }

    @Test fun keDariFormatIsRead() {
        val m = InstallmentMatcher.match("CICILAN BCA KE 03 DARI 03, TIKET.COM 0%3", "BCA_CC", 1081060.0, listOf(bcaTicketA, bcaTicketB))!!
        assertEquals(11L, m.installment.id)
        assertEquals(3, m.newStep)
        assertEquals("completed", m.newStatus)
    }

    @Test fun theAmountPicksBetweenTwoPlansWithTheSamePattern() {
        val m = InstallmentMatcher.match("CICILAN BCA KE 03 DARI 03, TIKET.COM 0%3", "BCA_CC", 1713594.0, listOf(bcaTicketA, bcaTicketB))!!
        assertEquals(12L, m.installment.id)
    }

    @Test fun aStepAlreadyFixedByHandIsNeverLowered() {
        val fixed = tiketCom.copy(currentStep = 9)
        val m = InstallmentMatcher.match("TIKET.COM J 009/012", "MANDIRI_CC", 25920.0, listOf(fixed))!!
        assertEquals(9, m.newStep)
    }

    @Test fun withoutANumberTheStepMovesUpByOne() {
        val m = InstallmentMatcher.match("Tiket.com", "MANDIRI_CC", 25920.0, listOf(tiketCom))!!
        assertEquals(9, m.newStep)
    }

    @Test fun thePatternMatchIgnoresCase() {
        assertEquals(7L, InstallmentMatcher.match("tiket.COM j 009/012", "MANDIRI_CC", 25920.0, listOf(tiketCom))?.installment?.id)
    }

    @Test fun aDifferentWalletDoesNotMatch() = assertNull(InstallmentMatcher.match("TIKET.COM J 009/012", "BCA_CC", 25920.0, listOf(tiketCom)))

    @Test fun aDifferentAmountDoesNotMatch() = assertNull(InstallmentMatcher.match("TIKET.COM J 009/012", "MANDIRI_CC", 99999.0, listOf(tiketCom)))

    @Test fun aCompletedPlanIsIgnored() {
        val done = tiketCom.copy(status = "completed", currentStep = 12)
        assertNull(InstallmentMatcher.match("TIKET.COM J 012/012", "MANDIRI_CC", 25920.0, listOf(done)))
    }

    @Test fun twoMatchingPlansMeanNoLink() {
        val twin = tiketCom.copy(id = 99)
        assertNull(InstallmentMatcher.match("TIKET.COM J 009/012", "MANDIRI_CC", 25920.0, listOf(tiketCom, twin)))
    }

    @Test fun aStepForADifferentPlanLengthMeansNoLink() =
        assertNull(InstallmentMatcher.match("TIKET.COM J 003/006", "MANDIRI_CC", 25920.0, listOf(tiketCom)))

    @Test fun aDateInTheTextIsNotAStep() {
        assertEquals(emptyList<Pair<Int, Int>>(), InstallmentMatcher.parseSteps("PAYMENT 25/12"))
    }

    @Test fun theLastStepCompletesThePlan() {
        val last = tiketCom.copy(currentStep = 11)
        val m = InstallmentMatcher.match("TIKET.COM J 012/012", "MANDIRI_CC", 25920.0, listOf(last))!!
        assertEquals(12, m.newStep)
        assertEquals("completed", m.newStatus)
    }

    @Test fun aPlanPickedByHandFallsBackToOneStepUpWhenTheTextHasAnotherPlanLength() {
        val m = InstallmentMatcher.advance(tiketCom, "TIKET.COM J 003/006", strict = false)!!
        assertEquals(9, m.newStep)
    }

    @Test fun aStrictAdvanceRejectsAnotherPlanLength() = assertNull(InstallmentMatcher.advance(tiketCom, "TIKET.COM J 003/006"))
}
