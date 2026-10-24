package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currency: String,
    val sortOrder: Int,
    val isDefault: Boolean,
    val type: String = "checking",
    val openingBalance: Double = 0.0,
)
