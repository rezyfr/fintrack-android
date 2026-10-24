package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.BackupRepository
import com.fidriyanto.banktracker.data.repository.StorageRepository
import com.fidriyanto.banktracker.domain.model.BackupOutcome
import com.fidriyanto.banktracker.domain.model.StorageMode
import java.time.LocalDate
import javax.inject.Inject

// ac: offline-backup - an offline install keeps its data only on the phone, so the user exports it to a
// file and imports it again on a new phone. A server install has its data on the server and needs no file.
class BackupUseCase @Inject constructor(
    private val backup: BackupRepository,
    private val storage: StorageRepository,
) {
    suspend fun available(): Boolean = storage.get().mode == StorageMode.LOCAL

    suspend fun export(uri: String): BackupOutcome = backup.export(uri)

    suspend fun restore(uri: String): BackupOutcome = backup.restore(uri)

    // The name the file picker suggests, for example fintrack-backup-2026-10-24.json
    fun fileName(today: LocalDate = LocalDate.now()): String = "fintrack-backup-$today.json"
}
