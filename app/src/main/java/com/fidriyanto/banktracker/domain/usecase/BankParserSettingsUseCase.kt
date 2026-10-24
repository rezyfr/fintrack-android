package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.BankParserRepository
import com.fidriyanto.banktracker.domain.model.KnownBank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

// The account each known bank's notifications go to. A null account means the bank is off.
data class BankSetting(val bank: KnownBank, val accountId: String?)

class BankParserSettingsUseCase @Inject constructor(
    private val bankParsers: BankParserRepository,
    private val accounts: AccountRepository,
) {
    // A bank the user never set shows the account with its own id, like the parser does.
    fun observe(): Flow<List<BankSetting>> = combine(bankParsers.observe(), accounts.observeAll()) { chosen, all ->
        val ids = all.map { it.id }.toSet()
        KnownBank.entries.map { b ->
            BankSetting(b, chosen[b.id]?.takeIf { it.isNotEmpty() && it in ids } ?: b.id.takeIf { chosen[b.id] == null && it in ids })
        }
    }

    suspend fun choose(bank: KnownBank, accountId: String?) = bankParsers.save(bank.id, accountId.orEmpty())
}
