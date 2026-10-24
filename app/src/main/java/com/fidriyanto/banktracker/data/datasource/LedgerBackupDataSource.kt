package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.model.LedgerBackup

// The whole offline store at once, for a backup.
interface LedgerBackupDataSource {
    suspend fun read(): LedgerBackup

    // Deletes every row of the offline store and writes the rows of the backup, in one transaction.
    suspend fun replace(backup: LedgerBackup)
}
