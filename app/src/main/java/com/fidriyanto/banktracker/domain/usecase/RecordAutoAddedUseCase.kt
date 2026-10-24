package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AutoAddedRepository
import com.fidriyanto.banktracker.domain.model.AutoSource
import javax.inject.Inject

private const val KEEP_MS = 30L * 24 * 60 * 60 * 1000

// ac: auto-added-notice - notes a transaction the app added by itself, so Home can tell the user. Notes older than 30 days go.
class RecordAutoAddedUseCase @Inject constructor(
    private val autoAdded: AutoAddedRepository,
) {
    suspend operator fun invoke(item: String, amount: Double, txType: String, wallet: String?, source: AutoSource) {
        autoAdded.prune(System.currentTimeMillis() - KEEP_MS)
        autoAdded.add(item, amount, txType, wallet, source)
    }
}
