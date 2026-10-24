package com.fidriyanto.banktracker.ui.budgetglance

import com.fidriyanto.banktracker.ui.common.SpendCompareChart
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.horizontalScroll
import com.fidriyanto.banktracker.ui.common.LoadingIndicator
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.common.LocalFloatingToolbarInset
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.BudgetLineSpend
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.CycleStepper
import com.fidriyanto.banktracker.domain.usecase.PayCycle
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space
import com.fidriyanto.banktracker.ui.common.countUp
import com.fidriyanto.banktracker.ui.common.grownFraction
import com.fidriyanto.banktracker.ui.common.rememberTick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Savings
import com.fidriyanto.banktracker.ui.common.EmptyState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import com.fidriyanto.banktracker.ui.common.heroGlow
import androidx.compose.foundation.layout.BoxWithConstraints
import com.fidriyanto.banktracker.ui.common.rememberFittedStyle
import com.fidriyanto.banktracker.ui.theme.categoryColor

private fun shortDate(iso: String): String = runCatching {
    LocalDate.parse(iso.take(10)).format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
}.getOrDefault(iso)

@Composable
fun BudgetGlanceScreen(viewModel: BudgetGlanceViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    // ac: budget-glance-reloads-on-resume — refetch every time the Budget tab resumes, so an
    // out-of-band change to a budget line (kind, target, pattern) shows without restarting the app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    Column(Modifier.fillMaxSize()) {
        // ac: budget-cycle-stepper — the same pay-cycle stepper as Insights. It stays at the top while the page scrolls.
        val cycle = PayCycle.cycleRange(s.cycleOffset)
        Box(Modifier.padding(start = Space.lg, top = Space.lg, end = Space.lg, bottom = Space.xs)) {
            CycleStepper(cycle.fromIso, cycle.toIso, canGoNext = s.cycleOffset > 0, onStep = viewModel::stepCycle)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg).padding(bottom = Space.lg + LocalFloatingToolbarInset.current),
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            when {
                s.glance != null && s.glance!!.lines.isNotEmpty() -> GlanceBody(s.glance!!)
                // ac: loading-states-for-fetches — a spinner while the budget lines load
                s.loading -> LoadingIndicator(Modifier.fillMaxWidth().padding(vertical = Space.xxl))
                s.error != null -> Text(stringResource(R.string.budget_error), color = MaterialTheme.colorScheme.error)
                // ac: empty-states — an empty screen says what it is for and what to do next
                else -> EmptyState(
                    icon = Icons.Outlined.Savings,
                    title = stringResource(R.string.budget_empty),
                    body = stringResource(R.string.budget_empty_body),
                )
            }
        }
    }
}

@Composable
private fun BudgetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val tick = rememberTick()
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = AppShape.pill,
        modifier = Modifier.clip(AppShape.pill),
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall, maxLines = 1,
            modifier = Modifier.clickable { tick(); onClick() }.padding(horizontal = Space.lg, vertical = Space.sm),
        )
    }
}

