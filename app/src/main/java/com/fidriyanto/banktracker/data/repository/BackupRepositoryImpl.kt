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
import com.fidriyanto.banktracker.data.datasource.remote.dto.CategoryDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.SubcategoryDto
import com.fidriyanto.banktracker.data.db.AccountEntity
import com.fidriyanto.banktracker.data.db.CategoryEntity
import com.fidriyanto.banktracker.data.db.SubcategoryEntity
import com.fidriyanto.banktracker.data.model.BACKUP_FORMAT
import com.fidriyanto.banktracker.data.model.BACKUP_VERSION
import com.fidriyanto.banktracker.data.model.BackupDocument
import com.fidriyanto.banktracker.domain.model.BackupOutcome
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-backup - the export writes one file with everything an offline install keeps. The import
// checks the file, then replaces the data on the phone. The AI settings and model stay out of the file.
@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val file: BackupFileDataSource,
    private val ledger: LedgerBackupDataSource,
    private val accounts: AccountLocalDataSource,
    private val categories: CategoryLocalDataSource,
    private val currencySettings: CurrencySettingsDataSource,
    private val rules: RuleDataSource,
    private val schedules: ScheduleDataSource,
    private val customBanks: CustomBankDataSource,
    private val bankAccounts: BankParserDataSource,
) : BackupRepository {
    override suspend fun export(uri: String) = attempt {
        val document = BackupDocument(
            format = BACKUP_FORMAT, version = BACKUP_VERSION, exportedAt = Instant.now().toString(),
            accounts = accounts.observeAll().first().map { AccountDto(it.id, it.name, it.currency, it.sortOrder, it.isDefault, it.type, it.openingBalance) },
            categories = categories.observeCategories().first().map { CategoryDto(it.name, it.sortOrder, it.isDefault) },
            subcategories = categories.observeSubcategories().first().map { SubcategoryDto(it.id, it.category, it.name, it.sortOrder) },
            ledger = ledger.read(),
            currencySettings = currencySettings.observe().first(),
            rules = rules.document(),
            schedules = schedules.all(),
            customBanks = customBanks.all(),
            bankAccounts = bankAccounts.observe().first(),
        )
        file.write(uri, document)
        BackupOutcome.DONE
    }

    override suspend fun restore(uri: String) = attempt {
        val document = file.read(uri)
        when {
            document.format != BACKUP_FORMAT -> BackupOutcome.NOT_A_BACKUP
            document.version > BACKUP_VERSION -> BackupOutcome.TOO_NEW
            else -> replaceWith(document).let { BackupOutcome.DONE }
        }
    }

    // The rows are mapped before the first write, so a bad row stops the import before anything changes.
    private suspend fun replaceWith(document: BackupDocument) {
        val accountRows = document.accounts.map { AccountEntity(it.id, it.name, it.currency, it.sortOrder, it.isDefault, it.type, it.openingBalance) }
        val categoryRows = document.categories.map { CategoryEntity(it.name, it.sortOrder, it.isDefault) }
        val subcategoryRows = document.subcategories.map { SubcategoryEntity(it.id, it.category, it.name, it.sortOrder) }
        ledger.replace(document.ledger)
        accounts.replaceAll(accountRows)
        categories.replaceAll(categoryRows, subcategoryRows)
        document.currencySettings?.let { currencySettings.save(it) }
        // An offline install has no shared copy of the rules, so nothing waits to be sent.
        document.rules?.let { rules.save(it, dirty = false) }
        schedules.all().forEach { schedules.delete(it.id) }
        document.schedules.forEach { schedules.upsert(it) }
        customBanks.all().forEach { customBanks.delete(it.id) }
        document.customBanks.forEach { customBanks.upsert(it) }
        bankAccounts.replaceAll(document.bankAccounts)
    }

    private inline fun attempt(block: () -> BackupOutcome): BackupOutcome = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: JsonParseException) {
        BackupOutcome.NOT_A_BACKUP
    } catch (e: Exception) {
        BackupOutcome.FAILED
    }
}
