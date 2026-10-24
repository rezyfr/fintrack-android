package com.fidriyanto.banktracker.ui.home

import com.fidriyanto.banktracker.ui.common.Book
import androidx.compose.runtime.LaunchedEffect
import com.fidriyanto.banktracker.ui.common.LoadingIndicator
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.feed.CategoryAvatar
import com.fidriyanto.banktracker.ui.common.LocalFloatingToolbarInset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.usecase.BudgetGlance
import com.fidriyanto.banktracker.domain.model.CardStatement
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.currencySymbol
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.Entrance
import com.fidriyanto.banktracker.ui.common.countUp
import com.fidriyanto.banktracker.ui.common.grownFraction
import com.fidriyanto.banktracker.ui.common.SpendCompareChart
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import com.fidriyanto.banktracker.ui.feed.EditTransactionBottomSheet
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import com.fidriyanto.banktracker.ui.common.EmptyState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import com.fidriyanto.banktracker.ui.theme.categoryColor
import com.fidriyanto.banktracker.ui.common.heroGlow

private fun walletName(w: String) = AccountStore.nameOf(w)
private fun shortDate(iso: String) = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
}.getOrDefault(iso)

@Composable
fun HomeScreen(onOpenBudget: () -> Unit, onOpenTransactions: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    // ac: home-cycle-overview — recompute the cycle and payday countdown each time the screen resumes,
    // so the overview rolls into the new cycle when the date changes while the app stays open.
    // ac: home-loading-state — the resume reload flips loading on but keeps the previous state's data
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    val app = LocalAppColors.current
    var pendingEdit by remember { mutableStateOf<TransactionUiModel?>(null)}
    // ac: home-loading-state — the first load shows a spinner instead of empty sections. A reload on
    // resume keeps the data already shown.
    if (s.loading && s.cycleFromIso.isEmpty()) {
        LoadingIndicator()
        return
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.lg).padding(bottom = LocalFloatingToolbarInset.current),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        // ac: home-section-stagger — the sections fade in and rise, one after the other, on first draw
        // Header
        Entrance(0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                    if (s.cycleFromIso.isNotEmpty()) {
                        Text(stringResource(R.string.home_cycle_range, shortDate(s.cycleFromIso), shortDate(s.cycleToIso)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = AppShape.pill) {
                    Text(
                        if (s.daysToPayday == 0) stringResource(R.string.home_payday_today)
                        else stringResource(R.string.home_payday_in, s.daysToPayday),
                        color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = Space.md, vertical = Space.sm),
                    )
                }
            }
        }

        // ac: auto-added-notice - transactions the app added by itself since the user last dismissed the notice
        AutoAddedNotice(onView = onOpenTransactions)

        // Hero: cycle summary, split by currency, styled like the mockup
        Entrance(1) { Hero(s) }

        // ac: home-budget-bento — two tiles side by side under the hero, so the page is a grid and
        // not one column. Both read the budget the page already has.
        s.budget?.let { b ->
            if (b.lines.isNotEmpty()) {
                Entrance(2) { BudgetBento(b) }
            }
        }

        // ac: home-cycle-spending-comparison — cumulative spending, this cycle vs last cycle
        s.spend?.let { sc ->
            if (sc.points.isNotEmpty()) {
                Entrance(3) {
                    Section(stringResource(R.string.home_spend_compare)) {
                        SpendCompareChart(sc, s.spendCurrency)
                    }
                }
            }
        }

        // ac: home-cycle-overview — a compact budget card with remaining, daily allowance, top lines
        s.budget?.let { b ->
            if (b.lines.isNotEmpty()) {
                Entrance(4) {
                    Section(stringResource(R.string.home_budget)) {
                        BudgetCard(b, app.red, onOpenBudget)
                    }
                }
            }
        }

        // ac: home-cycle-overview — each credit card's minimum payment and due date
        // Cards due (two-column compact cards)
        if (s.cards.isNotEmpty()) {
            Entrance(5) {
                Section(stringResource(R.string.home_cards_due)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        s.cards.forEach { CardDueCard(it, Modifier.weight(1f), app.red, app.warning) }
                        if (s.cards.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ac: home-cycle-overview — the most recent transactions in the cycle
        // Recent
        Entrance(5) {
            Section(stringResource(R.string.home_recent)) {
                Surface(color = MaterialTheme.colorScheme.surface, shape = AppShape.lg, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        if (s.recent.isEmpty()) {
                            // ac: empty-states — an empty list says what it is for and what to do next
                            EmptyState(
                                icon = Icons.Outlined.Payments,
                                title = stringResource(R.string.home_recent_empty),
                                body = stringResource(R.string.home_recent_empty_body),
                            )
                        } else {
                            s.recent.forEachIndexed { i, tx -> RecentRow(tx, app.red, app.green, showDivider = i < s.recent.lastIndex,
                                onClick = {
                                    pendingEdit = tx
                                }) }
                        }
                    }
                }
            }
        }
    }

    pendingEdit?.let { tx ->
        LaunchedEffect(tx.id) { viewModel.loadInstallments() }
        val installments by viewModel.installments.collectAsStateWithLifecycle()
        EditTransactionBottomSheet(
            transaction = tx,
            installments = installments,
            onDismiss = { pendingEdit = null },
            onSave = { edit ->
                viewModel.edit(tx.id, edit)
                pendingEdit = null
            },
        )
    }
}

// ac: android-budget-daily-allowance — Home compact budget card: remaining, daily allowance, top lines
@Composable private fun BudgetCard(b: BudgetGlance, red: Color, onOpenBudget: () -> Unit) {
    val over = b.totalSpent > b.totalTarget
    Surface(color = MaterialTheme.colorScheme.surface, shape = AppShape.lg, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(stringResource(R.string.budget_remaining), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // ac: home-figures-count-up — money figures count up from zero on first draw
                    Text(formatMoney(b.currency, countUp(b.remaining)), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
                }
                if (over) {
                    Text(
                        stringResource(R.string.budget_over_total, formatMoney(b.currency, b.totalSpent - b.totalTarget)),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (over) red else MaterialTheme.colorScheme.primary,
                    )
                }
            }
            // ac: budget-remaining-sums-lines — Home lists the first five lines, and the Budget tab lists every line
            // ac: home-budget-top-five-cta — Home lists the first five flexible lines; the rest are on the Budget tab
            b.lines.take(5).forEach { ls ->
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(categoryColor(ls.line.name)))
                            Text(ls.line.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        // ac: android-budget-daily-allowance — over-budget line shows the overage, not "0 left"
                        Text(
                            if (ls.isOver) stringResource(R.string.budget_line_over, formatMoney(b.currency, ls.spent - ls.line.target))
                            else stringResource(R.string.budget_line_remaining, formatMoney(b.currency, ls.remaining)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ls.isOver) red else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // ac: home-bars-grow — each budget bar grows from zero instead of appearing filled
                    val grown = grownFraction(ls.progress)
                    LinearProgressIndicator(
                        progress = { grown },
                        color = if (ls.isOver) red else categoryColor(ls.line.name),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(AppShape.pill),
                    )
                }
            }
            // ac: home-budget-top-five-cta — the call to action opens the Budget tab
            TextButton(onClick = onOpenBudget, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.home_budget_see_all), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable private fun Hero(s: HomeUiState) {
    val hero = LocalAppColors.current
    val onPine = hero.heroOn
    Box(
        Modifier.fillMaxWidth().clip(AppShape.xl).background(hero.heroBg).heroGlow(onPine),
    ) {
        // ac: currency-books - one block for the book shown. COMBINED shows one block for all currencies.
        val currencySettings = CurrencySettingsStore.current
        val heroCurrency = s.book.code ?: currencySettings.homeCode
        val heroLabel = if (s.book.isCombined) stringResource(R.string.home_all_accounts) else currencySettings.labelFor(heroCurrency)
        Column(Modifier.padding(Space.xl), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
            HeroCurrency(heroLabel, heroCurrency, s.moneyIn, s.moneyOut, onPine)
            // ac: home-cycle-overview — credit-card payments shown as separate info, not in money out
            if (s.ccPayments > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.home_card_payments), color = onPine.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
                    Text(formatMoney(currencySettings.homeCode, s.ccPayments), color = onPine, style = MaterialTheme.typography.titleSmall)
                }
            }
            // cycle-elapsed bar + foot
            // ac: home-bars-grow — the cycle bar grows from zero
            val frac = grownFraction(cycleElapsed(s.cycleFromIso, s.cycleToIso))
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(AppShape.pill).background(onPine.copy(alpha = 0.18f))) {
                    Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(AppShape.pill).background(onPine))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cycleDaysLabel(s.cycleFromIso, s.cycleToIso), color = onPine.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    Text(paydayDateLabel(s.cycleToIso), color = onPine.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable private fun HeroCurrency(label: String, currency: String, income: Double, expenses: Double, onPine: Color) {
    val net = income - expenses
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(label, color = onPine.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
        // ac: home-figures-count-up — the hero figures count up from zero on first draw
        Text(formatMoney(currency, countUp(net), showSign = true), color = onPine, style = MaterialTheme.typography.displayMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xl), modifier = Modifier.padding(top = Space.xs)) {
            HeroStat(stringResource(R.string.home_income), formatMoney(currency, countUp(income)), onPine)
            HeroStat(stringResource(R.string.home_spent), formatMoney(currency, countUp(expenses)), onPine)
        }
    }
}

@Composable private fun HeroStat(label: String, value: String, onPine: Color) {
    Column {
        Text(label, color = onPine.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
        Text(value, color = onPine, style = MaterialTheme.typography.titleMedium)
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

// A section label belongs to the card under it, so the two sit close together and the gap to the
// next section stays wide. Before this the parent Column spaced both the same.
@Composable private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        SectionLabel(label)
        content()
    }
}

@Composable private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = Space.xs))
}

@Composable private fun CardDueCard(c: CardStatement, modifier: Modifier, red: Color, warning: Color) {
    val pillColor = when { c.daysUntilDue < 0 -> red; c.daysUntilDue <= 3 -> warning; else -> MaterialTheme.colorScheme.primary }
    val pillText = when {
        c.daysUntilDue < 0 -> stringResource(R.string.home_due_late, -c.daysUntilDue)
        c.daysUntilDue == 0 -> stringResource(R.string.home_payday_today)
        else -> stringResource(R.string.home_due_in, c.daysUntilDue)
    }
    Surface(color = MaterialTheme.colorScheme.surface, shape = AppShape.lg, tonalElevation = 1.dp, modifier = modifier) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(walletName(c.wallet), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            // ac: card-statement-fifo-minimum-rollover — headline the owed balance; subline shows the
            // remaining minimum, or "Min paid", or "Next" for a rolled-forward upcoming statement.
            Text(formatMoney(AccountStore.currencyOf(c.wallet), countUp(c.balance)), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            val sub = when {
                c.upcoming -> stringResource(R.string.home_card_next_due, shortDate(c.dueIso))
                c.minimumPaid -> stringResource(R.string.home_card_min_paid, shortDate(c.dueIso))
                else -> stringResource(R.string.home_card_min_due, formatMoney(AccountStore.currencyOf(c.wallet), c.minimumRemaining), shortDate(c.dueIso))
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(color = pillColor.copy(alpha = 0.14f), shape = AppShape.pill) {
                Text(pillText, color = pillColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs))
            }
        }
    }
}

// ac: multicurrency-money-formatting — a recent row shows its amount in its own wallet currency
@Composable private fun RecentRow(tx: TransactionUiModel, red: Color, green: Color, showDivider: Boolean, onClick: () -> Unit = {}) {
    Row(Modifier.clickable{onClick.invoke()}.padding(Space.lg).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        // ac: home-recent-category-icons — the recent rows show the same category icon as the Transactions list
        CategoryAvatar(tx.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.item, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(stringResource(R.string.home_recent_meta, tx.category, shortDate(tx.dateIso)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val income = tx.txType == "income"
        Text(formatMoney(AccountStore.currencyOf(tx.wallet, CurrencySettingsStore.current.noWalletCurrency), if (income) tx.amount else -tx.amount, showSign = true), color = if (income) green else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
    }
    if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

// ac: home-budget-bento — the safe daily amount beside how much of the budget is gone. Two tiles of
// equal height, so the row is not ragged.
@Composable private fun BudgetBento(b: BudgetGlance) {
    val used = if (b.totalTarget > 0) (b.totalSpent / b.totalTarget).toFloat() else 0f
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = AppShape.xl,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(
                    stringResource(R.string.home_safe_to_spend),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    formatMoney(b.currency, countUp(b.dailyAllowance)),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    stringResource(R.string.budget_days_left, b.daysLeft),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = AppShape.xl,
            tonalElevation = 1.dp,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            Row(
                Modifier.padding(Space.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                MiniRing(used)
                Column {
                    Text(
                        stringResource(R.string.budget_percent, (used * 100).roundToInt()),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.home_of_budget_used),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable private fun MiniRing(fraction: Float) {
    // ac: home-bars-grow — the ring sweeps out from zero
    val grown = grownFraction(fraction)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val arc = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(52.dp)) {
        val stroke = 7.dp.toPx()
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = arc, startAngle = -90f, sweepAngle = 360f * grown.coerceIn(0f, 1f), useCenter = false,
            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}
