package com.fidriyanto.banktracker.ui.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ac: currency-books - the app keeps one book for each currency in use. COMBINED is the summary of all of
// them in the home currency. A book is COMBINED or a currency code. One switch at the top of the app
// picks the book, and every screen follows it. The app opens on COMBINED.
data class Book(val code: String?) {
    val isCombined: Boolean get() = code == null

    companion object {
        val COMBINED = Book(null)
    }
}

object BookStore {
    private val _book = MutableStateFlow(Book.COMBINED)
    val book: StateFlow<Book> = _book.asStateFlow()

    fun select(book: Book) {
        _book.value = book
    }
}

// The currency of a book, or null for COMBINED.
fun Book.currencyCode(): String? = code

// True when the wallet belongs to this book. COMBINED holds every wallet.
fun Book.holdsWallet(wallet: String?): Boolean = code?.let { AccountStore.currencyOf(wallet) == it } ?: true

// A transaction belongs to a book when it leaves or reaches one of the book's wallets, so a transfer
// between two currencies shows in both books.
fun Book.holdsTransaction(wallet: String?, toWallet: String?): Boolean =
    holdsWallet(wallet) || (toWallet != null && holdsWallet(toWallet))
