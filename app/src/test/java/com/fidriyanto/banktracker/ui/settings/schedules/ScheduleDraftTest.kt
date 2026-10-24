package com.fidriyanto.banktracker.ui.settings.schedules

import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.model.TRANSFER_CATEGORY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleDraftTest {
    private val base = ScheduleDraft(amount = "100", walletId = "A", category = "Salary", name = "Pay")

    @Test fun aTransferUsesTheTransferCategory() {
        val s = base.copy(txType = "transfer", toWalletId = "B", category = "").toSchedule("1")
        assertEquals(TRANSFER_CATEGORY, s.category)
        assertEquals("B", s.toWalletId)
    }

    @Test fun anIncomeKeepsItsCategoryAndHasNoTarget() {
        val s = base.copy(txType = "income", toWalletId = "B").toSchedule("1")
        assertEquals("Salary", s.category)
        assertNull(s.toWalletId)
    }

    @Test fun anExpenseNeedsACategoryAndATransferDoesNot() {
        assertEquals(WhatError.CATEGORY, base.copy(category = "").whatError())
        assertNull(base.copy(txType = "transfer", category = "", toWalletId = "B").whatError())
    }

    private val plan = Installment(
        id = 7, merchant = "Laptop plan", wallet = "CARD", installmentAmount = 2150000.0,
        totalInstallments = 6, currentStep = 2, dueDay = 22, status = "active",
    )

    @Test fun pickingAPlanFillsTheScheduleFromIt() {
        val d = ScheduleDraft().withPlan(plan)
        assertEquals(7L, d.installmentId)
        assertEquals("CARD", d.walletId)
        assertEquals("2150000", d.amount)
        assertEquals("Laptop plan", d.name)
        assertEquals(22, d.every)
        assertEquals(plan.nextDueDate(), d.startDate)
        assertEquals(plan.estimatedFinish(), d.endDate)
        assertEquals(true, d.endOn)
    }

    @Test fun aNameTheUserTypedStaysWhenAPlanIsPicked() {
        assertEquals("Work laptop", ScheduleDraft(name = "Work laptop").withPlan(plan).name)
    }

    @Test fun theScheduleEndsOnTheDateOfTheLastStep() {
        val dates = ScheduleDraft().withPlan(plan).copy(category = "Shopping").toSchedule("1")
            .let { com.fidriyanto.banktracker.domain.model.ScheduleDates.between(it, plan.nextDueDate(), plan.nextDueDate().plusYears(2)) }
        assertEquals(plan.remaining, dates.size)
    }

    @Test fun onlyAnExpenseKeepsThePlanLink() {
        assertEquals(7L, base.copy(installmentId = 7).toSchedule("1").installmentId)
        assertNull(base.copy(txType = "income", installmentId = 7).toSchedule("1").installmentId)
    }
}
