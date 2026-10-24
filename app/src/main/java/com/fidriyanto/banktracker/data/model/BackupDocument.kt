package com.fidriyanto.banktracker.data.model

import com.fidriyanto.banktracker.data.datasource.remote.dto.AccountDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetLineDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.CardBillingDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.CategoryDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.SubcategoryDto
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.fidriyanto.banktracker.domain.model.CustomBankPattern
import com.fidriyanto.banktracker.domain.model.Schedule
import com.google.gson.annotations.SerializedName

// The value of "format" in every backup file. A file without it is not a FinTrack backup.
const val BACKUP_FORMAT = "fintrack-backup"

// The newest backup version this app reads and writes.
const val BACKUP_VERSION = 1

// ac: offline-backup - one JSON file that holds everything an offline install keeps: the accounts, the
// categories, the offline store and the settings that live only on the phone. Every field has a default,
// so Gson fills a field that the file leaves out.
data class BackupDocument(
    @SerializedName("format") val format: String = "",
    @SerializedName("version") val version: Int = 0,
    @SerializedName("exported_at") val exportedAt: String = "",
    @SerializedName("accounts") val accounts: List<AccountDto> = emptyList(),
    @SerializedName("categories") val categories: List<CategoryDto> = emptyList(),
    @SerializedName("subcategories") val subcategories: List<SubcategoryDto> = emptyList(),
    @SerializedName("ledger") val ledger: LedgerBackup = LedgerBackup(),
    @SerializedName("currency_settings") val currencySettings: CurrencySettings? = null,
    @SerializedName("rules") val rules: RuleDocument? = null,
    @SerializedName("schedules") val schedules: List<Schedule> = emptyList(),
    @SerializedName("custom_banks") val customBanks: List<CustomBankPattern> = emptyList(),
    @SerializedName("bank_accounts") val bankAccounts: Map<String, String> = emptyMap(),
)

// The four tables of the offline store. A transaction keeps its id, so the links from a transaction to an
// installment or a budget line stay correct after an import.
data class LedgerBackup(
    @SerializedName("transactions") val transactions: List<BackupTransaction> = emptyList(),
    @SerializedName("installments") val installments: List<InstallmentDto> = emptyList(),
    @SerializedName("budget_lines") val budgetLines: List<BudgetLineDto> = emptyList(),
    @SerializedName("card_billing") val cardBilling: List<CardBillingDto> = emptyList(),
)

// One transaction of the offline store. It has the note and the creation time, which TransactionDto leaves out.
data class BackupTransaction(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("item") val item: String = "",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("category") val category: String = "",
    @SerializedName("subcategory") val subcategory: String? = null,
    @SerializedName("date") val date: String = "",
    @SerializedName("tab") val tab: String = "",
    @SerializedName("note") val note: String? = null,
    @SerializedName("wallet") val wallet: String = "",
    @SerializedName("tx_type") val txType: String = "",
    @SerializedName("to_wallet") val toWallet: String? = null,
    @SerializedName("to_amount") val toAmount: Double? = null,
    @SerializedName("budget_line_id") val budgetLineId: Long? = null,
    @SerializedName("installment_id") val installmentId: Long? = null,
    @SerializedName("created_at") val createdAt: Long = 0,
)
