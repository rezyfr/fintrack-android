package com.fidriyanto.banktracker.domain.model

// ac: custom-bank-patterns - a notification the user taught the app to read. [template] is the text of one
// example notification with blanks: {amount} once and {merchant} at most once. [titleContains] limits the
// pattern to notifications whose title holds that text. [exampleTitle] and [exampleText] are the notification the
// user marked, kept so the editor can show the marks again.
data class CustomBankPattern(
    val id: String,
    val name: String,
    val titleContains: String? = null,
    val template: String,
    val walletId: String,
    val txType: String = "expense",
    val enabled: Boolean = true,
    val exampleTitle: String = "",
    val exampleText: String = "",
)
