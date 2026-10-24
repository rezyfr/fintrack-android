package com.fidriyanto.banktracker.domain.model

// A bank whose notifications the app reads with its own pattern. To add a bank, add an entry here
// and a pattern in NotificationParser. The user then picks the account that bank's payments go to.
enum class KnownBank(val id: String, val label: String) {
    BCA("BCA", "BCA (myBCA)"),
    BBL("BBL", "Bangkok Bank (Bualuang)"),
}
