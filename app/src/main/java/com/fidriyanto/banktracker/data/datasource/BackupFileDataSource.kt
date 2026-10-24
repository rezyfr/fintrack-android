package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.model.BackupDocument

// Reads and writes a backup file that the user picked. uri is the text of the content Uri.
interface BackupFileDataSource {
    suspend fun write(uri: String, document: BackupDocument)
    suspend fun read(uri: String): BackupDocument
}
