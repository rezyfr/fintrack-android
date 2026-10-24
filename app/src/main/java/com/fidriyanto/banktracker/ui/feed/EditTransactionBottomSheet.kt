package com.fidriyanto.banktracker.ui.feed

import com.fidriyanto.banktracker.domain.model.BudgetLine
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.ui.add.TransactionForm
import com.fidriyanto.banktracker.ui.add.TxFormError
import com.fidriyanto.banktracker.ui.add.TxFormValues
import com.fidriyanto.banktracker.ui.add.TxType
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.subcategoriesFor
import java.time.LocalDate

// ac: transaction-form-redesign — the Edit sheet uses the shared TransactionForm. onDelete shows a
// Delete button in the header when the caller can delete.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionBottomSheet(
    transaction: TransactionUiModel,
    onDismiss: () -> Unit,
    onSave: (TransactionEdit) -> Unit,
    installments: List<Installment> = emptyList(),
    onDelete: (() -> Unit)? = null,
    budgetLines: List<BudgetLine> = emptyList(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // ac: category-picker — the most used categories, loaded once for the screen
    val frequentVm: EditFrequentCategoriesViewModel = hiltViewModel()
    val frequent by frequentVm.frequentByType.collectAsStateWithLifecycle()

    // ac: edit-transaction-from-feed — bottom sheet is pre-populated with the row's current values
    // ac: add-transfer-target-amount — preload destination wallet and received amount so editing a
    // cross-currency transfer keeps them instead of clearing them.
    var form by remember {
        mutableStateOf(
            TxFormValues(
                wallet = transaction.wallet?.let { id -> AccountStore.all.firstOrNull { it.id == id } } ?: AccountStore.all.first(),
                txType = TxType.entries.firstOrNull { it.id == transaction.txType } ?: TxType.entries.first(),
                toWallet = transaction.toWallet?.let { id -> AccountStore.all.firstOrNull { it.id == id } },
                toAmount = transaction.toAmount?.let { formatAmountForEdit(it) } ?: "",
                amount = formatAmountForEdit(transaction.amount),
                description = transaction.item,
                category = transaction.category,
                subcategory = transaction.subcategory,
                date = runCatching { LocalDate.parse(transaction.dateIso.take(10)) }.getOrDefault(LocalDate.now()),
            )
        )
    }

    // ac: transaction-form-validation — Save is refused with a message instead of keeping the old amount
    var validationError by remember { mutableStateOf<TxFormError?>(null) }

    // ac: installment-link-edit — the link shown in the sheet. The user can remove it or pick another plan.
    var linkedId by remember { mutableStateOf(transaction.installmentId) }

    ModalBottomSheet(
        // ac: edit-transaction-from-feed — dismissing the sheet without Save discards changes
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        TransactionForm(
            title = stringResource(R.string.tx_form_edit_title),
            values = form,
            onChange = { form = it; validationError = null },
            validationError = validationError,
            budgetLines = budgetLines,
            frequentByType = frequent,
            onClose = onDismiss,
            onDelete = onDelete,
            submitLabel = stringResource(R.string.tx_form_save_changes),
            onSubmit = {
                val f = form
                f.validate()?.let { validationError = it; return@TransactionForm }
                // ac: edit-transaction-from-feed — Save emits the edit; FeedViewModel applies it optimistically then PATCHes the server
                onSave(
                    TransactionEdit(
                        amount = f.amount.toDouble(),
                        item = f.description,
                        category = f.category,
                        dateIso = f.date.toString(),
                        wallet = f.wallet.id,
                        txType = f.txType.id,
                        toWallet = if (f.txType == TxType.TRANSFER) f.toWallet?.id else null,
                        // ac: edit-transfer-target-amount — to_amount cleared when not a cross-currency transfer
                        toAmount = if (f.isCrossCurrencyTransfer) f.toAmount.toDoubleOrNull() else null,
                        // ac: add-transaction-subcategory — persist the edited subcategory
                        subcategory = if (subcategoriesFor(f.category).isEmpty()) null else f.subcategory,
                        // ac: installment-link-edit — a plan picked by hand, an unlink, or an automatic match for an unlinked expense
                        installmentId = linkedId.takeIf { it != transaction.installmentId },
                        unlinkInstallment = transaction.installmentId != null && linkedId == null,
                        autoLinkInstallment = transaction.installmentId == null && linkedId == null && f.txType == TxType.EXPENSE,
                    )
                )
            },
        ) {
            // ac: installment-link-edit — card expenses show their installment link, or a picker when unlinked
            if (form.txType == TxType.EXPENSE) {
                InstallmentLinkSection(
                    linked = installments.firstOrNull { it.id == linkedId },
                    linkedUnknown = linkedId != null && installments.none { it.id == linkedId },
                    candidates = installments.filter { it.wallet == form.wallet.id && !it.isCompleted },
                    onLink = { linkedId = it.id },
                    onUnlink = { linkedId = null },
                )
            }
        }
    }
}

private fun formatAmountForEdit(amount: Double): String {
    return if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
}
