package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.AccountType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveAccountUseCaseTest {
    private val saved = slot<Account>()

    private fun useCase(existing: List<Account>): SaveAccountUseCase {
        val repo = mockk<AccountRepository>()
        every { repo.observeAll() } returns flowOf(existing)
        coEvery { repo.save(capture(saved)) } returns Result.success(Unit)
        return SaveAccountUseCase(repo)
    }

    // ac: account-opening-balance — the opening balance is saved with the account
    @Test fun saves_the_opening_balance() = runBlocking {
        useCase(emptyList())(null, "Index fund", "usd", AccountType.INVESTMENT, openingBalance = 12400.0)
        assertEquals(12400.0, saved.captured.openingBalance, 0.0)
        assertEquals("USD", saved.captured.currency)
    }

    // A credit card has no opening balance, so a value sent for one is dropped.
    @Test fun a_credit_account_keeps_zero() = runBlocking {
        useCase(emptyList())(null, "Card", "USD", AccountType.CREDIT, openingBalance = 500.0)
        assertEquals(0.0, saved.captured.openingBalance, 0.0)
    }

    // Setting an opening balance on a default account must not turn it into an ordinary one.
    @Test fun editing_a_default_account_keeps_it_default() = runBlocking {
        val bca = Account("BCA", "BCA", "IDR", isDefault = true, sortOrder = 3)
        useCase(listOf(bca))("BCA", "BCA", "IDR", AccountType.CHECKING, openingBalance = 2_000_000.0)
        assertTrue(saved.captured.isDefault)
        assertEquals(3, saved.captured.sortOrder)
        assertEquals(2_000_000.0, saved.captured.openingBalance, 0.0)
    }
}
