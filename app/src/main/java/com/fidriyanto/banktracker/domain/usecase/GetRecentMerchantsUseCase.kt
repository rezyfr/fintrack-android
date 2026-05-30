package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.MerchantHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentMerchantsUseCase @Inject constructor(
    private val repository: MerchantHistoryRepository,
) {
    operator fun invoke(): Flow<List<String>> = repository.observe()
}
