package com.fidriyanto.banktracker.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.theme.LocalDynamicColorToggle
import com.fidriyanto.banktracker.ui.theme.LocalIsDarkTheme
import com.fidriyanto.banktracker.ui.theme.LocalIsDynamicColorEnabled
import com.fidriyanto.banktracker.ui.theme.LocalThemeToggle
import com.fidriyanto.banktracker.ui.theme.Space

// Where each row of the Settings list leads
data class SettingsActions(
    val onBack: () -> Unit,
    val onAccounts: () -> Unit,
    val onCategories: () -> Unit,
    val onCurrencies: () -> Unit,
    val onBudgets: () -> Unit,
    val onBankNotifications: () -> Unit,
    val onRules: () -> Unit,
    val onSchedules: () -> Unit,
    val onInstallmentPlans: () -> Unit,
    val onAi: () -> Unit,
)

// The Settings list: the capture status, then the rows in groups. The Backup group shows only offline.
@Composable
fun SettingsMainScreen(viewModel: SettingsViewModel, actions: SettingsActions) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pending by viewModel.pendingCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val toggleTheme = LocalThemeToggle.current
    val isDynamic = LocalIsDynamicColorEnabled.current
    val toggleDynamic = LocalDynamicColorToggle.current

    // The system decides whether the listener is on, so read it again each time the screen resumes.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.setListenerActive(NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName))
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = actions.onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }

        SettingsStatusCard(active = state.isListenerActive) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }

        SettingsGroup(stringResource(R.string.settings_group_setup)) {
            SettingsRow(Icons.Outlined.AccountBalance, stringResource(R.string.settings_manage_accounts), "settings_accounts", actions.onAccounts, subtitle = stringResource(R.string.settings_accounts_hint))
            SettingsDivider()
            SettingsRow(Icons.Outlined.Category, stringResource(R.string.settings_manage_categories), "settings_categories", actions.onCategories, subtitle = stringResource(R.string.settings_categories_hint), tone = DiscTone.TERTIARY)
            SettingsDivider()
            SettingsRow(Icons.Outlined.CurrencyExchange, stringResource(R.string.settings_currency_settings), "settings_currencies", actions.onCurrencies, subtitle = stringResource(R.string.settings_currencies_hint), tone = DiscTone.SECONDARY)
            SettingsDivider()
            SettingsRow(Icons.Outlined.PieChart, stringResource(R.string.settings_budgets), "settings_budgets", actions.onBudgets, subtitle = stringResource(R.string.settings_budgets_hint))
            SettingsRow(Icons.Outlined.CreditCard, stringResource(R.string.settings_installment_plans), "settings_installment_plans", actions.onInstallmentPlans, subtitle = stringResource(R.string.settings_installment_plans_hint))
        }

        SettingsGroup(stringResource(R.string.settings_group_automation)) {
            SettingsRow(Icons.Outlined.NotificationsActive, stringResource(R.string.settings_bank_notifications), "settings_banks", actions.onBankNotifications, subtitle = stringResource(R.string.settings_banks_hint))
            SettingsDivider()
            SettingsRow(Icons.Outlined.Tune, stringResource(R.string.settings_rules), "settings_rules", actions.onRules, subtitle = stringResource(R.string.settings_rules_hint), tone = DiscTone.TERTIARY)
            SettingsDivider()
            SettingsRow(Icons.Outlined.EventRepeat, stringResource(R.string.settings_schedules), "settings_schedules", actions.onSchedules, subtitle = stringResource(R.string.settings_schedules_hint), tone = DiscTone.SECONDARY)
            SettingsDivider()
            SettingsRow(Icons.Outlined.AutoAwesome, stringResource(R.string.settings_ai), "settings_ai", actions.onAi, subtitle = stringResource(R.string.settings_ai_hint))
        }

        SettingsGroup(stringResource(R.string.settings_group_appearance)) {
            SettingsToggleRow(Icons.Outlined.DarkMode, stringResource(R.string.settings_dark_theme), isDark, { toggleTheme() }, "settings_dark_theme")
            // Dynamic color reads the wallpaper, which Android supports from 12 (API 31)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsDivider()
                SettingsToggleRow(Icons.Outlined.Palette, stringResource(R.string.settings_dynamic_color), isDynamic, { toggleDynamic() }, "settings_dynamic_color", subtitle = stringResource(R.string.settings_dynamic_color_hint))
            }
        }

        SettingsBackupGroup()

        SettingsGroup(stringResource(R.string.settings_group_troubleshooting)) {
            SettingsRow(
                Icons.Outlined.Sync, stringResource(R.string.settings_retry_pending_syncs), "settings_retry_syncs", { viewModel.retryPendingSyncs() },
                subtitle = if (pending == 0) stringResource(R.string.settings_pending_none) else pluralStringResource(R.plurals.settings_pending_waiting, pending, pending),
                tone = DiscTone.NEUTRAL, busy = state.isSyncing || state.isClearing,
            )
            SettingsDivider()
            SettingsRow(Icons.Outlined.DoneAll, stringResource(R.string.settings_mark_all_synced), "settings_mark_synced", { viewModel.markAllSynced() }, subtitle = stringResource(R.string.settings_mark_synced_hint), tone = DiscTone.NEUTRAL, busy = state.isSyncing || state.isClearing)
        }
    }
}
