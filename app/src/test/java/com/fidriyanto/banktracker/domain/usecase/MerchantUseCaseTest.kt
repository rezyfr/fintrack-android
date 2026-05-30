package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.MerchantHistoryRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantUseCaseTest {

    private val repo = mockk<MerchantHistoryRepository>(relaxed = true)

    @Test
    fun `GetRecentMerchantsUseCase returns flow from repository`() = runTest {
        every { repo.observe() } returns flowOf(listOf("Grab", "LINE MAN"))
        val result = GetRecentMerchantsUseCase(repo)().first()
        assertEquals(listOf("Grab", "LINE MAN"), result)
    }

    @Test
    fun `SaveMerchantUseCase delegates to repository`() = runTest {
        SaveMerchantUseCase(repo)("Grab")
        coVerify { repo.save("Grab") }
    }
}
