package com.fidriyanto.banktracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.AccountType
import com.fidriyanto.banktracker.ui.common.AccountTypeChips
import com.fidriyanto.banktracker.ui.common.labelRes
import com.fidriyanto.banktracker.ui.theme.Space

@Composable
fun ManageAccountsScreen(onBack: () -> Unit, viewModel: ManageAccountsViewModel = hiltViewModel()) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Account?>(null) }

    Column(Modifier.fillMaxWidth().padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.manage_accounts_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            items(accounts, key = { it.id }) { account ->
                AccountRow(account = account, onEdit = { editing = account }, onDelete = { viewModel.delete(account) })
            }
        }

        OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.manage_accounts_add))
        }
    }

    editing?.let { account ->
        EditAccountDialog(
            account = account,
            onDismiss = { editing = null },
            onConfirm = { name, opening -> viewModel.editAccount(account, name.trim(), opening); editing = null },
        )
    }

    if (showAdd) {
        AddAccountDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, currency, type, opening -> viewModel.addAccount(name, currency, type, opening); showAdd = false },
        )
    }
}

@Composable
private fun AccountRow(account: Account, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(Space.lg).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(account.displayName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    listOf(account.currency, stringResource(account.type.labelRes)).joinToString(" · ") +
                        if (account.isDefault) " · ${stringResource(R.string.manage_accounts_default_badge)}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // ac: manage-accounts — a default account cannot be renamed or deleted
            // ac: account-opening-balance — but its opening balance can be set, so a default account that
            // is not a credit card still has the edit button
            Row {
                if (!account.isDefault || account.type != AccountType.CREDIT) {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("account_rename_${account.id}")) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.manage_accounts_edit))
                    }
                }
                if (!account.isDefault) {
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("account_delete_${account.id}")) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            }
        }
    }
}

// ac: manage-accounts — the user can rename a custom account. The id stays the same, so stored
// transactions still point to it.
// ac: account-opening-balance — the opening balance is set here too. A default account keeps its
// name, and a credit card has no opening balance, because the growth chart leaves cards out.
@Composable
private fun EditAccountDialog(account: Account, onDismiss: () -> Unit, onConfirm: (name: String, openingBalance: Double) -> Unit) {
    var name by remember { mutableStateOf(account.displayName) }
    var opening by remember { mutableStateOf(openingText(account.openingBalance)) }
    val openingValue = parseOpeningBalance(opening)
    val changed = name.trim() != account.displayName || openingValue != account.openingBalance
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_accounts_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.manage_accounts_name_label)) },
                    singleLine = true,
                    enabled = !account.isDefault,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (account.type != AccountType.CREDIT) {
                    OpeningBalanceField(value = opening, currency = account.currency, onChange = { opening = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, openingValue ?: 0.0) },
                enabled = name.isNotBlank() && openingValue != null && changed,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, currency: String, type: AccountType, openingBalance: Double) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CHECKING) }
    var opening by remember { mutableStateOf("") }
    val openingValue = parseOpeningBalance(opening)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_accounts_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.manage_accounts_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text(stringResource(R.string.manage_accounts_currency_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                AccountTypeChips(selected = type, onSelect = { type = it })
                if (type != AccountType.CREDIT) {
                    OpeningBalanceField(value = opening, currency = currency.trim().uppercase(), onChange = { opening = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, currency, type, if (type == AccountType.CREDIT) 0.0 else openingValue ?: 0.0) },
                enabled = name.isNotBlank() && currency.isNotBlank() && openingValue != null,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun OpeningBalanceField(value: String, currency: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.manage_accounts_opening_label)) },
        prefix = if (currency.isEmpty()) null else ({ Text(currency) }),
        supportingText = { Text(stringResource(R.string.manage_accounts_opening_hint)) },
        isError = parseOpeningBalance(value) == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

// Empty means 0. Text that is not a number is null, so the dialog cannot save it.
internal fun parseOpeningBalance(text: String): Double? = if (text.isBlank()) 0.0 else text.trim().toDoubleOrNull()

// The text a stored opening balance shows as: empty for 0, and no trailing zeros otherwise.
internal fun openingText(value: Double): String =
    if (value == 0.0) "" else java.math.BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
