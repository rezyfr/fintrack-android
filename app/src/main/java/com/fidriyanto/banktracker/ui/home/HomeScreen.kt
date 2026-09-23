package com.fidriyanto.banktracker.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.CardStatement
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private fun rp(a: Double) = "Rp ${NumberFormat.getNumberInstance(Locale.US).format(kotlin.math.abs(a).roundToLong())}"
private fun walletName(w: String) = when (w) {
    "MANDIRI_CC" -> "Mandiri CC"; "BCA_CC" -> "BCA CC"; else -> w
}
private fun shortDate(iso: String) = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
}.getOrDefault(iso)

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val app = LocalAppColors.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(stringResource(R.string.home_greeting), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                if (s.cycleFromIso.isNotEmpty()) {
                    Text(stringResource(R.string.home_cycle_range, shortDate(s.cycleFromIso), shortDate(s.cycleToIso)),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(999.dp)) {
                Text(
                    if (s.daysToPayday == 0) stringResource(R.string.home_payday_today)
                    else stringResource(R.string.home_payday_in, s.daysToPayday),
                    color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        if (s.loading) {
            Text(stringResource(R.string.home_greeting), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        // Hero: net this cycle
        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.home_net_this_cycle), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f), fontSize = 13.sp)
                Text((if (s.net >= 0) "+" else "-") + rp(s.net), color = MaterialTheme.colorScheme.onPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    HeroStat(stringResource(R.string.home_income), rp(s.income))
                    HeroStat(stringResource(R.string.home_spent), rp(s.expenses))
                }
            }
        }

        // ac: home-cycle-overview — each credit card's minimum payment and due date
        // Cards due
        if (s.cards.isNotEmpty()) {
            SectionLabel(stringResource(R.string.home_cards_due))
            s.cards.forEach { CardDueRow(it, app.red, app.warning) }
        }

        // ac: home-cycle-overview — the most recent transactions in the cycle
        // Recent
        SectionLabel(stringResource(R.string.home_recent))
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column {
                if (s.recent.isEmpty()) {
                    Text(stringResource(R.string.home_recent_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                } else {
                    s.recent.forEachIndexed { i, tx -> RecentRow(tx, app.red, app.green, showDivider = i < s.recent.lastIndex) }
                }
            }
        }
    }
}

@Composable private fun HeroStat(label: String, value: String) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 11.sp)
        Text(value, color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
}

@Composable private fun CardDueRow(c: CardStatement, red: Color, warning: Color) {
    val pillColor = when { c.daysUntilDue < 0 -> red; c.daysUntilDue <= 3 -> warning; else -> MaterialTheme.colorScheme.primary }
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(walletName(c.wallet), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.home_min_due, rp(c.minimum), shortDate(c.dueIso)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (c.daysUntilDue < 0) "${-c.daysUntilDue}d late" else "${c.daysUntilDue}d",
                color = pillColor, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable private fun RecentRow(tx: TransactionUiModel, red: Color, green: Color, showDivider: Boolean) {
    Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(tx.item, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(stringResource(R.string.home_recent_meta, tx.category, shortDate(tx.dateIso)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val income = tx.txType == "income"
        Text((if (income) "+" else "-") + rp(tx.amount), color = if (income) green else red, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
    if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
