package com.fidriyanto.banktracker.ui.feed

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DeleteTransactionDialog(
    transaction: TransactionUiModel,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    // ac: delete-transaction-from-feed — dialog body names the item + amount so the user can confirm intent
    val amount = formatDeleteAmount(transaction.amount, transaction.wallet)

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
            TextButton(onClick = onConfirm) {
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

private fun formatDeleteAmount(amount: Double, wallet: String?): String {
    val isThb = wallet == null || wallet == "BBL"
    val symbol = if (isThb) "฿" else "Rp "
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
    val body = formatter.format(kotlin.math.abs(amount).toLong())
    return "$symbol$body"
}
