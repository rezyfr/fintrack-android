package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class TransactionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("item") val item: String?,
    @SerializedName("amount") val amount: Double,
    @SerializedName("category") val category: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("tab") val tab: String?,
    @SerializedName("wallet") val wallet: String?,
    @SerializedName("tx_type") val txType: String?,
    @SerializedName("to_wallet") val toWallet: String?,
    // ac: add-transfer-target-amount — received amount for cross-currency transfers, read back
    @SerializedName("to_amount") val toAmount: Double? = null,
    // ac: add-transaction-subcategory — optional subcategory read back from the server
    @SerializedName("subcategory") val subcategory: String? = null,
    // ac: budget-line-details-and-assignment — manual budget assignment read back from the server
    @SerializedName("budget_line_id") val budgetLineId: Long? = null,
    // ac: installment-link-edit — the installment this transaction pays a step of, read back
    @SerializedName("installment_id") val installmentId: Long? = null,
)

data class TransactionInsertDto(
    @SerializedName("tab") val tab: String,
    @SerializedName("date") val date: String,
    @SerializedName("item") val item: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("category") val category: String,
    @SerializedName("note") val note: String?,
    @SerializedName("wallet") val wallet: String?,
    @SerializedName("tx_type") val txType: String,
    @SerializedName("to_wallet") val toWallet: String?,
    // ac: add-transfer-target-amount — persist the received amount for cross-currency transfers
    @SerializedName("to_amount") val toAmount: Double? = null,
    @SerializedName("subcategory") val subcategory: String? = null,
    // ac: installment-auto-link — set only when the transaction matched an installment
    @SerializedName("installment_id") val installmentId: Long? = null,
)

data class TransactionPatchDto(
    @SerializedName("amount") val amount: Double,
    @SerializedName("item") val item: String,
    @SerializedName("category") val category: String,
    @SerializedName("date") val date: String,
    @SerializedName("wallet") val wallet: String?,
    @SerializedName("tx_type") val txType: String,
    @SerializedName("to_wallet") val toWallet: String?,
    @SerializedName("to_amount") val toAmount: Double?,
    @SerializedName("subcategory") val subcategory: String? = null,
    // ac: installment-link-edit — sent only when a link is set. An unlink uses its own PATCH.
    @SerializedName("installment_id") val installmentId: Long? = null,
)

// ac: batch-edit-transaction-category — a batch changes the category of every listed id and nothing else
data class CategoryBatchPatchDto(
    @SerializedName("ids") val ids: List<Long>,
    @SerializedName("category") val category: String,
)

// One page of the transaction list. nextOffset is null on the last page.
data class TransactionPageDto(
    @SerializedName("items") val items: List<TransactionDto>,
    @SerializedName("next_offset") val nextOffset: Int?,
)

data class TransferRatesDto(
    @SerializedName("rates") val rates: List<Double>,
)
