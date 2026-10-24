package com.fidriyanto.banktracker.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.currenciesInUse

// ac: currency-books - the one switch for the whole app: Combined, then one book for each currency in use,
// the home currency first, named by the labels from Settings
@Composable
fun BookSwitch(modifier: Modifier = Modifier) {
    // ac: book-switch-transition — the chip shows the requested book at once. The pages follow when the
    // transition has swapped the book, so a tap is never ignored and the old page does not change early.
    val book by BookTransition.requested.collectAsStateWithLifecycle()
    val settings = CurrencySettingsStore.current
    val currencies = currenciesInUse(settings.homeCode, AccountStore.all)
    // A book whose currency is no longer in use falls back to Combined.
    LaunchedEffect(currencies, book) { if (book.code != null && book.code !in currencies) BookTransition.request(Book.COMBINED) }
    // One currency means one book. The switch has nothing to choose, so it is not shown.
    if (currencies.size < 2) {
        Spacer(modifier)
        return
    }
    val options = listOf(Book.COMBINED to stringResource(R.string.currency_combined)) +
        currencies.map { Book(it) to settings.labelFor(it) }
    CurrencyToggle(
        options = options,
        selected = book,
        onSelect = BookTransition::request,
        tagFor = { "book_" + (it.code ?: "combined") },
        modifier = modifier,
    )
}
