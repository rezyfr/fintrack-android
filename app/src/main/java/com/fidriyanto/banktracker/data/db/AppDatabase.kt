package com.fidriyanto.banktracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        ProcessedRefEntity::class,
        MonthlyOverviewEntity::class,
        MonthlyBudgetEntity::class
    ],
    version = 7
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE monthly_overview ADD COLUMN family REAL NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE transactions_new (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `merchant` TEXT NOT NULL,
                        `item` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `dateIso` TEXT NOT NULL,
                        `referenceNo` TEXT NOT NULL,
                        `tab` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `wallet` TEXT,
                        `txType` TEXT NOT NULL DEFAULT 'expense',
                        `toWallet` TEXT
                    )
                """)
                db.execSQL("""
                    INSERT INTO transactions_new
                    SELECT id, merchant, item, amount, category, dateIso, referenceNo, tab, status, createdAt, wallet, txType, toWallet
                    FROM transactions
                """)
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `category_cache`")
            }
        }
    }
}
