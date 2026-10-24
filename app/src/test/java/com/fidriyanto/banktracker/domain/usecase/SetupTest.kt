package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.CurrencyOverride
import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.BankParserRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.StorageRepository
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.AccountType
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.fidriyanto.banktracker.domain.model.NewAccount
import com.fidriyanto.banktracker.domain.model.SetupError
import com.fidriyanto.banktracker.domain.model.SetupInput
import com.fidriyanto.banktracker.domain.model.SetupStatus
import com.fidriyanto.banktracker.domain.model.StorageMode
import com.fidriyanto.banktracker.domain.model.StorageSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SetupTest {
    private val usd = NewAccount("Main", "USD", AccountType.CHECKING)
    private fun input(
        home: String = "USD", day: Int = 1, accounts: List<NewAccount> = listOf(usd), rates: Map<String, Double> = emptyMap(),
    ) = SetupInput(home, day, accounts, rates)

    @Test fun a_valid_single_currency_setup_passes() = assertNull(SetupValidator.validate(input()))

    @Test fun a_valid_two_currency_setup_passes() {
        val two = input(accounts = listOf(usd, NewAccount("Trip", "eur", AccountType.CREDIT)))
        assertNull(SetupValidator.validate(two))
        // Three currencies in one setup.
        assertNull(SetupValidator.validate(input(accounts = listOf(usd, NewAccount("Trip", "EUR", AccountType.CREDIT), NewAccount("Yen", "JPY", AccountType.CHECKING)))))
    }

    @Test fun each_problem_is_named() {
        assertEquals(SetupError.HOME_CURRENCY, SetupValidator.validate(input(home = "")))
        assertEquals(SetupError.HOME_CURRENCY, SetupValidator.validate(input(home = "XYZ1")))
        assertEquals(SetupError.CYCLE_DAY, SetupValidator.validate(input(day = 0)))
        assertEquals(SetupError.CYCLE_DAY, SetupValidator.validate(input(day = 29)))
        assertEquals(SetupError.NO_ACCOUNT, SetupValidator.validate(input(accounts = emptyList())))
        assertEquals(SetupError.ACCOUNT_CURRENCY, SetupValidator.validate(input(accounts = listOf(NewAccount("Bad", "ZZ", AccountType.CHECKING)))))
        assertEquals(SetupError.ACCOUNT_CURRENCY, SetupValidator.validate(input(accounts = listOf(NewAccount(" ", "USD", AccountType.CHECKING)))))
    }

    @Test fun account_ids_come_from_the_name_and_stay_unique() {
        assertEquals("MY_BANK", accountIdFor("My Bank"))
        assertEquals("ACCOUNT", accountIdFor("!!!"))
        assertEquals("MY_BANK_2", accountIdFor("My Bank", setOf("MY_BANK")))
        assertEquals("MY_BANK_3", accountIdFor("my bank", setOf("MY_BANK", "MY_BANK_2")))
    }

    private fun accountsRepo(list: List<Account>, refreshOk: Boolean = true) = mockk<AccountRepository>().also {
        every { it.observeAll() } returns flowOf(list)
        coEvery { it.refresh() } returns if (refreshOk) Result.success(Unit) else Result.failure(Exception("offline"))
        coEvery { it.save(any()) } returns Result.success(Unit)
    }

    private val settingsRepo = mockk<CurrencySettingsRepository>().also {
        coEvery { it.refresh() } returns Result.success(Unit)
        coEvery { it.save(any()) } returns Unit
    }

    private fun storageRepo(mode: StorageMode = StorageMode.SERVER) = mockk<StorageRepository>().also {
        coEvery { it.get() } returns StorageSettings(mode, null, chosen = true)
    }

    @Test fun a_phone_with_accounts_opens_at_once() = runBlocking {
        val repo = accountsRepo(listOf(Account("A", "A", "USD", false)))
        assertEquals(SetupStatus.DONE, CheckSetupUseCase(repo, settingsRepo, storageRepo())())
        coVerify(exactly = 0) { repo.refresh() }
    }

    @Test fun no_accounts_anywhere_needs_setup() = runBlocking {
        assertEquals(SetupStatus.NEEDED, CheckSetupUseCase(accountsRepo(emptyList()), settingsRepo, storageRepo())())
    }

    @Test fun an_offline_phone_with_no_accounts_still_opens() = runBlocking {
        assertEquals(SetupStatus.DONE, CheckSetupUseCase(accountsRepo(emptyList(), refreshOk = false), settingsRepo, storageRepo())())
    }

    @Test fun an_offline_install_with_no_accounts_needs_setup_without_a_server_read() = runBlocking {
        val repo = accountsRepo(emptyList())
        assertEquals(SetupStatus.NEEDED, CheckSetupUseCase(repo, settingsRepo, storageRepo(StorageMode.LOCAL))())
        coVerify(exactly = 0) { repo.refresh() }
    }

    @Test fun finishing_setup_saves_settings_and_every_account() = runBlocking {
        val repo = accountsRepo(emptyList())
        val saved = slot<CurrencySettings>()
        coEvery { settingsRepo.save(capture(saved)) } returns Unit
        val three = input(
            day = 10, rates = mapOf("EUR" to 1.1),
            accounts = listOf(usd, NewAccount("Main", "EUR", AccountType.INVESTMENT), NewAccount("Yen", "jpy", AccountType.CHECKING)),
        )
        assertTrue(CompleteSetupUseCase(repo, settingsRepo)(three).isSuccess)
        assertEquals(
            CurrencySettings("USD", "USD", 10, listOf(CurrencyOverride("EUR", "EUR", 1.1), CurrencyOverride("JPY", "JPY", null))),
            saved.captured,
        )
        coVerify { repo.save(match { it.id == "MAIN" && it.currency == "USD" && it.sortOrder == 0 }) }
        coVerify { repo.save(match { it.id == "MAIN_2" && it.type == AccountType.INVESTMENT && it.sortOrder == 1 }) }
    }

    @Test fun invalid_setup_saves_nothing() = runBlocking {
        val repo = accountsRepo(emptyList())
        assertTrue(CompleteSetupUseCase(repo, settingsRepo)(input(accounts = emptyList())).isFailure)
        coVerify(exactly = 0) { repo.save(any()) }
    }

    private val parsedBca = ParsedTransaction("Shop", 10.0, LocalDate.of(2026, 10, 1), "", 1L, wallet = "BCA", rawFields = mapOf("bank" to "BCA"))
    private fun bankRepo(map: Map<String, String>) = mockk<BankParserRepository>().also { every { it.observe() } returns flowOf(map) }
    private val own = listOf(Account("BCA", "BCA", "IDR", false), Account("MINE", "Mine", "USD", false))

    @Test fun a_chosen_account_wins() = runBlocking {
        val r = ResolveBankWalletUseCase(bankRepo(mapOf("BCA" to "MINE")), accountsRepo(own))(parsedBca)
        assertEquals("MINE", r?.wallet)
    }

    @Test fun an_unset_bank_keeps_its_own_id_when_that_account_exists() = runBlocking {
        assertEquals("BCA", ResolveBankWalletUseCase(bankRepo(emptyMap()), accountsRepo(own))(parsedBca)?.wallet)
    }

    @Test fun an_unset_bank_with_no_such_account_is_skipped() = runBlocking {
        assertNull(ResolveBankWalletUseCase(bankRepo(emptyMap()), accountsRepo(listOf(own[1])))(parsedBca))
    }

    @Test fun a_bank_turned_off_is_skipped_and_the_model_result_passes_through() = runBlocking {
        val off = ResolveBankWalletUseCase(bankRepo(mapOf("BCA" to "")), accountsRepo(own))
        assertNull(off(parsedBca))
        val fromModel = parsedBca.copy(rawFields = mapOf("source" to "ai"))
        assertEquals(fromModel, off(fromModel))
    }

    @Test fun a_new_account_goes_last_and_a_renamed_one_keeps_its_place() = runBlocking {
        val repo = accountsRepo(listOf(Account("A", "A", "USD", false, sortOrder = 0), Account("B", "B", "USD", false, sortOrder = 4)))
        SaveAccountUseCase(repo)(null, "Fresh", "usd")
        coVerify { repo.save(match { it.id == "FRESH" && it.sortOrder == 5 && it.currency == "USD" }) }
        SaveAccountUseCase(repo)("A", "Renamed", "USD")
        coVerify { repo.save(match { it.id == "A" && it.displayName == "Renamed" && it.sortOrder == 0 }) }
    }
}
