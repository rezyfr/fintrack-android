package com.fidriyanto.banktracker.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.theme.LocalAppColors

private fun categoriesFor(txType: TxType): List<String> = when (txType) {
    TxType.EXPENSE    -> listOf(
        "Bills", "Subscriptions", "Entertainment", "Food & Drink", "Groceries",
        "Health & Wellbeing", "Other", "Shopping", "Transport", "Travel", "Business", "Gifts"
    )
    TxType.INCOME     -> listOf("Salary", "Freelance", "Business", "Dividends", "Rental", "Bonus", "Gift", "Other")
    TxType.TRANSFER   -> listOf("Transfer Out")
    TxType.INVESTMENT -> listOf("Investment", "Dividends", "Other")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(viewModel: AddViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appColors = LocalAppColors.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.add_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)

        // Wallet picker
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.add_wallet), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Wallet.entries.forEach { w ->
                    FilterChip(
                        selected = state.wallet == w,
                        onClick  = { viewModel.update { copy(wallet = w, toWallet = null) } },
                        label    = { Text(w.id, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Tx type picker
        ToggleRow("Type", TxType.entries.map { it.displayName }, state.txType.displayName) { name ->
            val picked = TxType.entries.first { it.displayName == name }
            viewModel.update { copy(txType = picked, toWallet = null, category = categoriesFor(picked).first()) }
        }

        // To-wallet picker (only for transfers)
        if (state.txType == TxType.TRANSFER) {
            val destinations = Wallet.entries.filter { it != state.wallet }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.add_to_wallet), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    destinations.forEach { w ->
                        FilterChip(
                            selected = state.toWallet == w,
                            onClick  = { viewModel.update { copy(toWallet = w) } },
                            label    = { Text(w.id, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.amount, onValueChange = { viewModel.update { copy(amount = it) } },
            label = { Text(stringResource(R.string.add_amount_label, state.wallet.currency)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            value = state.description, onValueChange = { viewModel.update { copy(description = it) } },
            label = { Text(stringResource(R.string.add_description_label)) }, modifier = Modifier.fillMaxWidth()
        )

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = state.category, onValueChange = {},
                readOnly = true, label = { Text(stringResource(R.string.add_category_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categoriesFor(state.txType).forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = { viewModel.update { copy(category = cat) }; expanded = false }
                    )
                }
            }
        }

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        state.successMessage?.let { Text(it, color = appColors.green, fontSize = 12.sp) }

        Button(
            onClick = { viewModel.submit() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !state.isLoading,
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(stringResource(R.string.add_sync_button), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ToggleRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            options.forEach { opt ->
                FilterChip(selected = opt == selected, onClick = { onSelect(opt) }, label = { Text(opt) })
            }
        }
    }
}
