package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.DashboardRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDashboardDataUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    fun observeForMonths(months: List<String>) = repository.observeForMonths(months)
    fun observeTransportBreakdown(fromDate: String, toDate: String) =
        repository.observeTransportBreakdown(fromDate, toDate)
    suspend fun refresh(months: List<String>) = repository.refresh(months)
}
