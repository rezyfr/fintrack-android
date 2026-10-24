package com.fidriyanto.banktracker.domain.usecase

// ac: manage-accounts — currency-by-wallet is supplied by the caller (sourced from the accounts
// list) instead of a hardcoded map, so a custom account's currency is resolved correctly too
// ac: configurable-currency-pair — an unrecognized wallet falls back to the configured home
// currency instead of a hardcoded "IDR" literal
internal fun walletCurrencyOf(wallet: String?, currencyByWallet: Map<String, String>, homeCode: String): String =
    currencyByWallet[wallet] ?: homeCode
