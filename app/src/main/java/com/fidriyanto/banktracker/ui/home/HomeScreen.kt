package com.fidriyanto.banktracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.usecase.BudgetGlance
import com.fidriyanto.banktracker.domain.model.CardStatement
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.ui.theme.Fraunces
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private fun rp(a: Double) = "Rp ${NumberFormat.getNumberInstance(Locale.US).format(kotlin.math.abs(a).roundToLong())}"
private fun thb(a: Double): String {
    val n = NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 0; maximumFractionDigits = 0 }.format(kotlin.math.abs(a).roundToLong())
    return "฿$n"
}
private fun walletName(w: String) = when (w) {
    "MANDIRI_CC" -> "Mandiri CC"; "BCA_CC" -> "BCA CC"; else -> w
}
private fun shortDate(iso: String) = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
}.getOrDefault(iso)

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    // ac: home-cycle-overview — recompute the cycle and payday countdown each time the screen resumes,
    // so the overview rolls into the new cycle when the date changes while the app stays open.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    val app = LocalAppColors.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(stringResource(R.string.home_greeting), fontFamily = Fraunces, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
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

        // Hero: cycle summary, split by currency, styled like the mockup
        Hero(s)

        // ac: home-cycle-overview — a compact budget card with remaining, daily allowance, top lines
        s.budget?.let { b ->
            if (b.lines.isNotEmpty()) {
                SectionLabel(stringResource(R.string.home_budget))
                BudgetCard(b, app.red)
            }
        }

        // ac: home-cycle-overview — each credit card's minimum payment and due date
        // Cards due (two-column compact cards)
        if (s.cards.isNotEmpty()) {
            SectionLabel(stringResource(R.string.home_cards_due))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                s.cards.forEach { CardDueCard(it, Modifier.weight(1f), app.red, app.warning) }
                if (s.cards.size == 1) Spacer(Modifier.weight(1f))
            }
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

// ac: android-budget-daily-allowance — Home compact budget card: remaining, daily allowance, top lines
@Composable private fun BudgetCard(b: BudgetGlance, red: Color) {
    val over = b.totalSpent > b.totalTarget
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(stringResource(R.string.budget_remaining), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rp(b.remaining), fontFamily = Fraunces, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    if (over) stringResource(R.string.budget_over_total, rp(b.totalSpent - b.totalTarget))
                    else stringResource(R.string.budget_safe, rp(b.dailyAllowance)),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (over) red else MaterialTheme.colorScheme.primary,
                )
            }
            b.lines.take(3).forEach { ls ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ls.line.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.budget_line_remaining, rp(ls.remaining)), fontSize = 12.sp,
                            color = if (ls.isOver) red else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { ls.progress },
                        color = if (ls.isOver) red else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                    )
                }
            }
        }
    }
}

@Composable private fun Hero(s: HomeUiState) {
    val hero = LocalAppColors.current
    val onPine = hero.heroOn
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(hero.heroBg),
    ) {
        // Soft highlight in the top-right corner, the card's only "empty" space.
        Box(Modifier.align(Alignment.TopEnd).offset(x = 44.dp, y = (-44).dp).size(150.dp).clip(CircleShape).background(onPine.copy(alpha = 0.07f)))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_net_this_cycle), color = onPine.copy(alpha = 0.85f), fontSize = 13.sp)
                Text(
                    if (s.daysToPayday == 0) stringResource(R.string.home_payday_today) else stringResource(R.string.home_payday_in, s.daysToPayday),
                    color = onPine, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            HeroCurrency(stringResource(R.string.home_cycle_thb), s.thbIn, s.thbOut, ::thb, onPine)
            HorizontalDivider(color = onPine.copy(alpha = 0.15f))
            HeroCurrency(stringResource(R.string.home_cycle_idr), s.idrIn, s.idrOut, ::rp, onPine)
            // cycle-elapsed bar + foot
            val frac = cycleElapsed(s.cycleFromIso, s.cycleToIso)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(onPine.copy(alpha = 0.18f))) {
                    Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(999.dp)).background(onPine))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cycleDaysLabel(s.cycleFromIso, s.cycleToIso), color = onPine.copy(alpha = 0.8f), fontSize = 11.sp)
                    Text(paydayDateLabel(s.cycleToIso), color = onPine.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable private fun HeroCurrency(label: String, income: Double, expenses: Double, fmt: (Double) -> String, onPine: Color) {
    val net = income - expenses
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = onPine.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text((if (net >= 0) "+" else "-") + fmt(net), color = onPine, fontFamily = Fraunces, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = 2.dp)) {
            HeroStat(stringResource(R.string.home_income), fmt(income), onPine)
            HeroStat(stringResource(R.string.home_spent), fmt(expenses), onPine)
        }
    }
}

@Composable private fun HeroStat(label: String, value: String, onPine: Color) {
    Column {
        Text(label, color = onPine.copy(alpha = 0.75f), fontSize = 11.sp)
        Text(value, color = onPine, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun cycleSpan(fromIso: String, toIso: String): Triple<Long, Long, Long> {
    val from = runCatching { LocalDate.parse(fromIso) }.getOrNull()
    val to = runCatching { LocalDate.parse(toIso) }.getOrNull()
    if (from == null || to == null) return Triple(0, 1, 1)
    val today = LocalDate.now()
    val total = (to.toEpochDay() - from.toEpochDay()) + 1
    val elapsed = (today.toEpochDay() - from.toEpochDay()) + 1
    return Triple(elapsed.coerceIn(0, total), total, total)
}

private fun cycleElapsed(fromIso: String, toIso: String): Float {
    val (elapsed, total, _) = cycleSpan(fromIso, toIso)
    return if (total > 0) (elapsed.toFloat() / total).coerceIn(0f, 1f) else 0f
}

private fun cycleDaysLabel(fromIso: String, toIso: String): String {
    val (elapsed, total, _) = cycleSpan(fromIso, toIso)
    return "$elapsed of $total days"
}

private fun paydayDateLabel(toIso: String): String = runCatching {
    "Payday " + LocalDate.parse(toIso).plusDays(1).format(DateTimeFormatter.ofPattern("d MMM", java.util.Locale.US))
}.getOrDefault("")

@Composable private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
}

@Composable private fun CardDueCard(c: CardStatement, modifier: Modifier, red: Color, warning: Color) {
    val pillColor = when { c.daysUntilDue < 0 -> red; c.daysUntilDue <= 3 -> warning; else -> MaterialTheme.colorScheme.primary }
    val pillText = when {
        c.daysUntilDue < 0 -> stringResource(R.string.home_due_late, -c.daysUntilDue)
        c.daysUntilDue == 0 -> stringResource(R.string.home_payday_today)
        else -> stringResource(R.string.home_due_in, c.daysUntilDue)
    }
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(walletName(c.wallet), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(rp(c.minimum), fontFamily = Fraunces, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.home_min_due_short, shortDate(c.dueIso)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(color = pillColor.copy(alpha = 0.14f), shape = RoundedCornerShape(999.dp)) {
                Text(pillText, color = pillColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
            }
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
