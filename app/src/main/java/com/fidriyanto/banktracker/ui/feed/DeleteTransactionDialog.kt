package com.fidriyanto.banktracker.ui.feed

import com.fidriyanto.banktracker.domain.model.formatMoney
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.currencySymbol
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import java.text.NumberFormat
import java.util.Locale
import com.fidriyanto.banktracker.ui.common.rememberConfirm

@Composable
fun DeleteTransactionDialog(
    transaction: TransactionUiModel,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    // ac: haptic-feedback — a firmer confirm when the transaction is removed
    val confirm = rememberConfirm()
    // ac: delete-transaction-from-feed — dialog body names the item + amount so the user can confirm intent
    val amount = formatMoney(AccountStore.currencyOf(transaction.wallet, CurrencySettingsStore.current.noWalletCurrency), transaction.amount)

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.delete_dialog_title)) },
        text = {
            Text(
                stringResource(R.string.delete_dialog_message, transaction.item, amount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = { confirm(); onConfirm() }) {
                Text(
                    stringResource(R.string.delete_dialog_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            // ac: delete-transaction-from-feed — Cancel button restores resting state without any mutation
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