@Composable
private fun GlanceBody(g: com.fidriyanto.banktracker.domain.usecase.BudgetGlance) {
    val app = LocalAppColors.current
    val over = g.totalSpent > g.totalTarget
    // ac: budget-line-details-and-assignment — one expanded line at a time; tapping a line toggles it
    var expandedId by remember { mutableStateOf<Long?>(null) }
    fun toggle(id: Long) { expandedId = if (expandedId == id) null else id }
    // ac: android-budget-daily-allowance — flex total/spent/remaining and a safe daily allowance
    // Ring + remaining
    // ac: budget-hero — the Budget tab opens on a pine hero, the way Home does, so the two tabs
    // read as the same app and the tab has one clear subject
    val heroOn = app.heroOn
    // ac: budget-hero — the figure sits on the left and the ring on the right, as the design says.
    // Stacking them centred made the card very tall and left the content hanging off to one side,
    // because a Column without fillMaxWidth centres inside its own content, not inside the card.
    // ac: over-budget-hero — going over changes what the hero says and what it is made of. The
    // ground turns deep clay, so the state reads before any number does, and the headline becomes
    // the overage. "Rp 0 remaining" is true but says nothing once you are over; the amount you are
    // over by is the fact that matters, so it takes the largest type on the card.
    Box(Modifier.fillMaxWidth().clip(AppShape.xl).background(if (over) app.heroOverBg else app.heroBg).heroGlow(heroOn)) {
        Column(Modifier.fillMaxWidth().padding(Space.xl)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BoxWithConstraints(Modifier.weight(1f)) {
                    val room = maxWidth
                    Column {
                        Text(
                            stringResource(if (over) R.string.budget_over_label else R.string.budget_remaining),
                            color = heroOn.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        // ac: budget-figures-count-up — the remaining figure counts up from zero on first draw
                        // ac: budget-hero — the figure is measured against the room beside the ring and
                        // takes the largest role of the scale that fits, so it never wraps and never
                        // clips. The size comes from the settled value, so it holds still while the
                        // figure counts up.
                        val figure = if (over) g.totalSpent - g.totalTarget else g.remaining
                        val settled = formatMoney(g.currency, figure)
                        val figureStyle = rememberFittedStyle(
                            text = settled,
                            maxWidth = room,
                            candidates = listOf(
                                MaterialTheme.typography.displayLarge,
                                MaterialTheme.typography.displayMedium,
                                MaterialTheme.typography.displaySmall,
                                MaterialTheme.typography.headlineLarge,
                                MaterialTheme.typography.headlineMedium,
                            ),
                        )
                        Text(
                            formatMoney(g.currency, countUp(figure)),
                            style = figureStyle,
                            color = heroOn,
                            maxLines = 1,
                        )
                        Text(stringResource(R.string.budget_of_total, formatMoney(g.currency, g.totalTarget)), color = heroOn.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.width(Space.lg))
                // ac: over-budget-on-hero — the expense red is as dark as the pine ground, so the ring
                // and its percent use the hero's own light coral instead
                Ring(fraction = if (g.totalTarget > 0) (g.totalSpent / g.totalTarget).toFloat() else 0f,
                    over = over, color = heroOn, track = heroOn.copy(alpha = 0.2f), red = app.heroOver)
            }
            // ac: budget-cycle-stepper — a past cycle has no days left, so the foot shows nothing.
            // ac: over-budget-hero — the overage moved to the headline, so the foot no longer repeats it.
            if (g.isCurrentCycle) {
                HorizontalDivider(color = heroOn.copy(alpha = 0.18f), modifier = Modifier.padding(vertical = Space.lg))
                if (!over) {
                    Text(
                        stringResource(R.string.budget_safe, formatMoney(g.currency, g.dailyAllowance)),
                        color = heroOn, style = MaterialTheme.typography.headlineMedium,
                    )
                }
                run {
                    Text(stringResource(R.string.budget_days_left, g.daysLeft), color = heroOn.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
                    // ac: budget-cap-at-balance — say so when the balance, not the budget, sets the daily amount
                    val balance = g.balance
                    if (!over && balance != null && balance < g.remaining) {
                        Text(
                            stringResource(R.string.budget_limited_by_balance, formatMoney(g.currency, balance)),
                            color = heroOn.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
    // ac: budget-spend-comparison-chart — cumulative spend this cycle vs last cycle for the selected
    // budget: all flexible lines, or one line picked from the chips
    if (g.allCompare != null) {
        var selectedLineId by rememberSaveable { mutableStateOf<Long?>(null) }
        val compare = selectedLineId?.let { g.lineCompare[it] } ?: g.allCompare
        Text(stringResource(R.string.budget_compare_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            BudgetChip(stringResource(R.string.budget_compare_all), selectedLineId == null) { selectedLineId = null }
            (if (g.mergedChips) g.lines.distinctBy { it.line.name } else g.lines).forEach { ls ->
                BudgetChip(ls.line.name, selectedLineId == ls.line.id) { selectedLineId = ls.line.id }
            }
        }
        SpendCompareChart(compare, g.currency)
    }
    // Lines
    g.lines.forEach { ls ->
        LineRow(ls, g.currency, g.daysLeft, app.red, expandedId == ls.line.id) { toggle(ls.line.id) }
    }
    // ac: budget-unbudgeted-line — spend that no line claims, so it is not hidden
    if (g.unbudgetedSpent > 0.0) {
        UnbudgetedRow(g.unbudgetedSpent, g.unbudgetedRecent, g.currency, app.red, expandedId == UNBUDGETED_ID) { toggle(UNBUDGETED_ID) }
    }

    // ac: budget-fixed-bills — fixed obligations listed with their spend, marked Paid when covered
    if (g.fixed.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.budget_fixed_bills),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Space.xs, top = Space.xs),
        )
        g.fixed.forEach { ls ->
            FixedRow(ls, g.currency, app.green, expandedId == ls.line.id) { toggle(ls.line.id) }
        }
    }
}

// ac: budget-line-details-and-assignment — the expanded list: up to 3 recent transactions for a line,
// each with its name, date, and amount, or an empty note when the line has no transactions yet.
@Composable
private fun RecentTransactions(recent: List<TransactionUiModel>) {
    Column(Modifier.padding(top = Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        if (recent.isEmpty()) {
            Text(stringResource(R.string.budget_line_tx_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            recent.forEach { tx ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tx.item, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        Text(shortDate(tx.dateIso), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatMoney(AccountStore.currencyOf(tx.wallet), tx.amount), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// ac: budget-fixed-bills — one compact row per fixed line: name, spent of target, and a Paid badge
// ac: budget-line-details-and-assignment — tapping a fixed line expands its recent transactions
@Composable
private fun FixedRow(ls: BudgetLineSpend, currency: String, green: Color, expanded: Boolean, onToggle: () -> Unit) {
    val paid = ls.line.target > 0.0 && ls.spent >= ls.line.target
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.clickable { onToggle() }.animateContentSize().padding(horizontal = Space.lg, vertical = Space.md)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(ls.line.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(R.string.budget_fixed_spent, formatMoney(currency, ls.spent), formatMoney(currency, ls.line.target)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (paid) {
                    Text(
                        stringResource(R.string.budget_paid),
                        style = MaterialTheme.typography.labelMedium, color = green,
                    )
                }
            }
            if (expanded) RecentTransactions(ls.recent)
        }
    }
}

@Composable
private fun Ring(fraction: Float, over: Boolean, color: Color, track: Color, red: Color) {
    // ac: budget-ring-grows — the ring sweeps out from zero instead of appearing full
    val grown = grownFraction(fraction)
    val pct = (fraction * 100).roundToLong()
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 11.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color = if (over) red else color, startAngle = -90f, sweepAngle = 360f * grown.coerceIn(0f, 1f), useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(stringResource(R.string.budget_percent, pct.toInt()), style = MaterialTheme.typography.headlineMedium, color = if (over) red else color)
    }
}

// ac: budget-line-details-and-assignment — tapping a flex line expands its recent transactions
@Composable
private fun LineRow(ls: BudgetLineSpend, currency: String, daysLeft: Int, red: Color, expanded: Boolean, onToggle: () -> Unit) {
    // ac: android-budget-daily-allowance — each line's remaining and per-day remaining
    val perDay = if (daysLeft > 0) ls.remaining / daysLeft else ls.remaining
    // ac: over-budget-ground — an over-budget line tints its whole card, so it is visible without
    // reading the numbers. Before this the only signal was red text.
    val ground = if (ls.isOver) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    Surface(color = ground, shape = AppShape.lg, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.clickable { onToggle() }.animateContentSize().padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(categoryColor(ls.line.name)))
                    Text(ls.line.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(stringResource(R.string.budget_percent, (ls.progress * 100).roundToLong().toInt()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // ac: budget-bars-grow — each line bar grows from zero instead of appearing filled
            val grown = grownFraction(ls.progress)
            LinearProgressIndicator(
                progress = { grown },
                color = if (ls.isOver) red else categoryColor(ls.line.name),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(AppShape.pill),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // ac: android-budget-daily-allowance — an over-budget line shows how far over it is,
                // instead of a "0 left / 0 per day" that reads as if there were still budget.
                Text(
                    if (ls.isOver) stringResource(R.string.budget_line_over, formatMoney(currency, ls.spent - ls.line.target))
                    else if (daysLeft == 0) stringResource(R.string.budget_line_remaining, formatMoney(currency, ls.remaining))
                    else stringResource(R.string.budget_line_remaining, formatMoney(currency, ls.remaining)) + " · " +
                        stringResource(R.string.budget_line_perday, formatMoney(currency, perDay)),
                    style = MaterialTheme.typography.bodySmall, color = if (ls.isOver) red else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.budget_line_of, formatMoney(currency, ls.line.target)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded) RecentTransactions(ls.recent)
        }
    }
}

private const val UNBUDGETED_ID = -1L

// ac: budget-unbudgeted-line — the spend no budget line claims. It has no target, so all of it counts
// against what is left. Tapping it shows its latest transactions.
@Composable
private fun UnbudgetedRow(spent: Double, recent: List<TransactionUiModel>, currency: String, red: Color, expanded: Boolean, onToggle: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = AppShape.lg, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.clickable { onToggle() }.animateContentSize().padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.budget_unbudgeted), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(formatMoney(currency, spent), style = MaterialTheme.typography.titleMedium, color = red)
            }
            Text(stringResource(R.string.budget_unbudgeted_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expanded) RecentTransactions(recent)
        }
    }
}
