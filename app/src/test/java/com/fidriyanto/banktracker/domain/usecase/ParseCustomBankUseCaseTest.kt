package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.CustomBankRepository
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.CustomBankPattern
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ParseCustomBankUseCaseTest {
    private val patterns = mockk<CustomBankRepository>()
    private val accounts = mockk<AccountRepository>()
    private val now = 1_760_000_000_000L

    private fun pattern(template: String, wallet: String = "A", type: String = "expense", name: String = "My bank", id: String = name) =
        CustomBankPattern(id, name, null, template, wallet, type)

    private fun useCase(list: List<CustomBankPattern>): ParseCustomBankUseCase {
        coEvery { patterns.all() } returns list
        every { accounts.observeAll() } returns flowOf(listOf(Account("A", "Main", "USD", true), Account("B", "Second", "EUR", false)))
        return ParseCustomBankUseCase(patterns, accounts)
    }

    @Test fun aMatchBecomesATransactionForTheAccountOfThePattern() = runBlocking {
        val parsed = useCase(listOf(pattern("Paid {amount} to {merchant}", wallet = "B")))("Alert", "Paid 12.50 to Cafe", now)!!
        assertEquals("Cafe", parsed.item)
        assertEquals(12.5, parsed.amount, 0.0)
        assertEquals("B", parsed.wallet)
        assertEquals("expense", parsed.txType)
        assertEquals("Other", parsed.category)
        assertEquals(now, parsed.timestampMs)
        assertEquals(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate(), parsed.date)
    }

    @Test fun anIncomePatternGivesIncome() = runBlocking {
        val parsed = useCase(listOf(pattern("Received {amount}", type = "income")))("Alert", "Received 40", now)!!
        assertEquals("income", parsed.txType)
    }

    @Test fun withoutAMerchantTheItemIsThePatternName() = runBlocking {
        val parsed = useCase(listOf(pattern("Received {amount}", name = "Salary bank")))("Alert", "Received 40", now)!!
        assertEquals("Salary bank", parsed.item)
    }

    @Test fun noMatchGivesNull() = runBlocking {
        assertNull(useCase(listOf(pattern("Paid {amount}")))("Alert", "Hello there", now))
    }

    @Test fun firstMatchingPatternWins() = runBlocking {
        val list = listOf(pattern("Paid {amount}", wallet = "A", id = "1"), pattern("Paid {amount}", wallet = "B", id = "2"))
        assertEquals("A", useCase(list)("Alert", "Paid 5", now)!!.wallet)
    }

    @Test fun aPatternWhoseAccountWasRemovedGivesNull() = runBlocking {
        assertNull(useCase(listOf(pattern("Paid {amount}", wallet = "GONE")))("Alert", "Paid 5", now))
    }
}
