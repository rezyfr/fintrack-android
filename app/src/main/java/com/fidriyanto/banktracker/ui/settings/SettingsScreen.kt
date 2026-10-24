package com.fidriyanto.banktracker.ui.settings

import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.domain.model.currenciesInUse
import com.fidriyanto.banktracker.domain.model.CurrencyOverride
import androidx.compose.foundation.verticalScroll
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import com.fidriyanto.banktracker.ui.theme.LocalDynamicColorToggle
import com.fidriyanto.banktracker.ui.theme.LocalIsDarkTheme
import com.fidriyanto.banktracker.ui.theme.LocalIsDynamicColorEnabled
import com.fidriyanto.banktracker.ui.settings.budgets.BudgetsFlow
import com.fidriyanto.banktracker.ui.settings.rules.RulesFlow
import com.fidriyanto.banktracker.ui.settings.installmentplans.InstallmentPlansFlow
import com.fidriyanto.banktracker.ui.settings.schedules.SchedulesFlow
import com.fidriyanto.banktracker.ui.theme.LocalThemeToggle
import com.fidriyanto.banktracker.ui.settings.custombanks.CustomBanksFlow
import com.fidriyanto.banktracker.ui.theme.Space

private enum class SettingsSubScreen { MAIN, ACCOUNTS, CATEGORIES, CURRENCY, AI, BANKS, RULES, SCHEDULES, BUDGETS, INSTALLMENT_PLANS }

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    var sub by remember { mutableStateOf(SettingsSubScreen.MAIN) }
    // Back inside a Settings page goes to the Settings list first.
    BackHandler(enabled = sub != SettingsSubScreen.MAIN) { sub = SettingsSubScreen.MAIN }
    // ac: manage-accounts — Settings has a Manage Accounts entry
    // ac: manage-categories — Settings has a Manage Categories entry
    when (sub) {
        SettingsSubScreen.ACCOUNTS -> ManageAccountsScreen(onBack = { sub = SettingsSubScreen.MAIN })
        SettingsSubScreen.CATEGORIES -> ManageCategoriesScreen(onBack = { sub = SettingsSubScreen.MAIN })
        // ac: configurable-currency-pair — Settings has a Currency Settings entry
        SettingsSubScreen.CURRENCY -> CurrencySettingsScreen(viewModel = viewModel, onBack = { sub = SettingsSubScreen.MAIN })
        // ac: on-device-ai — Settings has an On-device AI entry
        SettingsSubScreen.AI -> AiSettingsScreen(onBack = { sub = SettingsSubScreen.MAIN })
        // ac: bank-parser-accounts - Settings has a Bank notifications entry
        SettingsSubScreen.BANKS -> CustomBanksFlow(onBack = { sub = SettingsSubScreen.MAIN })
        // ac: custom-rules - Settings has a Rules entry
        SettingsSubScreen.RULES -> RulesFlow(onBack = { sub = SettingsSubScreen.MAIN })
        // ac: scheduled-transactions - Settings has a Scheduled entry
        SettingsSubScreen.SCHEDULES -> SchedulesFlow(onBack = { sub = SettingsSubScreen.MAIN })
        // Settings has a Budgets entry: create, edit and delete budget lines
        SettingsSubScreen.BUDGETS -> BudgetsFlow(onBack = { sub = SettingsSubScreen.MAIN })
        SettingsSubScreen.INSTALLMENT_PLANS -> InstallmentPlansFlow(onBack = { sub = SettingsSubScreen.MAIN })
        SettingsSubScreen.MAIN -> SettingsMainScreen(
            viewModel = viewModel,
            actions = SettingsActions(
                onBack = onBack,
                onAccounts = { sub = SettingsSubScreen.ACCOUNTS },
                onCategories = { sub = SettingsSubScreen.CATEGORIES },
                onCurrencies = { sub = SettingsSubScreen.CURRENCY },
                onBudgets = { sub = SettingsSubScreen.BUDGETS },
                onBankNotifications = { sub = SettingsSubScreen.BANKS },
                onRules = { sub = SettingsSubScreen.RULES },
                onSchedules = { sub = SettingsSubScreen.SCHEDULES },
                onInstallmentPlans = { sub = SettingsSubScreen.INSTALLMENT_PLANS },
                onAi = { sub = SettingsSubScreen.AI },
            ),
        )
    }
}

// ac: configurable-currency-pair - the home currency, the pay cycle day, and for each other currency in the
// user's accounts a book name and a starting exchange rate
@Composable
private fun CurrencySettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val saved by viewModel.currencySettings.collectAsStateWithLifecycle()
    val others = currenciesInUse(saved.homeCode, AccountStore.all).drop(1)
    var homeCode by remember(saved) { mutableStateOf(saved.homeCode) }
    var homeLabel by remember(saved) { mutableStateOf(saved.homeLabel) }
    var cycleDay by remember(saved) { mutableStateOf(saved.cycleStartDay.toString()) }
    var labels by remember(saved) { mutableStateOf(others.associateWith { saved.labelFor(it) }) }
    var rates by remember(saved) { mutableStateOf(others.associateWith { saved.overrideFor(it)?.rate?.toString().orEmpty() }) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.currency_settings_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }

        OutlinedTextField(
            value = homeCode, onValueChange = { homeCode = it },
            label = { Text(stringResource(R.string.currency_home_code_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = homeLabel, onValueChange = { homeLabel = it },
            label = { Text(stringResource(R.string.currency_home_label_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = cycleDay, onValueChange = { cycleDay = it.filter(Char::isDigit).take(2) },
            label = { Text(stringResource(R.string.currency_cycle_day_label)) },
            supportingText = { Text(stringResource(R.string.currency_cycle_day_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        if (others.isNotEmpty()) {
            Text(stringResource(R.string.currency_other_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Text(stringResource(R.string.currency_other_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        others.forEach { code ->
            OutlinedTextField(
                value = labels[code].orEmpty(), onValueChange = { labels = labels + (code to it) },
                label = { Text(stringResource(R.string.currency_book_name_label, code)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = rates[code].orEmpty(), onValueChange = { rates = rates + (code to it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text(stringResource(R.string.currency_rate_label, code, homeCode.trim().uppercase())) },
                supportingText = { Text(stringResource(R.string.currency_rate_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = {
                viewModel.saveCurrencySettings(
                    CurrencySettings(
                        homeCode = homeCode, homeLabel = homeLabel, cycleStartDay = cycleDay.toIntOrNull() ?: saved.cycleStartDay,
                        overrides = others.map { CurrencyOverride(it, labels[it].orEmpty(), rates[it]?.toDoubleOrNull()) },
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = homeCode.isNotBlank() && homeLabel.isNotBlank() && (cycleDay.toIntOrNull() ?: 0) in 1..28,
        ) {
            Text(stringResource(R.string.action_save))
        }
    }
}
