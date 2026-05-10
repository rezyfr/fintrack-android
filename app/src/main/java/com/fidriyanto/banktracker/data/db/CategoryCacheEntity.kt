package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_cache")
data class CategoryCacheEntity(
    @PrimaryKey val merchantKey: String,
    val category: String,
    val cleanDescription: String
)
