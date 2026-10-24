package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// ac: offline-mode - the billing setup of a card in offline mode, with the fields of the server's
// card_billing table.
@Entity(tableName = "ledger_card_billing")
data class LedgerCardBillingEntity(
    @PrimaryKey val wallet: String,
    val cutoffDay: Int,
    val dueDay: Int,
    val minPercent: Double = 5.0,
    val minFullInstallments: Boolean = false,
)
