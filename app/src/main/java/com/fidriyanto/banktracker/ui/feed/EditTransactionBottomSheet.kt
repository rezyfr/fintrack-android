package com.fidriyanto.banktracker.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.ui.add.MerchantSuggestionDropdown
import com.fidriyanto.banktracker.ui.add.TxType
import com.fidriyanto.banktracker.ui.add.Wallet
import com.fidriyanto.banktracker.ui.add.categoriesFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionBottomSheet(
    transaction: TransactionUiModel,
    onDismiss: () -> Unit,
    onSave: (TransactionEdit) -> Unit,
    merchantHistory: List<String> = emptyList(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ac: edit-transaction-from-feed — bottom sheet is pre-populated with the row's current values
    var amount by remember { mutableStateOf(formatAmountForEdit(transaction.amount)) }
    var item by remember { mutableStateOf(transaction.item) }
    var category by remember { mutableStateOf(transaction.category) }
    var date by remember { mutableStateOf(transaction.dateIso) }
    var wallet by remember { mutableStateOf(transaction.wallet?.let { id -> Wallet.entries.firstOrNull { it.id == id } } ?: Wallet.entries.first()) }
    var txType by remember { mutableStateOf(TxType.entries.firstOrNull { it.id == transaction.txType } ?: TxType.entries.first()) }
    var toWallet by remember { mutableStateOf<Wallet?>(null) }
    // ac: edit-transfer-target-amount — Received Amount state for cross-currency transfers
    var toAmount by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var walletExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var toWalletExpanded by remember { mutableStateOf(false) }
    var dismissedQuery by remember { mutableStateOf<String?>(null) }

    val merchantSuggestions by remember {
        derivedStateOf {
            val query = item.trim()
            when {
                query == dismissedQuery -> emptyList()
                item.isBlank()         -> merchantHistory
                else                   -> merchantHistory.filter { it.contains(item, ignoreCase = true) }
            }
        }
    }

    ModalBottomSheet(
        // ac: edit-transaction-from-feed — dismissing the sheet without Save discards changes
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.edit_sheet_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.edit_amount_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val dropdownWidth = maxWidth
                OutlinedTextField(
                    value = item,
                    onValueChange = { item = it; dismissedQuery = null },
                    label = { Text(stringResource(R.string.edit_item_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                MerchantSuggestionDropdown(
                    suggestions = merchantSuggestions,
                    width = dropdownWidth,
                    onSelect = { item = it; dismissedQuery = it.trim() },
                    onDismiss = { dismissedQuery = item.trim() },
                )
            }

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.edit_category_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    categoriesFor(txType).forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { category = cat; categoryExpanded = false },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text(stringResource(R.string.edit_date_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            ExposedDropdownMenuBox(
                expanded = walletExpanded,
                onExpandedChange = { walletExpanded = it },
            ) {
                OutlinedTextField(
                    value = wallet.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.edit_wallet_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(walletExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = walletExpanded,
                    onDismissRequest = { walletExpanded = false },
                ) {
                    Wallet.entries.forEach { w ->
                        DropdownMenuItem(
                            text = { Text(w.displayName) },
                            onClick = { wallet = w; walletExpanded = false },
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
            ) {
                OutlinedTextField(
                    value = txType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.edit_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                ) {
                    TxType.entries.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.displayName) },
                            onClick = { txType = t; typeExpanded = false },
                        )
                    }
                }
            }

            // ac: edit-transaction-from-feed — to-wallet field appears only when the type is transfer
            if (txType == TxType.TRANSFER) {
                ExposedDropdownMenuBox(
                    expanded = toWalletExpanded,
                    onExpandedChange = { toWalletExpanded = it },
                ) {
                    OutlinedTextField(
                        value = toWallet?.displayName.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.edit_to_wallet_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(toWalletExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = toWalletExpanded,
                        onDismissRequest = { toWalletExpanded = false },
                    ) {
                        Wallet.entries.filter { it != wallet }.forEach { w ->
                            DropdownMenuItem(
                                text = { Text(w.displayName) },
                                onClick = { toWallet = w; toWalletExpanded = false },
                            )
                        }
                    }
                }
            }

            // ac: edit-transfer-target-amount — Received Amount input shown only for cross-currency transfers
            if (txType == TxType.TRANSFER && toWallet != null && wallet.currency != toWallet?.currency) {
                OutlinedTextField(
                    value = toAmount,
                    onValueChange = { toAmount = it },
                    label = { Text(stringResource(R.string.edit_to_amount_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            Button(
                onClick = {
                    // ac: edit-transfer-target-amount — to_amount cleared when not a cross-currency transfer
                    val isCrossCurrency = txType == TxType.TRANSFER && toWallet != null && wallet.currency != toWallet?.currency
                    // ac: edit-transaction-from-feed — Save emits the edit; FeedViewModel applies it optimistically then PATCHes Supabase
                    onSave(
                        TransactionEdit(
                            amount   = amount.toDoubleOrNull() ?: transaction.amount,
                            item     = item,
                            category = category,
                            dateIso  = date,
                            wallet   = wallet.id,
                            txType   = txType.id,
                            toWallet = if (txType == TxType.TRANSFER) toWallet?.id else null,
                            toAmount = if (isCrossCurrency) toAmount.toDoubleOrNull() else null,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

private fun formatAmountForEdit(amount: Double): String {
    return if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
}
