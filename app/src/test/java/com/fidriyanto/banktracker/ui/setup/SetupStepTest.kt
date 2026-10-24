package com.fidriyanto.banktracker.ui.setup

import com.fidriyanto.banktracker.domain.model.AccountType
import com.fidriyanto.banktracker.domain.model.NewAccount
import com.fidriyanto.banktracker.domain.model.ServerCheck
import com.fidriyanto.banktracker.domain.model.StorageMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupStepTest {
    private val withoutRates = listOf(SetupStep.WELCOME, SetupStep.STORAGE, SetupStep.CURRENCY, SetupStep.CYCLE, SetupStep.ACCOUNTS, SetupStep.DONE)

    @Test fun theRatesPageOnlyShowsWhenAnotherCurrencyIsUsed() {
        assertEquals(withoutRates, SetupStep.path(hasOtherCurrencies = false))
        assertEquals(SetupStep.entries - SetupStep.SERVER, SetupStep.path(hasOtherCurrencies = true))
    }

    @Test fun theServerPageOnlyShowsWhenTheDataLivesOnAServer() {
        assertEquals(SetupStep.entries.toList(), SetupStep.path(hasOtherCurrencies = true, useServer = true))
        assertEquals(SetupStep.SERVER, SetupStep.next(SetupStep.STORAGE, hasOtherCurrencies = false, useServer = true))
        assertEquals(SetupStep.CURRENCY, SetupStep.next(SetupStep.STORAGE, hasOtherCurrencies = false))
        assertEquals(SetupStep.SERVER, SetupStep.previous(SetupStep.CURRENCY, hasOtherCurrencies = false, useServer = true))
        assertEquals(SetupStep.STORAGE, SetupStep.previous(SetupStep.CURRENCY, hasOtherCurrencies = false))
    }

    @Test fun nextAndPreviousFollowThePath() {
        assertEquals(SetupStep.DONE, SetupStep.next(SetupStep.ACCOUNTS, hasOtherCurrencies = false))
        assertEquals(SetupStep.RATES, SetupStep.next(SetupStep.ACCOUNTS, hasOtherCurrencies = true))
        assertEquals(SetupStep.ACCOUNTS, SetupStep.previous(SetupStep.DONE, hasOtherCurrencies = false))
        assertEquals(SetupStep.RATES, SetupStep.previous(SetupStep.DONE, hasOtherCurrencies = true))
    }

    @Test fun theEndsDoNotMove() {
        assertEquals(SetupStep.DONE, SetupStep.next(SetupStep.DONE, hasOtherCurrencies = false))
        assertEquals(SetupStep.WELCOME, SetupStep.previous(SetupStep.WELCOME, hasOtherCurrencies = true))
    }

    private val usd = NewAccount("Main", "USD", AccountType.CHECKING)
    private fun form(step: SetupStep, home: String = "USD", day: String = "1", accounts: List<NewAccount> = emptyList()) =
        SetupFormState(homeCode = home, cycleDay = day, accounts = accounts, step = step)

    @Test fun eachPageHasItsOwnRuleToContinue() {
        assertTrue(form(SetupStep.WELCOME).canContinue)
        assertTrue(form(SetupStep.CURRENCY).canContinue)
        assertFalse(form(SetupStep.CURRENCY, home = "XX").canContinue)
        assertFalse(form(SetupStep.CURRENCY, home = "").canContinue)
        assertTrue(form(SetupStep.CYCLE, day = "28").canContinue)
        assertFalse(form(SetupStep.CYCLE, day = "29").canContinue)
        assertFalse(form(SetupStep.CYCLE, day = "").canContinue)
        assertFalse(form(SetupStep.ACCOUNTS).canContinue)
        assertTrue(form(SetupStep.ACCOUNTS, accounts = listOf(usd)).canContinue)
        assertTrue(form(SetupStep.RATES, accounts = listOf(usd)).canContinue)
        assertTrue(form(SetupStep.DONE, accounts = listOf(usd)).canContinue)
        assertFalse(form(SetupStep.DONE).canContinue)
    }

    @Test fun theStorageAndServerPagesNeedAnAnswer() {
        assertFalse(form(SetupStep.STORAGE).canContinue)
        assertTrue(form(SetupStep.STORAGE).copy(storage = StorageMode.LOCAL).canContinue)
        val server = form(SetupStep.SERVER).copy(storage = StorageMode.SERVER)
        assertFalse(server.canContinue)
        assertFalse(server.copy(serverCheck = ServerCheck.BadToken).canContinue)
        assertFalse(server.copy(serverCheck = ServerCheck.Ok(0), checking = true).canContinue)
        assertTrue(server.copy(serverCheck = ServerCheck.Ok(0)).canContinue)
    }

    @Test fun aServerWithAccountsEndsSetupOnTheServerPage() {
        val server = form(SetupStep.SERVER).copy(storage = StorageMode.SERVER, serverAddress = "fintrack.example.com", serverToken = " t ")
        assertFalse(server.copy(serverCheck = ServerCheck.Ok(0)).serverHasData)
        assertTrue(server.copy(serverCheck = ServerCheck.Ok(2)).serverHasData)
        assertFalse(server.copy(storage = StorageMode.LOCAL, serverCheck = ServerCheck.Ok(2)).serverHasData)
        assertEquals("https://fintrack.example.com", server.server?.baseUrl)
        assertEquals("t", server.server?.token)
    }

    @Test fun progressGrowsAndReachesOneOnTheLastPage() {
        val accounts = listOf(usd, NewAccount("Trip", "EUR", AccountType.CHECKING))
        assertEquals(1f / 7f, form(SetupStep.WELCOME, accounts = accounts).progress, 1e-6f)
        assertEquals(6f / 7f, form(SetupStep.RATES, accounts = accounts).progress, 1e-6f)
        assertEquals(1f, form(SetupStep.DONE, accounts = accounts).progress, 1e-6f)
        assertEquals(1f, form(SetupStep.DONE, accounts = listOf(usd)).progress, 1e-6f)
    }
}
