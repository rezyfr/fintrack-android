package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_refs")
data class ProcessedRefEntity(
    @PrimaryKey val referenceNo: String,
    val processedAt: Long = System.currentTimeMillis()
)
