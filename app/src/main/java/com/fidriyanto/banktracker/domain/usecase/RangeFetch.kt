package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// ac: insights-six-month-trend — each date range is fetched on its own and in parallel. A range that
// appears twice is fetched once. A failed fetch gives no rows.
// ac: insights-wallet-flow — with a wallet, the rows include transfers into that wallet.
internal suspend fun TransactionRepository.fetchRanges(
    ranges: List<Pair<String, String>>,
    wallet: String?,
): Map<Pair<String, String>, List<TransactionUiModel>> = coroutineScope {
    ranges.distinct().associateWith { (from, to) ->
        async { fetch(month = null, wallet = null, txType = null, dateFrom = from, dateTo = to, involvingWallet = wallet).getOrElse { emptyList() } }
    }.mapValues { it.value.await() }
}
