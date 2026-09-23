package com.fidriyanto.banktracker.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

private fun walletName(wallet: String): String = when (wallet) {
    "MANDIRI_CC" -> "Mandiri Credit Card"
    "BCA_CC"     -> "BCA Credit Card"
    else         -> wallet
}

private fun rp(amount: Double): String =
    "Rp ${NumberFormat.getNumberInstance(Locale.US).format(amount.roundToLong())}"

private fun prettyDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))
}.getOrDefault(iso)

@Composable
fun CardsScreen(viewModel: CardsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            state.loading -> Text(stringResource(R.string.cards_title), color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.error != null -> Text(stringResource(R.string.cards_error), color = MaterialTheme.colorScheme.error)
            state.statements.isEmpty() -> Text(stringResource(R.string.cards_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> state.statements.forEach { StatementCard(it) }
        }
    }
}

@Composable
private fun StatementCard(s: CardStatement) {
    val app = LocalAppColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(walletName(s.wallet), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                DuePill(s.daysUntilDue)
            }
            Text(stringResource(R.string.cards_statement_closed, prettyDate(s.cutoffIso)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            AmountRow(stringResource(R.string.cards_balance), rp(s.balance), MaterialTheme.colorScheme.onSurface)
            AmountRow(stringResource(R.string.cards_minimum), rp(s.minimum), app.red)
            AmountRow(stringResource(R.string.cards_due), prettyDate(s.dueIso), MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
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
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(999.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
    }
}
