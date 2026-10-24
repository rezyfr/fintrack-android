package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.BudgetLineRepository
import com.fidriyanto.banktracker.data.repository.CategoryRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.LlmRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.fidriyanto.banktracker.domain.model.LlmTool
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AskFinanceUseCaseTest {
    private val rows = listOf(
        TransactionUiModel(1, "Groceries run", "Groceries", 1137850.0, "2026-10-05", "BCA", "expense", TransactionStatus.SYNCED),
        TransactionUiModel(2, "Rent", "Bills", 655000.0, "2026-10-01", "BCA", "expense", TransactionStatus.SYNCED),
    )
    private val llm = mockk<LlmRepository>()
    private val useCase = AskFinanceUseCase(
        llm,
        mockk<TransactionRepository>().also { coEvery { it.fetch(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.success(rows); coEvery { it.recentTransferRates(any(), any()) } returns Result.success(emptyList()) },
        mockk<AccountRepository>().also { every { it.observeAll() } returns flowOf(listOf(Account("BCA", "BCA", "IDR", true))) },
        mockk<CurrencySettingsRepository>().also { every { it.observe() } returns flowOf(CurrencySettings()) },
        mockk<BudgetLineRepository>(),
        mockk<WalletBalanceRepository>(),
        mockk<CategoryRepository>().also { every { it.observeAll() } returns flowOf(emptyList()) },
    )

    // The model calls largest_expenses, then writes this sentence.
    private fun modelWrites(sentence: String, callTool: Boolean = true) {
        coEvery { llm.generate(any(), any(), any(), any()) } coAnswers {
            if (callTool) arg<List<LlmTool>>(3).first { it.name == "largest_expenses" }.run(mapOf("period" to "this_month", "limit" to "2"))
            Result.success(sentence)
        }
    }

    private fun ask() = runBlocking { useCase("What are my 2 biggest expenses?", emptyList()) }

    @Test fun a_sentence_with_exact_amounts_is_the_answer_and_the_tool_text_is_the_source() {
        modelWrites("Your biggest expenses are Groceries run at Rp 1,137,850 and Rent at Rp 655,000.")
        val m = ask().getOrThrow()
        assertEquals("Your biggest expenses are Groceries run at Rp 1,137,850 and Rent at Rp 655,000.", m.text)
        assertTrue(m.tables.single().sections.single().rows.map { it.label }.contains("Groceries run"))
        assertNull(m.source)
    }

    @Test fun a_sentence_with_garbled_amounts_is_replaced_by_the_exact_tool_text() {
        modelWrites("Your biggest are Groceries at Rp1,1,850 and Bills at Rp6555,000.")
        val m = ask().getOrThrow()
        // The sentence is dropped. The exact table is the answer.
        assertEquals("", m.text)
        assertEquals(listOf("Rp 1,137,850", "Rp 655,000"), m.tables.single().sections.single().rows.map { it.value })
    }

    @Test fun an_empty_reply_after_a_tool_call_shows_the_tool_sentence() {
        modelWrites("")
        assertEquals(2, ask().getOrThrow().tables.single().sections.single().rows.size)
    }

    @Test fun numbers_with_no_tool_call_are_not_trusted() {
        modelWrites("You spent Rp 5,000,000.", callTool = false)
        assertTrue(ask().isFailure)
    }

    @Test fun a_reply_with_no_numbers_and_no_tool_is_kept() {
        modelWrites("I can only answer questions about your spending.", callTool = false)
        assertEquals("I can only answer questions about your spending.", ask().getOrThrow().text)
    }

    @Test fun a_follow_up_gets_the_category_and_period_of_the_last_answer() {
        modelWrites("Your biggest expenses are Groceries run at Rp 1,137,850 and Rent at Rp 655,000.")
        val first = ask().getOrThrow()
        assertTrue(first.context.orEmpty(), first.context.orEmpty().startsWith("largest_expenses(period=this_month, limit=2)"))
        assertTrue(first.context.orEmpty(), first.context.orEmpty().contains("which found: Largest expenses from"))
        val system = io.mockk.slot<String>()
        coEvery { llm.generate(capture(system), any(), any(), any()) } returns Result.success("OK")
        runBlocking { useCase("Please break it down", listOf(first)) }
        assertTrue(system.captured, system.captured.contains("The last answer came from: largest_expenses(period=this_month, limit=2)"))
        assertTrue(system.captured, system.captured.contains("keep its category and period"))
    }
}
