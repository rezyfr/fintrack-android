package com.fidriyanto.banktracker.ui.add

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import java.time.Instant
import java.time.ZoneOffset

internal fun categoriesFor(txType: TxType): List<String> = when (txType) {
    TxType.EXPENSE    -> listOf(
        "Bills", "Subscriptions", "Entertainment", "Food & Drink", "Groceries",
        "Health & Wellbeing", "Family", "Other", "Shopping", "Transport", "Travel", "Business", "Gifts"
    )
    TxType.INCOME     -> listOf("Salary", "Freelance", "Business", "Dividends", "Rental", "Bonus", "Gift", "Other")
    // ac: transfer-uses-transfer-category — transfers use the category "Transfer", matching web and the DB
    TxType.TRANSFER   -> listOf("Transfer")
    TxType.INVESTMENT -> listOf("Investment", "Dividends", "Other")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    viewModel: AddViewModel = hiltViewModel(),
    onSaved: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val merchantSuggestions by viewModel.merchantSuggestions.collectAsStateWithLifecycle()
    val appColors = LocalAppColors.current

    // ac: four-tab-nav-with-add-fab — a successful save closes the add bottom sheet and resets the form
    LaunchedEffect(state.successMessage) {
        if (!state.successMessage.isNullOrEmpty()) {
            onSaved()
            viewModel.reset()
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    // ac: add-transaction-date — tapping the date field opens a date picker dialog pre-set to the current field value
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                // ac: add-transaction-date — selecting a date from the picker updates the date field to the chosen date
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.update { copy(date = picked) }
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

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
        ToggleRow(stringResource(R.string.add_tx_type_label), TxType.entries.map { it.displayName }, state.txType.displayName) { name ->
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

        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            val dropdownWidth = maxWidth
            OutlinedTextField(
                value = state.description, onValueChange = { viewModel.update { copy(description = it) } },
                label = { Text(stringResource(R.string.add_description_label)) }, modifier = Modifier.fillMaxWidth()
            )
            MerchantSuggestionDropdown(
                suggestions = merchantSuggestions,
                width = dropdownWidth,
                onSelect = {
                    viewModel.update { copy(description = it) }
                    viewModel.dismissSuggestions()
                },
                onDismiss = { viewModel.dismissSuggestions() },
            )
        }

        // ac: add-transaction-date — date field defaults to today and opens a date picker on press
        val dateInteractionSource = remember { MutableInteractionSource() }
        val isDatePressed by dateInteractionSource.collectIsPressedAsState()
        if (isDatePressed) showDatePicker = true
        OutlinedTextField(
            value = state.date.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.add_date_label)) },
            modifier = Modifier.fillMaxWidth(),
            interactionSource = dateInteractionSource,
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
            // ac: add-transaction-date — submit button is labelled Submit
            else Text(stringResource(R.string.add_submit_button), fontWeight = FontWeight.SemiBold)
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
