package com.fidriyanto.banktracker.domain.usecase

// An account id made from its name, for example "My Bank" gives "MY_BANK". Transactions keep pointing
// to the id when the account is renamed. [taken] holds ids already in use, and a clash gets a number.
fun accountIdFor(name: String, taken: Set<String> = emptySet()): String {
    val base = name.trim().uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_').ifEmpty { "ACCOUNT" }
    if (base !in taken) return base
    return generateSequence(2) { it + 1 }.map { "${base}_$it" }.first { it !in taken }
}
