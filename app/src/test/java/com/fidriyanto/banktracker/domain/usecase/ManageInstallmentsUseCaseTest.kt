package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.InstallmentRepository
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.model.NewInstallment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageInstallmentsUseCaseTest {
    private fun plan(step: Int, total: Int) = NewInstallment("Laptop", "LAPTOP", "CARD", 100.0, total, step, 5, "active")

    private fun created(n: NewInstallment) = Installment(1, n.merchant, n.merchantPattern, n.wallet, n.installmentAmount, n.totalInstallments, n.currentStep, n.dueDay, n.status)

    @Test fun aPlanWithEveryStepPaidIsCreatedAsCompleted() = runBlocking {
        val repo = mockk<InstallmentRepository>()
        val sent = slot<NewInstallment>()
        coEvery { repo.create(capture(sent)) } answers { Result.success(created(sent.captured)) }
        ManageInstallmentsUseCase(repo).create(plan(step = 12, total = 12))
        assertEquals("completed", sent.captured.status)
    }

    @Test fun aPlanWithStepsLeftIsCreatedAsActive() = runBlocking {
        val repo = mockk<InstallmentRepository>()
        val sent = slot<NewInstallment>()
        coEvery { repo.create(capture(sent)) } answers { Result.success(created(sent.captured)) }
        ManageInstallmentsUseCase(repo).create(plan(step = 0, total = 12).copy(status = "completed"))
        assertEquals("active", sent.captured.status)
    }

    @Test fun aFailedCreateIsReturnedAsFailure() = runBlocking {
        val repo = mockk<InstallmentRepository>()
        coEvery { repo.create(any()) } returns Result.failure(Exception("HTTP 500"))
        assertTrue(ManageInstallmentsUseCase(repo).create(plan(1, 12)).isFailure)
    }

    @Test fun deleteCallsTheRepository() = runBlocking {
        val repo = mockk<InstallmentRepository>()
        coEvery { repo.delete(7) } returns Result.success(Unit)
        assertTrue(ManageInstallmentsUseCase(repo).delete(7).isSuccess)
        coVerify(exactly = 1) { repo.delete(7) }
    }
}
