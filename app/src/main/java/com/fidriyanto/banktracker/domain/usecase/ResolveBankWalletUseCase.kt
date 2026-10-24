package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.BankParserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ResolveBankWalletUseCase @Inject constructor(
    private val bankParsers: BankParserRepository,
    private val accounts: AccountRepository,
) {
    // A notification read by a bank pattern goes to the account the user chose for that bank. A bank the
    // user never set keeps its earlier behavior: it goes to the account with the bank's own id, when one
    // exists. Null means the bank is off or has no account, and the notification is not saved.
    // A notification the on-device model read has no bank and passes through.
    suspend operator fun invoke(parsed: ParsedTransaction): ParsedTransaction? {
        val bank = parsed.rawFields["bank"] ?: return parsed
        val ids = accounts.observeAll().first().map { it.id }.toSet()
        val wallet = when (val chosen = bankParsers.observe().first()[bank]) {
            null -> parsed.wallet.takeIf { it in ids }
            "" -> null
            else -> chosen.takeIf { it in ids }
        }
        return wallet?.let { parsed.copy(wallet = it) }
    }
}
