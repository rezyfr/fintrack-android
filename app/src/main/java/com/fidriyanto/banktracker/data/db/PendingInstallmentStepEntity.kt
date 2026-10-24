package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// ac: installment-link-step-retry — an installment step update that failed to reach the server. One row
// per installment holds the highest step waiting to be sent.
@Entity(tableName = "pending_installment_steps")
data class PendingInstallmentStepEntity(
    @PrimaryKey val installmentId: Long,
    val newStep: Int,
    val newStatus: String,
)
