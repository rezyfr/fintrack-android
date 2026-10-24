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
        AccountEntity::class,
        CategoryEntity::class,
        SubcategoryEntity::class,
        PendingInstallmentStepEntity::class,
        AutoAddedEntity::class,
        LedgerTransactionEntity::class,
        LedgerInstallmentEntity::class,
        LedgerBudgetLineEntity::class,
        LedgerCardBillingEntity::class,
    ],
    version = 19
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun processedRefDao(): ProcessedRefDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun pendingInstallmentStepDao(): PendingInstallmentStepDao
    abstract fun autoAddedDao(): AutoAddedDao
    abstract fun ledgerDao(): LedgerDao

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

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE transactions_new (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
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
                    SELECT id, item, amount, category, dateIso, referenceNo, tab, status, createdAt, wallet, txType, toWallet
                    FROM transactions
                """)
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN subcategory TEXT")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN toAmount REAL")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `name` TEXT NOT NULL PRIMARY KEY,
                        `sortOrder` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `subcategories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `category` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL
                    )
                """)
            }
        }

        // ac: installment-link-step-retry — the queue of installment step updates that failed to send
        // The monthly overview and monthly budget tables belonged to a screen that was never reachable.
        // ac: account-opening-balance — what an account held before its first transaction
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `openingBalance` REAL NOT NULL DEFAULT 0")
            }
        }

        // ac: notification-log - the list of notifications the listener saw and what became of each
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notification_log` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `postedAtMs` INTEGER NOT NULL, " +
                        "`packageName` TEXT NOT NULL, `title` TEXT, `text` TEXT, `outcome` TEXT NOT NULL)"
                )
            }
        }

        // ac: auto-added-notice - the transactions the app added by itself, for the notice on Home
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `auto_added` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `addedAtMs` INTEGER NOT NULL, " +
                        "`item` TEXT NOT NULL, `amount` REAL NOT NULL, `txType` TEXT NOT NULL, `wallet` TEXT, " +
                        "`source` TEXT NOT NULL, `dismissed` INTEGER NOT NULL)"
                )
            }
        }

        // The notification log was removed, so its table goes
        // ac: offline-mode - the offline store. The statements are the ones Room generates for the four
        // ledger entities, so the migrated tables match a new install exactly.
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                LEDGER_TABLES_SQL.forEach { db.execSQL(it) }
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `notification_log`")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `monthly_overview`")
                db.execSQL("DROP TABLE IF EXISTS `monthly_budget`")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `type` TEXT NOT NULL DEFAULT 'checking'")
                // An existing install keeps its behavior until the next refresh reads the type from the server:
                // a *_CC account is a credit card and INVESTMENT is the investment pool.
                db.execSQL("UPDATE `accounts` SET `type` = 'credit' WHERE `id` LIKE '%\\_CC' ESCAPE '\\'")
                db.execSQL("UPDATE `accounts` SET `type` = 'investment' WHERE `id` = 'INVESTMENT'")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_installment_steps` " +
                        "(`installmentId` INTEGER NOT NULL, `newStep` INTEGER NOT NULL, " +
                        "`newStatus` TEXT NOT NULL, PRIMARY KEY(`installmentId`))"
                )
            }
        }
    }
}
