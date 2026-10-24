package com.fidriyanto.banktracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery

// The sum of one account's money flow, from the balance queries.
data class WalletNet(val walletId: String?, val net: Double)

// ac: offline-mode - the offline store. Each query answers like the matching endpoint of the server.
@Dao
abstract class LedgerDao {
    // Transactions

    @Insert
    abstract suspend fun insertTransaction(row: LedgerTransactionEntity): Long

    @Update
    abstract suspend fun updateTransaction(row: LedgerTransactionEntity)

    @Query("SELECT * FROM ledger_transactions WHERE id = :id")
    abstract suspend fun transaction(id: Long): LedgerTransactionEntity?

    @Query("DELETE FROM ledger_transactions WHERE id IN (:ids)")
    abstract suspend fun deleteTransactions(ids: List<Long>): Int

    @Query("UPDATE ledger_transactions SET category = :category WHERE id IN (:ids)")
    abstract suspend fun setCategory(ids: List<Long>, category: String)

    // The filtered list, built by ledgerTransactionQuery.
    @RawQuery
    abstract suspend fun queryTransactions(query: SupportSQLiteQuery): List<LedgerTransactionEntity>

    @Query(
        "SELECT * FROM ledger_transactions WHERE txType = 'transfer' AND wallet IN (:from) AND toWallet IN (:to) " +
            "AND amount > 0 AND toAmount > 0 ORDER BY date DESC, id DESC LIMIT :limit",
    )
    abstract suspend fun transfers(from: List<String>, to: List<String>, limit: Int): List<LedgerTransactionEntity>

    // Money that leaves or enters each account up to a day. Transfers count here as money that leaves.
    @Query(
        "SELECT wallet AS walletId, SUM(CASE txType WHEN 'income' THEN amount WHEN 'expense' THEN -amount " +
            "WHEN 'transfer' THEN -amount ELSE 0 END) AS net FROM ledger_transactions WHERE date <= :upTo GROUP BY wallet",
    )
    abstract suspend fun ownFlow(upTo: String): List<WalletNet>

    // Money that transfers bring into each account up to a day: to_amount, or amount when to_amount is null.
    @Query(
        "SELECT toWallet AS walletId, SUM(COALESCE(toAmount, amount)) AS net FROM ledger_transactions " +
            "WHERE txType = 'transfer' AND toWallet IS NOT NULL AND date <= :upTo GROUP BY toWallet",
    )
    abstract suspend fun incomingTransfers(upTo: String): List<WalletNet>

    @Query("SELECT * FROM ledger_transactions ORDER BY id")
    abstract suspend fun allTransactions(): List<LedgerTransactionEntity>

    // Installments

    @Query("SELECT * FROM ledger_installments ORDER BY dueDay, id")
    abstract suspend fun installments(): List<LedgerInstallmentEntity>

    @Query("SELECT * FROM ledger_installments WHERE id = :id")
    abstract suspend fun installment(id: Long): LedgerInstallmentEntity?

    @Insert
    abstract suspend fun insertInstallment(row: LedgerInstallmentEntity): Long

    @Query("UPDATE ledger_installments SET merchant = :merchant WHERE id = :id")
    abstract suspend fun setMerchant(id: Long, merchant: String): Int

    @Query("UPDATE ledger_installments SET currentStep = :step, status = :status WHERE id = :id")
    abstract suspend fun setStep(id: Long, step: Int, status: String): Int

    @Query("UPDATE ledger_installments SET excluded = :excluded WHERE id = :id")
    abstract suspend fun setExcluded(id: Long, excluded: Boolean): Int

    @Query("DELETE FROM ledger_installments WHERE id = :id")
    abstract suspend fun deleteInstallmentRow(id: Long): Int

    @Query("UPDATE ledger_transactions SET installmentId = NULL WHERE installmentId = :installmentId")
    abstract suspend fun unlinkAllFrom(installmentId: Long)

    // A deleted plan keeps its transactions and clears their link, the same as ON DELETE SET NULL on the server.
    @Transaction
    open suspend fun deleteInstallment(id: Long): Int {
        unlinkAllFrom(id)
        return deleteInstallmentRow(id)
    }

    // Budget lines

    @Query("SELECT * FROM ledger_budget_lines WHERE active = 1 ORDER BY sortOrder, id")
    abstract suspend fun activeBudgetLines(): List<LedgerBudgetLineEntity>

    @Query("SELECT * FROM ledger_budget_lines WHERE id = :id")
    abstract suspend fun budgetLine(id: Long): LedgerBudgetLineEntity?

    @Insert
    abstract suspend fun insertBudgetLine(row: LedgerBudgetLineEntity): Long

    @Update
    abstract suspend fun updateBudgetLine(row: LedgerBudgetLineEntity)

    @Query("DELETE FROM ledger_budget_lines WHERE id = :id")
    abstract suspend fun deleteBudgetLine(id: Long): Int

    @Query("SELECT * FROM ledger_budget_lines ORDER BY id")
    abstract suspend fun allBudgetLines(): List<LedgerBudgetLineEntity>

    // Card billing

    @Query("SELECT * FROM ledger_card_billing ORDER BY wallet")
    abstract suspend fun cardBilling(): List<LedgerCardBillingEntity>

    // Backup: an import replaces the whole offline store in one transaction.

    @Query("SELECT * FROM ledger_installments ORDER BY id")
    abstract suspend fun allInstallments(): List<LedgerInstallmentEntity>

    @Insert
    abstract suspend fun insertTransactions(rows: List<LedgerTransactionEntity>)

    @Insert
    abstract suspend fun insertInstallments(rows: List<LedgerInstallmentEntity>)

    @Insert
    abstract suspend fun insertBudgetLines(rows: List<LedgerBudgetLineEntity>)

    @Insert
    abstract suspend fun insertCardBilling(rows: List<LedgerCardBillingEntity>)

    @Query("DELETE FROM ledger_transactions")
    abstract suspend fun clearTransactions()

    @Query("DELETE FROM ledger_installments")
    abstract suspend fun clearInstallments()

    @Query("DELETE FROM ledger_budget_lines")
    abstract suspend fun clearBudgetLines()

    @Query("DELETE FROM ledger_card_billing")
    abstract suspend fun clearCardBilling()

    @Transaction
    open suspend fun replaceAll(
        transactions: List<LedgerTransactionEntity>,
        installments: List<LedgerInstallmentEntity>,
        budgetLines: List<LedgerBudgetLineEntity>,
        cardBilling: List<LedgerCardBillingEntity>,
    ) {
        clearTransactions()
        clearInstallments()
        clearBudgetLines()
        clearCardBilling()
        insertInstallments(installments)
        insertBudgetLines(budgetLines)
        insertCardBilling(cardBilling)
        insertTransactions(transactions)
    }
}
