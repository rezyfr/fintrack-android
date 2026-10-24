package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.InstallmentDataSource
import com.fidriyanto.banktracker.data.datasource.PendingInstallmentStepDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentCreateDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentDto
import com.fidriyanto.banktracker.domain.model.NewInstallment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallmentRepositoryImplTest {
    private val source = mockk<InstallmentDataSource>()
    private val pending = mockk<PendingInstallmentStepDataSource>(relaxed = true)
    private val repo = InstallmentRepositoryImpl(source, pending)

    @Test fun createSendsEveryFieldAndReturnsThePlan() = runBlocking {
        val sent = slot<InstallmentCreateDto>()
        coEvery { source.create(capture(sent)) } returns Result.success(InstallmentDto(4, "Laptop", "LAP", "CARD", 100.0, 12, 3, 5, "active"))
        val plan = repo.create(NewInstallment("Laptop", "LAP", "CARD", 100.0, 12, 3, 5, "active")).getOrThrow()
        assertEquals(4L, plan.id)
        assertEquals("LAP", sent.captured.merchantPattern)
        assertEquals(3, sent.captured.currentStep)
        assertEquals(false, sent.captured.excluded)
    }

    @Test fun aDeletedPlanLosesItsWaitingStepUpdate() = runBlocking {
        coEvery { source.delete(4) } returns Result.success(Unit)
        repo.delete(4)
        coVerify { pending.delete(4) }
    }

    @Test fun aFailedDeleteKeepsTheWaitingStepUpdate() = runBlocking {
        coEvery { source.delete(4) } returns Result.failure(Exception("HTTP 500"))
        repo.delete(4)
        coVerify(exactly = 0) { pending.delete(any()) }
    }
}
