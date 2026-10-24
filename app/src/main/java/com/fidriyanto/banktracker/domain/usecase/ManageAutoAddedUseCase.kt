package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AutoAddedRepository
import com.fidriyanto.banktracker.domain.model.AutoAddedItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ac: auto-added-notice - the notes that the user has not dismissed yet, newest first, and the dismiss action
class ManageAutoAddedUseCase @Inject constructor(
    private val autoAdded: AutoAddedRepository,
) {
    fun observe(): Flow<List<AutoAddedItem>> = autoAdded.observeActive()
    suspend fun dismissAll() = autoAdded.dismissAll()
}
