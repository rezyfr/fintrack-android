package com.fidriyanto.banktracker.data.db

// ac: offline-mode - the tables of the offline store, copied from the code Room generates for the ledger
// entities. MIGRATION_18_19 runs them, so an updated phone gets the same tables as a new install.
// Change them together with the entities.
internal val LEDGER_TABLES_SQL: List<String> = listOf(
    "CREATE TABLE IF NOT EXISTS `ledger_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`item` TEXT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `subcategory` TEXT, " +
        "`date` TEXT NOT NULL, `tab` TEXT NOT NULL, `note` TEXT, `wallet` TEXT NOT NULL, `txType` TEXT NOT NULL, " +
        "`toWallet` TEXT, `toAmount` REAL, `budgetLineId` INTEGER, `installmentId` INTEGER, `createdAt` INTEGER NOT NULL)",
    "CREATE INDEX IF NOT EXISTS `index_ledger_transactions_date` ON `ledger_transactions` (`date`)",
    "CREATE INDEX IF NOT EXISTS `index_ledger_transactions_wallet` ON `ledger_transactions` (`wallet`)",
    "CREATE INDEX IF NOT EXISTS `index_ledger_transactions_toWallet` ON `ledger_transactions` (`toWallet`)",
    "CREATE INDEX IF NOT EXISTS `index_ledger_transactions_installmentId` ON `ledger_transactions` (`installmentId`)",
    "CREATE TABLE IF NOT EXISTS `ledger_installments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`merchant` TEXT NOT NULL, `merchantPattern` TEXT NOT NULL, `wallet` TEXT NOT NULL, " +
        "`installmentAmount` REAL NOT NULL, `totalInstallments` INTEGER NOT NULL, `currentStep` INTEGER NOT NULL, " +
        "`dueDay` INTEGER NOT NULL, `status` TEXT NOT NULL, `excluded` INTEGER NOT NULL)",
    "CREATE TABLE IF NOT EXISTS `ledger_budget_lines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`name` TEXT NOT NULL, `kind` TEXT NOT NULL, `currency` TEXT NOT NULL, `target` REAL NOT NULL, " +
        "`matchWallets` TEXT, `matchPattern` TEXT, `matchCategories` TEXT, `sortOrder` INTEGER NOT NULL, " +
        "`active` INTEGER NOT NULL, `category` TEXT, `subcategory` TEXT, `dueDay` INTEGER)",
    "CREATE TABLE IF NOT EXISTS `ledger_card_billing` (`wallet` TEXT NOT NULL, `cutoffDay` INTEGER NOT NULL, " +
        "`dueDay` INTEGER NOT NULL, `minPercent` REAL NOT NULL, `minFullInstallments` INTEGER NOT NULL, " +
        "PRIMARY KEY(`wallet`))",
)
