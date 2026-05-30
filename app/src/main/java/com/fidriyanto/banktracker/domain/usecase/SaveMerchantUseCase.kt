package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.MerchantHistoryRepository
import javax.inject.Inject

class SaveMerchantUseCase @Inject constructor(
    private val repository: MerchantHistoryRepository,
) {
    suspend operator fun invoke(merchant: String) = repository.save(merchant)
}
