package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.AiSettingsRepository
import com.fidriyanto.banktracker.data.repository.CategoryRepository
import com.fidriyanto.banktracker.data.repository.LlmRepository
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.AiSettings
import com.fidriyanto.banktracker.domain.model.Category
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiUseCasesTest {
    private val llm = mockk<LlmRepository>()
    private val accounts = mockk<AccountRepository>().also {
        every { it.observeAll() } returns flowOf(listOf(Account("BBL", "Bangkok Bank", "THB", true), Account("BCA", "BCA", "IDR", true)))
    }
    private val categories = mockk<CategoryRepository>().also {
        every { it.observeAll() } returns flowOf(listOf(Category("Food & Drink", listOf("Coffee"), true), Category("Other", emptyList(), true)))
    }
    private fun settings(on: Boolean) = mockk<AiSettingsRepository>().also { every { it.observe() } returns flowOf(AiSettings(on, on)) }

    private fun reply(text: String) = coEvery { llm.generate(any(), any(), any(), any()) } returns Result.success(text)

    @Test fun notification_becomes_a_transaction_in_the_wallet_of_its_currency() = runBlocking {
        reply("expense|85.00|THB|7-ELEVEN|Food & Drink|Coffee")
        val parsed = ParseNotificationWithAiUseCase(llm, accounts, categories, settings(true))("Bualuang", "7-ELEVEN85.00THB", 1_760_000_000_000)!!
        assertEquals("BBL", parsed.wallet)
        assertEquals(85.0, parsed.amount, 0.0)
        assertEquals("7-ELEVEN", parsed.item)
        assertEquals("Coffee", parsed.subcategory)
        assertEquals("ai", parsed.rawFields["source"])
    }

    @Test fun income_in_rupiah_goes_to_the_rupiah_wallet() = runBlocking {
        reply("income|2000000|Rp|BUDI||")
        val parsed = ParseNotificationWithAiUseCase(llm, accounts, categories, settings(true))("BCA", "Transfer masuk Rp 2.000.000 dari BUDI", 1L)!!
        assertEquals("BCA", parsed.wallet)
        assertEquals("income", parsed.txType)
    }

    @Test fun a_currency_with_no_wallet_is_skipped() = runBlocking {
        reply("expense|10|USD|Shop||")
        assertNull(ParseNotificationWithAiUseCase(llm, accounts, categories, settings(true))("Bank", "Paid USD 10 at Shop", 1L))
    }

    @Test fun a_non_payment_is_skipped() = runBlocking {
        reply("none|0|none|||")
        assertNull(ParseNotificationWithAiUseCase(llm, accounts, categories, settings(true))("Bank", "Your OTP is 123456 transaction code", 1L))
    }

    @Test fun the_setting_off_never_calls_the_model() = runBlocking {
        assertNull(ParseNotificationWithAiUseCase(llm, accounts, categories, settings(false))("Bank", "Paid THB 10", 1L))
        coVerify(exactly = 0) { llm.generate(any(), any(), any(), any()) }
    }

    @Test fun a_missing_model_gives_no_result() = runBlocking {
        coEvery { llm.generate(any(), any(), any(), any()) } returns Result.failure(IllegalStateException("not installed"))
        assertNull(ParseNotificationWithAiUseCase(llm, accounts, categories, settings(true))("Bank", "Paid THB 10", 1L))
    }

    @Test fun suggestion_uses_the_users_categories() = runBlocking {
        reply("Food & Drink|Coffee")
        val s = SuggestCategoryUseCase(llm, categories, settings(true))("latte at the corner cafe")!!
        assertEquals("Food & Drink", s.category)
        assertEquals("Coffee", s.subcategory)
    }

    @Test fun short_text_or_setting_off_skips_the_model() = runBlocking {
        assertNull(SuggestCategoryUseCase(llm, categories, settings(true))("ab"))
        assertNull(SuggestCategoryUseCase(llm, categories, settings(false))("latte"))
        coVerify(exactly = 0) { llm.generate(any(), any(), any(), any()) }
    }
}
