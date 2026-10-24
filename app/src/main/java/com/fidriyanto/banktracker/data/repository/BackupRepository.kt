package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.BackupOutcome

// A backup file of an offline install. uri is the text of the content Uri the user picked.
interface BackupRepository {
    suspend fun export(uri: String): BackupOutcome

    // Replaces the data on the phone with the data in the file.
    suspend fun restore(uri: String): BackupOutcome
}
