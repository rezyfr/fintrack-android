package com.fidriyanto.banktracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TransactionEntity::class,
        CategoryCacheEntity::class,
        ProcessedRefEntity::class,
        MonthlyOverviewEntity::class,
        MonthlyBudgetEntity::class
    ],
    version = 2
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryCacheDao(): CategoryCacheDao
    abstract fun processedRefDao(): ProcessedRefDao
    abstract fun monthlyOverviewDao(): MonthlyOverviewDao
}
