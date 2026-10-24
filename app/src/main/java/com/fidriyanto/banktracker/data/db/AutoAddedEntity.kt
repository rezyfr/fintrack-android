package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// ac: auto-added-notice - a transaction the app added by itself: a schedule posted it or a bank notification made it
@Entity(tableName = "auto_added")
data class AutoAddedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val addedAtMs: Long,
    val item: String,
    val amount: Double,
    val txType: String,
    val wallet: String?,
    val source: String,
    val dismissed: Boolean = false,
)
