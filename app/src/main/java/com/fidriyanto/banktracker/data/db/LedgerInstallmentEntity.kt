package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// ac: offline-mode - an installment plan in offline mode, with the fields of the server's installments table.
@Entity(tableName = "ledger_installments")
data class LedgerInstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val merchantPattern: String,
    val wallet: String,
    val installmentAmount: Double,
    val totalInstallments: Int,
    val currentStep: Int = 0,
    val dueDay: Int,
    val status: String = "active",
    val excluded: Boolean = false,
)
