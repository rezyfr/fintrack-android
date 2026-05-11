package com.fidriyanto.banktracker.categorization

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CategoryResolverTest {
    private val claude = mockk<ClaudeCategorizor>()
    private val resolver = CategoryResolver(claude)
    private fun tx(merchant: String, amount: Double = 100.0, channel: String = "BillPayment") =
        ParsedTransaction(merchant, amount, LocalDate.now(), channel, "REF123")

    @Test fun `uses rule match for known merchant`() = runTest {
        val (category, _) = resolver.resolve(tx("BTS TIM TVM"), 25000.0, "apikey")
        assertEquals("Transport", category)
        coVerify(exactly = 0) { claude.categorize(any(), any(), any(), any()) }
    }

    @Test fun `uses PromptPay heuristic for large transfer`() = runTest {
        val result = resolver.resolve(tx("PromptPay – MR X", 30000.0, "PromptPay"), 25000.0, "apikey")
        assertEquals("Transfer Out", result.category)
        assertEquals(true, result.flagged)
    }

    @Test fun `calls Claude for unknown merchant`() = runTest {
        coEvery { claude.categorize(any(), any(), any(), any()) } returns Pair("Shopping", "Online purchase")
        val (category, _) = resolver.resolve(tx("RANDOM SHOP"), 25000.0, "apikey")
        assertEquals("Shopping", category)
        coVerify(exactly = 1) { claude.categorize(any(), any(), any(), any()) }
    }
}
