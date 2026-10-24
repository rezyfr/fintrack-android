package com.fidriyanto.banktracker.ui.settings.installmentplans

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallmentPlanDraftTest {
    private val ok = InstallmentPlanDraft(name = "Laptop", walletId = "CARD", amount = "2150000", total = "12", paid = "0", dueDay = 15)

    private fun existing(amount: Double = 2150000.0, pattern: String = "Laptop", status: String = "active", wallet: String = "CARD") =
        Installment(1, "Other", pattern, wallet, amount, 12, 1, 15, status)

    private fun tx(item: String, amount: Double = 2150000.0) =
        TransactionUiModel(9, item, "Other", amount, "2026-10-10", "CARD", "expense", TransactionStatus.SYNCED)

    @Test fun aFullDraftHasNoError() = assertNull(ok.error())

    @Test fun aBlankNameIsAnError() = assertEquals(PlanError.NAME, ok.copy(name = "  ").planError())

    @Test fun noAccountIsAnError() = assertEquals(PlanError.ACCOUNT, ok.copy(walletId = "").planError())

    @Test fun anAmountOfZeroOrNoTextIsAnError() {
        assertEquals(PlanError.AMOUNT, ok.copy(amount = "0").paymentsError())
        assertEquals(PlanError.AMOUNT, ok.copy(amount = "").paymentsError())
    }

    @Test fun totalStepsMustBeAtLeastOne() = assertEquals(PlanError.TOTAL, ok.copy(total = "0").paymentsError())

    @Test fun stepsPaidMustBeBetweenZeroAndTotal() {
        assertNull(ok.copy(paid = "12").paymentsError())
        assertEquals(PlanError.PAID, ok.copy(paid = "13").paymentsError())
        assertEquals(PlanError.PAID, ok.copy(paid = "").paymentsError())
    }

    @Test fun theDueDayMustBeFromOneTo31() {
        assertEquals(PlanError.DUE_DAY, ok.copy(dueDay = 0).paymentsError())
        assertEquals(PlanError.DUE_DAY, ok.copy(dueDay = 32).paymentsError())
        assertNull(ok.copy(dueDay = 31).paymentsError())
    }

    @Test fun theStatementTextDefaultsToTheName() {
        assertEquals("Laptop", ok.statementText)
        assertEquals("LAP", ok.copy(statement = " LAP ").statementText)
    }

    @Test fun aBlankStatementTextIsAnError() = assertEquals(PlanError.STATEMENT, ok.copy(statement = " ").paymentsError())

    @Test fun theDraftMapsToTheRequest() {
        val n = ok.copy(name = " Laptop ", statement = "LAPTOP STORE", paid = "3").toNew()
        assertEquals("Laptop", n.merchant)
        assertEquals("LAPTOP STORE", n.merchantPattern)
        assertEquals("CARD", n.wallet)
        assertEquals(2150000.0, n.installmentAmount, 0.0)
        assertEquals(12, n.totalInstallments)
        assertEquals(3, n.currentStep)
        assertEquals(15, n.dueDay)
        assertEquals("active", n.status)
    }

    @Test fun everyStepPaidMapsToCompleted() = assertEquals("completed", ok.copy(paid = "12").toNew().status)

    @Test fun noFinishDateWhenEveryStepIsPaid() {
        assertNull(ok.copy(paid = "12").estimatedFinish())
        assertEquals(ok.preview()!!.estimatedFinish(), ok.estimatedFinish())
    }

    @Test fun aSamePlanOnTheSameAccountIsADuplicate() {
        assertTrue(ok.collides(listOf(existing())))
    }

    @Test fun aDifferentAmountAccountOrStatusIsNotADuplicate() {
        assertFalse(ok.collides(listOf(existing(amount = 100.0))))
        assertFalse(ok.collides(listOf(existing(wallet = "OTHER"))))
        assertFalse(ok.collides(listOf(existing(status = "completed"))))
        assertFalse(ok.collides(listOf(existing(pattern = "PHONE"))))
    }

    @Test fun aPaymentFillsTheAmountAndTheStatementText() {
        val d = InstallmentPlanDraft(walletId = "CARD").withPayment(tx("SHOP 0%12 005/012"))
        assertEquals("SHOP 0%12", d.name)
        assertEquals("SHOP 0%12", d.statementText)
        assertEquals("2150000", d.amount)
        assertEquals("12", d.total)
        assertEquals("5", d.paid)
    }

    @Test fun aPaymentWithoutAStepKeepsTheTotalAndThePaidSteps() {
        val d = ok.copy(total = "6", paid = "2").withPayment(tx("SHOP", 99.5))
        assertEquals("Laptop", d.name)
        assertEquals("SHOP", d.statementText)
        assertEquals("99.5", d.amount)
        assertEquals("6", d.total)
        assertEquals("2", d.paid)
    }
}
