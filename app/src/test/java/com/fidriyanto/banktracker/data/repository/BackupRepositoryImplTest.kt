package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.AccountLocalDataSource
import com.fidriyanto.banktracker.data.datasource.BackupFileDataSource
import com.fidriyanto.banktracker.data.datasource.BankParserDataSource
import com.fidriyanto.banktracker.data.datasource.CategoryLocalDataSource
import com.fidriyanto.banktracker.data.datasource.CurrencySettingsDataSource
import com.fidriyanto.banktracker.data.datasource.CustomBankDataSource
import com.fidriyanto.banktracker.data.datasource.LedgerBackupDataSource
import com.fidriyanto.banktracker.data.datasource.RuleDataSource
import com.fidriyanto.banktracker.data.datasource.ScheduleDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.AccountDto
import com.fidriyanto.banktracker.data.db.AccountEntity
import com.fidriyanto.banktracker.data.model.BACKUP_FORMAT
import com.fidriyanto.banktracker.data.model.BACKUP_VERSION
import com.fidriyanto.banktracker.data.model.BackupDocument
import com.fidriyanto.banktracker.data.model.BackupTransaction
import com.fidriyanto.banktracker.data.model.LedgerBackup
import com.fidriyanto.banktracker.data.model.RuleDocument
import com.fidriyanto.banktracker.domain.model.BackupOutcome
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupRepositoryImplTest {
    private val file = mockk<BackupFileDataSource>(relaxed = true)
    private val ledger = mockk<LedgerBackupDataSource>(relaxed = true)
    private val accounts = mockk<AccountLocalDataSource>(relaxed = true)
    private val categories = mockk<CategoryLocalDataSource>(relaxed = true)
    private val currency = mockk<CurrencySettingsDataSource>(relaxed = true)
    private val rules = mockk<RuleDataSource>(relaxed = true)
    private val schedules = mockk<ScheduleDataSource>(relaxed = true)
    private val customBanks = mockk<CustomBankDataSource>(relaxed = true)
    private val bankAccounts = mockk<BankParserDataSource>(relaxed = true)
    private val repo = BackupRepositoryImpl(file, ledger, accounts, categories, currency, rules, schedules, customBanks, bankAccounts)

    private val account = AccountEntity("A", "Main", "USD", 0, true)
    private val ledgerRows = LedgerBackup(transactions = listOf(BackupTransaction(id = 4, item = "Lunch", amount = 3.0, note = "with team")))
    private val valid = BackupDocument(
        format = BACKUP_FORMAT, version = BACKUP_VERSION, accounts = listOf(AccountDto("A", "Main", "USD", 0, true)),
        ledger = ledgerRows, currencySettings = CurrencySettings(homeCode = "USD"), rules = RuleDocument(emptyList(), 5L),
        bankAccounts = mapOf("bank" to "A"),
    )

    @Test fun theExportWritesEverythingThePhoneKeeps() = runBlocking {
        every { accounts.observeAll() } returns flowOf(listOf(account))
        every { categories.observeCategories() } returns flowOf(emptyList())
        every { categories.observeSubcategories() } returns flowOf(emptyList())
        every { currency.observe() } returns flowOf(CurrencySettings(homeCode = "USD"))
        every { bankAccounts.observe() } returns flowOf(mapOf("bank" to "A"))
        coEvery { ledger.read() } returns ledgerRows
        coEvery { rules.document() } returns RuleDocument(emptyList(), 5L)
        coEvery { schedules.all() } returns emptyList()
        coEvery { customBanks.all() } returns emptyList()
        val written = slot<BackupDocument>()
        coEvery { file.write("content://out", capture(written)) } returns Unit

        assertEquals(BackupOutcome.DONE, repo.export("content://out"))
        assertEquals(valid, written.captured.copy(exportedAt = ""))
    }

    @Test fun theImportReplacesTheDataOnThePhone() = runBlocking {
        coEvery { file.read("content://in") } returns valid
        coEvery { schedules.all() } returns emptyList()
        coEvery { customBanks.all() } returns emptyList()

        assertEquals(BackupOutcome.DONE, repo.restore("content://in"))
        coVerify { ledger.replace(ledgerRows) }
        coVerify { accounts.replaceAll(listOf(account)) }
        coVerify { categories.replaceAll(emptyList(), emptyList()) }
        coVerify { rules.save(RuleDocument(emptyList(), 5L), dirty = false) }
        coVerify { bankAccounts.replaceAll(mapOf("bank" to "A")) }
    }

    @Test fun aFileThatIsNotABackupChangesNothing() = runBlocking {
        coEvery { file.read("content://in") } returns valid.copy(format = "other")
        assertEquals(BackupOutcome.NOT_A_BACKUP, repo.restore("content://in"))
        coEvery { file.read("content://in") } throws JsonSyntaxException("not json")
        assertEquals(BackupOutcome.NOT_A_BACKUP, repo.restore("content://in"))
        coVerify(exactly = 0) { ledger.replace(any()) }
    }

    @Test fun aFileFromANewerAppChangesNothing() = runBlocking {
        coEvery { file.read("content://in") } returns valid.copy(version = BACKUP_VERSION + 1)
        assertEquals(BackupOutcome.TOO_NEW, repo.restore("content://in"))
        coVerify(exactly = 0) { ledger.replace(any()) }
    }

    // Gson leaves a missing name as null. The import stops before the first write.
    @Test fun aBrokenRowStopsTheImportBeforeAnyWrite() = runBlocking {
        val json = """{"format":"$BACKUP_FORMAT","version":1,"accounts":[{"id":"A","currency":"USD"}]}"""
        coEvery { file.read("content://in") } returns Gson().fromJson(json, BackupDocument::class.java)
        assertEquals(BackupOutcome.FAILED, repo.restore("content://in"))
        coVerify(exactly = 0) { ledger.replace(any()) }
        coVerify(exactly = 0) { accounts.replaceAll(any()) }
    }

    @Test fun aFileWithMissingListsReadsThemAsEmpty() {
        val doc = Gson().fromJson("""{"format":"$BACKUP_FORMAT","version":1}""", BackupDocument::class.java)
        assertEquals(emptyList<AccountDto>(), doc.accounts)
        assertEquals(LedgerBackup(), doc.ledger)
    }
}
