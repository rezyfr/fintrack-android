package com.fidriyanto.banktracker.ui.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import com.fidriyanto.banktracker.ui.theme.SetupNumeral
import com.fidriyanto.banktracker.ui.theme.SetupSymbol
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.fidriyanto.banktracker.ui.common.Entrance
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.AccountType
import com.fidriyanto.banktracker.domain.model.NewAccount
import com.fidriyanto.banktracker.domain.model.currencySymbol
import com.fidriyanto.banktracker.domain.usecase.CurrencyDetect
import com.fidriyanto.banktracker.domain.usecase.PayCycle
import com.fidriyanto.banktracker.ui.common.AccountTypeChips
import com.fidriyanto.banktracker.ui.common.labelRes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.fidriyanto.banktracker.ui.theme.Space

private val commonCurrencies = listOf("USD", "EUR", "GBP", "JPY", "IDR", "THB", "SGD", "MYR", "AUD")

@Composable
internal fun PageTitle(title: Int, body: Int, bodyArg: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Entrance(0) { Text(stringResource(title), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground) }
        Entrance(1) {
            Text(
                if (bodyArg == null) stringResource(body) else stringResource(body, bodyArg),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun WelcomePage() {
    Column(Modifier.fillMaxWidth().padding(top = Space.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xl)) {
        Entrance(0) { PulsingEmblem() }
        Entrance(1) {
            Text(stringResource(R.string.setup_welcome_title), style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        }
        Entrance(2) { Text(stringResource(R.string.setup_welcome_body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrencyPage(s: SetupFormState, vm: SetupViewModel) {
    val symbol = CurrencyDetect.normalize(s.homeCode).takeIf { it.isNotEmpty() }?.let { currencySymbol(it).trim() } ?: "?"
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.xl)) {
        PageTitle(R.string.setup_currency_title, R.string.setup_currency_body)
        Entrance(2, Modifier.align(Alignment.CenterHorizontally)) {
            AnimatedContent(
                targetState = symbol,
                transitionSpec = { (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "symbol",
            ) { Text(it, style = SetupSymbol, color = MaterialTheme.colorScheme.primary) }
        }
        Entrance(3, Modifier.align(Alignment.CenterHorizontally)) {
            OutlinedTextField(
                value = s.homeCode, onValueChange = { v -> vm.update { copy(homeCode = v.uppercase().take(3)) } },
                modifier = Modifier.width(190.dp), singleLine = true,
                textStyle = MaterialTheme.typography.displayMedium.copy(textAlign = TextAlign.Center),
                label = { Text(stringResource(R.string.setup_currency_label)) },
                isError = s.homeCode.length == 3 && CurrencyDetect.normalize(s.homeCode).isEmpty(),
            )
        }
        Entrance(4) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                (listOf(deviceCode()) + commonCurrencies).filter { it.isNotEmpty() }.distinct().forEach { c ->
                    FilterChip(selected = s.homeCode == c, onClick = { vm.update { copy(homeCode = c) } }, label = { Text(c) })
                }
            }
        }
    }
}

private fun deviceCode(): String = SetupFormState().homeCode

@Composable
fun CyclePage(s: SetupFormState, vm: SetupViewModel) {
    val day = s.cycleDay.toIntOrNull()?.coerceIn(1, 28) ?: 1
    val cycle = PayCycle.cycleRange(0, LocalDate.now(), day)
    val format = DateTimeFormatter.ofPattern("d MMM")
    val listState = rememberLazyListState()
    LaunchedEffect(day) { listState.animateScrollToItem((day - 3).coerceAtLeast(0)) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xl)) {
        PageTitle(R.string.setup_cycle_title, R.string.setup_cycle_body)
        Entrance(2, Modifier.align(Alignment.CenterHorizontally)) {
            AnimatedContent(
                targetState = day,
                transitionSpec = {
                    val up = targetState > initialState
                    (slideInVertically(tween(260)) { if (up) it / 2 else -it / 2 } + fadeIn(tween(260))) togetherWith
                        (slideOutVertically(tween(260)) { if (up) -it / 2 else it / 2 } + fadeOut(tween(200)))
                },
                label = "day",
            ) { Text(it.toString(), style = SetupNumeral, color = MaterialTheme.colorScheme.primary) }
        }
        Entrance(3) {
            LazyRow(state = listState, contentPadding = PaddingValues(horizontal = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                itemsIndexed((1..28).toList()) { _, d ->
                    val on = d == day
                    val grow by animateFloatAsState(if (on) 1.18f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "grow")
                    Box(
                        Modifier.size(52.dp).scale(grow).clip(CircleShape)
                            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { vm.update { copy(cycleDay = d.toString()) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(d.toString(), fontWeight = FontWeight.SemiBold, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Entrance(4) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                AnimatedContent(cycle.start.format(format) to cycle.end.format(format), transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "preview") { (from, to) ->
                    Text(stringResource(R.string.setup_cycle_preview, from, to), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(stringResource(R.string.currency_cycle_day_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AccountsPage(s: SetupFormState, vm: SetupViewModel) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PageTitle(R.string.setup_accounts_title, R.string.setup_accounts_body)
        s.accounts.forEach { account -> androidx.compose.runtime.key(account) { Entrance(0) { AccountLine(account) { vm.removeAccount(account) } } } }
        Entrance(2) { AddAccountForm(s.currencies, vm::addAccount) }
    }
}

@Composable
private fun AccountLine(account: NewAccount, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = Space.lg, top = Space.xs, bottom = Space.xs, end = Space.xs).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.account_with_currency, account.currency, stringResource(account.type.labelRes)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_delete)) }
        }
    }
}

@Composable
private fun AddAccountForm(currencies: List<String>, onAdd: (String, String, AccountType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CHECKING) }
    var currency by remember(currencies.firstOrNull()) { mutableStateOf(currencies.firstOrNull().orEmpty()) }
    val ready = name.isNotBlank() && CurrencyDetect.normalize(currency).isNotEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        OutlinedTextField(
            value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.setup_account_name)) },
        )
        OutlinedTextField(
            value = currency, onValueChange = { currency = it.uppercase().take(3) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.setup_account_currency)) },
            isError = currency.length == 3 && CurrencyDetect.normalize(currency).isEmpty(),
        )
        Row(Modifier.fillMaxWidth().padding(vertical = Space.xs), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            currencies.forEach { c -> FilterChip(selected = c == currency, onClick = { currency = c }, label = { Text(c) }) }
        }
        AccountTypeChips(selected = type, onSelect = { type = it })
        OutlinedButton(onClick = { onAdd(name, currency, type); name = "" }, enabled = ready, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.setup_add_account))
        }
    }
}

@Composable
fun RatesPage(s: SetupFormState, vm: SetupViewModel) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PageTitle(R.string.setup_rates_title, R.string.setup_rates_body, s.homeCode)
        s.otherCurrencies.forEachIndexed { i, code ->
            Entrance(2 + i) {
                OutlinedTextField(
                    value = s.rates[code].orEmpty(),
                    onValueChange = { v -> vm.update { copy(rates = rates + (code to v.filter { c -> c.isDigit() || c == '.' })) } },
                    label = { Text(stringResource(R.string.currency_rate_label, code, s.homeCode)) },
                    leadingIcon = { Text(currencySymbol(code).trim(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun DonePage(s: SetupFormState) {
    val day = s.cycleDay.toIntOrNull() ?: 1
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = Space.md), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        AnimatedCheck()
        Entrance(3) { Text(stringResource(R.string.setup_done_title), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground) }
        Entrance(4) { Text(stringResource(R.string.setup_done_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Entrance(5) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text(stringResource(R.string.setup_done_currency, CurrencyDetect.normalize(s.homeCode)), color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.setup_done_cycle, day), color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.setup_done_accounts, s.accounts.size, s.currencies.joinToString(", ")), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
