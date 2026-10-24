package com.fidriyanto.banktracker.ui.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

// ac: book-switch-sheet — which pages are still loading for the book they show. A page reports its own
// flag, and the sheet waits for the page in view. The keys are the tab routes.
object BookLoad {
    const val HOME = "home"
    const val BUDGET = "budget"

    private val _busy = MutableStateFlow<Set<String>>(emptySet())
    val busy: StateFlow<Set<String>> = _busy.asStateFlow()

    fun report(key: String, loading: Boolean) {
        _busy.update { if (loading) it + key else it - key }
    }

    // Waits until the page is idle. Returns true when it got idle, and false when the timeout came
    // first, so a slow network never traps the screen.
    suspend fun awaitIdle(key: String, timeoutMs: Long): Boolean =
        withTimeoutOrNull(timeoutMs) {
            busy.first { key !in it }
            true
        } ?: false
}

// ac: book-switch-sheet — a tap on the book switch is a request. The book changes at once. A sheet rises
// only if the page behind it is still loading after a short moment, and leaves once it has its data.
object BookTransition {
    private val _requested = MutableStateFlow(Book.COMBINED)
    val requested: StateFlow<Book> = _requested.asStateFlow()

    fun request(book: Book) {
        _requested.value = book
    }
}
