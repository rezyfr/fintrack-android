package com.fidriyanto.banktracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryCacheEntity::class,
        ProcessedRefEntity::class,
        MonthlyOverviewEntity::class,
        MonthlyBudgetEntity::class
    ],
    version = 4
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryCacheDao(): CategoryCacheDao
    abstract fun processedRefDao(): ProcessedRefDao
    abstract fun monthlyOverviewDao(): MonthlyOverviewDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `processed_refs`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `processed_refs` " +
                    "(`compositeKey` TEXT NOT NULL, `processedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`compositeKey`))"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN wallet TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN txType TEXT NOT NULL DEFAULT 'expense'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN toWallet TEXT")
            }
        }
    }
}
