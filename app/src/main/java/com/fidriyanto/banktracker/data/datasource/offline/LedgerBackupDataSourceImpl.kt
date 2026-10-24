package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.datasource.LedgerBackupDataSource
import com.fidriyanto.banktracker.data.db.LedgerDao
import com.fidriyanto.banktracker.data.model.LedgerBackup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerBackupDataSourceImpl @Inject constructor(
    private val dao: LedgerDao,
) : LedgerBackupDataSource {
    override suspend fun read() = LedgerBackup(
        transactions = dao.allTransactions().map { it.toBackup() },
        installments = dao.allInstallments().map { it.toDto() },
        budgetLines = dao.allBudgetLines().map { it.toDto() },
        cardBilling = dao.cardBilling().map { it.toDto() },
    )

    // The rows are mapped before the transaction starts, so a bad row stops the import before anything is deleted.
    override suspend fun replace(backup: LedgerBackup) {
        val transactions = backup.transactions.map { it.toLedger() }
        val installments = backup.installments.map { it.toLedger() }
        val budgetLines = backup.budgetLines.map { it.toLedger() }
        val cardBilling = backup.cardBilling.map { it.toLedger() }
        dao.replaceAll(transactions, installments, budgetLines, cardBilling)
    }
}
