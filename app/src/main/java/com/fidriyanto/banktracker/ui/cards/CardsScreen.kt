package com.fidriyanto.banktracker.ui.cards

import com.fidriyanto.banktracker.ui.common.holdsWallet
import com.fidriyanto.banktracker.ui.common.BookStore
import com.fidriyanto.banktracker.ui.common.LoadingIndicator
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.common.LocalFloatingToolbarInset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.CardStatement
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space
import com.fidriyanto.banktracker.ui.common.countUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import com.fidriyanto.banktracker.ui.common.EmptyState

private fun walletName(wallet: String): String = AccountStore.nameOf(wallet)

private fun prettyDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))
}.getOrDefault(iso)

@Composable
fun CardsScreen(viewModel: CardsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // ac: currency-books — only the cards of the book picked at the top of the app
    val book by BookStore.book.collectAsStateWithLifecycle()
    val statements = state.statements.filter { book.holdsWallet(it.wallet) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.lg).padding(bottom = LocalFloatingToolbarInset.current),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        when {
            // ac: loading-states-for-fetches — a spinner while the card statements load
            state.loading -> LoadingIndicator(Modifier.fillMaxWidth().padding(vertical = Space.xxl))
            state.error != null -> Text(stringResource(R.string.cards_error), color = MaterialTheme.colorScheme.error)
            // ac: empty-states — an empty screen says what it is for and what to do next
            state.statements.isEmpty() -> EmptyState(
                icon = Icons.Outlined.CreditCard,
                title = stringResource(R.string.cards_empty),
                body = stringResource(R.string.cards_empty_body),
            )
            statements.isEmpty() -> EmptyState(
                icon = Icons.Outlined.CreditCard,
                title = stringResource(R.string.book_no_cards),
                body = stringResource(R.string.book_no_cards_body),
            )
            else -> statements.forEach { StatementCard(it) }
        }
    }
}

@Composable
// ac: multicurrency-money-formatting — card amounts use the card wallet currency
private fun StatementCard(s: CardStatement) {
    val app = LocalAppColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = AppShape.lg,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(walletName(s.wallet), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                DuePill(s.daysUntilDue)
            }
            Text(stringResource(R.string.cards_statement_closed, prettyDate(s.cutoffIso)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            AmountRow(stringResource(R.string.cards_balance), formatMoney(AccountStore.currencyOf(s.wallet), countUp(s.balance)), MaterialTheme.colorScheme.onSurface)
            AmountRow(stringResource(R.string.cards_minimum), formatMoney(AccountStore.currencyOf(s.wallet), countUp(s.minimum)), app.red)
            AmountRow(stringResource(R.string.cards_due), prettyDate(s.dueIso), MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}

@Composable
private fun DuePill(daysLeft: Int) {
    val app = LocalAppColors.current
    val text = when {
        daysLeft < 0 -> stringResource(R.string.cards_overdue, -daysLeft)
        daysLeft == 0 -> stringResource(R.string.cards_due_today)
        else -> stringResource(R.string.cards_due_in, daysLeft)
    }
    val color = when {
        daysLeft < 0 -> app.red
        daysLeft <= 3 -> app.warning
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(color = color.copy(alpha = 0.14f), shape = AppShape.pill) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs))
    }
}
