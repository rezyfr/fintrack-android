package com.fidriyanto.banktracker.ui.settings.custombanks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.CustomBankPattern
import com.fidriyanto.banktracker.ui.theme.Space

// ac: custom-bank-patterns - the banks the user taught the app. A row opens the editor, the switch turns the pattern on or off.
@Composable
fun CustomBanksSection(
    patterns: List<CustomBankPattern>,
    accounts: List<Account>,
    onToggle: (CustomBankPattern, Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Text(stringResource(R.string.custom_banks_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.custom_banks_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
        patterns.forEach { p ->
            val account = accounts.firstOrNull { it.id == p.walletId }
            Card(Modifier.fillMaxWidth().clickable { onEdit(p.id) }) {
                Row(Modifier.padding(Space.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            account?.let { stringResource(R.string.account_with_currency, it.displayName, it.currency) } ?: stringResource(R.string.custom_banks_no_account),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = p.enabled, onCheckedChange = { onToggle(p, it) })
                }
            }
        }
        OutlinedButton(onClick = onAdd, Modifier.fillMaxWidth()) { Text(stringResource(R.string.custom_banks_add)) }
    }
}
