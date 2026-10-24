package com.fidriyanto.banktracker.ui.balances

import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.domain.model.currenciesInUse
import androidx.compose.foundation.layout.Spacer
import com.fidriyanto.banktracker.ui.common.holdsWallet
import com.fidriyanto.banktracker.ui.common.BookStore
import com.fidriyanto.banktracker.ui.common.LoadingIndicator
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.common.LocalFloatingToolbarInset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import com.fidriyanto.banktracker.domain.model.currencySymbol
import com.fidriyanto.banktracker.domain.model.isZeroDecimalCurrency
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space
import com.fidriyanto.banktracker.ui.common.countUp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import com.fidriyanto.banktracker.ui.common.heroGlow
import androidx.compose.foundation.layout.BoxWithConstraints
import com.fidriyanto.banktracker.ui.common.rememberFittedStyle
import com.fidriyanto.banktracker.domain.usecase.GrowthSeriesBuilder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesScreen(onOpenMenu: () -> Unit, viewModel: BalancesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // ac: currency-books — only the accounts of the book picked at the top of the app
    val book by BookStore.book.collectAsStateWithLifecycle()
    val balances = state.balances.filter { book.holdsWallet(it.id) }

    val pullState = rememberPullToRefreshState()
    LaunchedEffect(pullState.isRefreshing) { if (pullState.isRefreshing) viewModel.refresh() }
    LaunchedEffect(state.isLoading) { if (!state.isLoading && pullState.isRefreshing) pullState.endRefresh() }

    Box(Modifier.nestedScroll(pullState.nestedScrollConnection)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Space.lg)
                .verticalScroll(rememberScrollState())
                .padding(bottom = LocalFloatingToolbarInset.current),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            // ac: accounts-tab-revamp — an Accounts title with a menu button at the top right, replacing
            // the old Balances/Settings toggle
            Row(
                Modifier.fillMaxWidth().padding(vertical = Space.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.balances_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(onClick = onOpenMenu) {
                    Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.balances_menu))
                }
            }

            when {
                state.isLoading && state.balances.isEmpty() -> {
                    LoadingIndicator(Modifier.fillMaxWidth().height(200.dp))
                }
                state.error != null && state.balances.isEmpty() -> {
                    Text(
                        "Failed to load: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {
                    // ac: credit-cards-look-like-cards — a credit card is drawn as a card: wider than
                    // tall, in a 2-up grid, with a chip mark. A bank account stays a row. Before this
                    // every account was the same row and only the word "owed" told them apart.
                    val cards = balances.filter { it.type == "credit" }
                    val accounts = balances.filter { it.type != "credit" }
                    // ac: accounts-net-worth-hero — the net sits at the top in a pine hero, as the
                    // design shows, instead of two grey tiles at the foot of the page
                    NetWorthHero(balances)
                    // ac: account-growth-chart — the growth of checking and investment sits under the net worth
                    state.growth?.let { history ->
                        val holds = { id: String -> book.holdsWallet(id) }
                        GrowthCard(
                            seriesFor = { code, range, withInvestment -> GrowthSeriesBuilder.build(history, code, range, holds, withInvestment) },
                            currenciesFor = { withInvestment -> GrowthSeriesBuilder.currencies(history, holds, withInvestment) },
                            hasInvestment = GrowthSeriesBuilder.hasInvestment(history, holds),
                        )
                    }
                    if (cards.isNotEmpty()) {
                        SectionHeading(stringResource(R.string.balances_section_cards))
                        cards.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                                pair.forEach { CreditCardTile(it, Modifier.weight(1f)) }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    if (accounts.isNotEmpty()) {
                        SectionHeading(stringResource(R.string.balances_section_accounts))
                    }
                    // ac: view-wallet-balances — each wallet is displayed as a card
                    accounts.forEach { wallet ->
                        WalletBalanceCard(wallet)
                    }

                    // ac: remove-wallet-reconciliation — no Reconciliation section, month navigator, or
                    // opening-balance input below the net worth row
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // Only draw the refresh indicator during an active pull or refresh. At rest it parks above
        // the content top, which here lands inside the title row and looks like a stray circle.
        if (pullState.isRefreshing || pullState.progress > 0f) {
            PullToRefreshContainer(pullState, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = Space.sm),
    )
}

// ac: credit-cards-look-like-cards — the card tile: name and chip mark on top, the owed balance and
// its currency at the foot, on the owed ground so a liability never reads like money you hold.
@Composable
private fun CreditCardTile(wallet: WalletBalanceDto, modifier: Modifier) {
    val ground = MaterialTheme.colorScheme.errorContainer
    val ink = MaterialTheme.colorScheme.onErrorContainer
    Column(
        modifier
            .clip(AppShape.lg)
            .background(ground)
            .heightIn(min = 112.dp)
            .padding(Space.lg),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(
                wallet.name,
                style = MaterialTheme.typography.titleSmall,
                color = ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Column(Modifier.padding(top = Space.md)) {
            Text(
                formatMoney(wallet.currency, countUp(wallet.balance)),
                style = MaterialTheme.typography.headlineSmall,
                color = ink,
            )
            Text(
                stringResource(R.string.balances_subtitle_owed, wallet.currency, wallet.type),
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.8f),
            )
        }
    }
}

// ac: view-wallet-balances — credit card wallets visually distinguished and labeled as owed
// ac: accounts-tab-revamp — each account is a rounded card: name and "currency · type" on the left,
// balance right-aligned in the display font, owed and negative balances in the expense color
@Composable
private fun WalletBalanceCard(wallet: WalletBalanceDto) {
    val isCredit = wallet.type == "credit"
    val expense = LocalAppColors.current.red
    val subtitle = stringResource(
        if (isCredit) R.string.balances_subtitle_owed else R.string.balances_subtitle,
        wallet.currency,
        wallet.type,
    )
    val shape = AppShape.lg
    // ac: account-type-signature — each account carries an icon for its type in a tinted square, and
    // a credit card sits on a tinted ground. Before this every account was the same grey row and
    // only the word "owed" told a card apart from a bank account.
    val ground = if (isCredit) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val markGround = if (isCredit) expense.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primaryContainer
    val markInk = if (isCredit) expense else MaterialTheme.colorScheme.onPrimaryContainer
    val icon = when (wallet.type) {
        "credit" -> Icons.Outlined.CreditCard
        "cash" -> Icons.Outlined.Payments
        else -> Icons.Outlined.AccountBalance
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ground)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(Space.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(AppShape.md).background(markGround),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = markInk, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(wallet.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            formatMoney(wallet.currency, countUp(wallet.balance)),
            style = MaterialTheme.typography.headlineMedium,
            color = if (isCredit || wallet.balance < 0) expense else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ac: hide-empty-currency-sections — a currency with no wallet at all is left out, instead of
// showing a permanent net tile for a currency the user never uses
// ac: accounts-net-worth-hero — the net of each currency in use. The app never converts between
// currencies, so there is no single combined figure to show. One currency gets the display figure;
// several are listed as chips of equal weight, because none of them is the total.
@Composable
private fun NetWorthHero(balances: List<WalletBalanceDto>) {
    val app = LocalAppColors.current
    val heroOn = app.heroOn
    val settings = CurrencySettingsStore.current
    // ac: configurable-currency-pair — the net counts accounts the user owns. A credit card is owed
    // money, so it is left out.
    val nets = currenciesInUse(settings.homeCode, AccountStore.all).mapNotNull { code ->
        val own = balances.filter { it.currency == code }
        if (own.isEmpty()) null else code to own.filter { it.type != "credit" }.sumOf { it.balance }
    }
    if (nets.isEmpty()) return
    Box(Modifier.fillMaxWidth().clip(AppShape.xl).background(app.heroBg).heroGlow(heroOn)) {
        Column(Modifier.fillMaxWidth().padding(Space.xl), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(stringResource(R.string.balances_net_worth), color = heroOn.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            if (nets.size == 1) {
                val (code, net) = nets.first()
                // ac: budget-hero — the figure takes the largest role that fits, so a long amount
                // never clips
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val style = rememberFittedStyle(
                        text = formatMoney(code, net),
                        maxWidth = maxWidth,
                        candidates = listOf(
                            MaterialTheme.typography.displayLarge,
                            MaterialTheme.typography.displayMedium,
                            MaterialTheme.typography.displaySmall,
                            MaterialTheme.typography.headlineLarge,
                        ),
                    )
                    Text(formatMoney(code, countUp(net)), style = style, color = heroOn, maxLines = 1)
                }
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    nets.forEach { (code, net) ->
                        Surface(color = heroOn.copy(alpha = 0.12f), shape = AppShape.pill) {
                            Column(Modifier.padding(horizontal = Space.lg, vertical = Space.md)) {
                                Text(stringResource(R.string.balances_currency_net, code), color = heroOn.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                Text(formatMoney(code, countUp(net)), color = heroOn, style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
