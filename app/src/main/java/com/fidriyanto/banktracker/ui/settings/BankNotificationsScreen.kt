package com.fidriyanto.banktracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.ui.settings.custombanks.CustomBanksSection
import com.fidriyanto.banktracker.ui.theme.Space

// ac: bank-parser-accounts - for each bank the app reads, the account its payments go to, or off
@Composable
fun BankNotificationsScreen(onBack: () -> Unit, onAddBank: () -> Unit, onEditBank: (String) -> Unit, viewModel: BankNotificationsViewModel = hiltViewModel()) {
    val banks by viewModel.banks.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val customBanks by viewModel.customBanks.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.bank_notifications_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(stringResource(R.string.bank_notifications_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
        banks.forEach { setting ->
            val chosen = accounts.firstOrNull { it.id == setting.accountId }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text(setting.bank.label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    AccountPicker(chosen, accounts) { viewModel.choose(setting.bank, it) }
                }
            }
        }
        CustomBanksSection(customBanks, accounts, onToggle = { p, on -> viewModel.setCustomEnabled(p.id, on) }, onEdit = onEditBank, onAdd = onAddBank)
    }
}

@Composable
private fun AccountPicker(selected: Account?, accounts: List<Account>, onPick: (Account?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.displayName ?: stringResource(R.string.bank_notifications_off))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.bank_notifications_off)) }, onClick = { onPick(null); open = false })
            accounts.forEach { a ->
                DropdownMenuItem(text = { Text(stringResource(R.string.account_with_currency, a.displayName, a.currency)) }, onClick = { onPick(a); open = false })
            }
        }
    }
}
