package com.fidriyanto.banktracker.ui.add

import com.fidriyanto.banktracker.domain.model.INCOME_CATEGORIES
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.BookStore
import com.fidriyanto.banktracker.ui.common.CategoryStore
import com.fidriyanto.banktracker.ui.common.holdsWallet
import com.fidriyanto.banktracker.ui.common.rememberConfirm

internal val INVESTMENT_CATEGORIES = listOf("Investment", "Dividends", "Other")

// ac: manage-categories — expense categories come from CategoryStore instead of a compiled-in list
internal fun categoriesFor(txType: TxType): List<String> = when (txType) {
    TxType.EXPENSE -> CategoryStore.expenseCategoryNames
    TxType.INCOME -> INCOME_CATEGORIES
    // ac: transfer-uses-transfer-category — transfers use the category "Transfer", matching web and the DB
    TxType.TRANSFER -> listOf("Transfer")
    TxType.INVESTMENT -> INVESTMENT_CATEGORIES
}

// ac: transaction-form-redesign — the Add form uses the shared TransactionForm
@Composable
fun AddScreen(
    viewModel: AddViewModel = hiltViewModel(),
    onClose: () -> Unit = {},
    onSaved: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val book by BookStore.book.collectAsStateWithLifecycle()
    val budgetLines by viewModel.budgetLines.collectAsStateWithLifecycle()
    val suggestion by viewModel.suggestion.collectAsStateWithLifecycle()
    val frequent by viewModel.frequentByType.collectAsStateWithLifecycle()
    // ac: haptic-feedback — a firmer confirm when the transaction is saved
    val confirm = rememberConfirm()

    // ac: four-tab-nav-with-add-fab — a successful save closes the add bottom sheet and resets the form
    LaunchedEffect(state.successMessage) {
        if (!state.successMessage.isNullOrEmpty()) {
            confirm()
            onSaved()
            viewModel.reset()
        }
    }

    TransactionForm(
        title = stringResource(R.string.tx_form_add_title),
        values = state.form,
        onChange = viewModel::updateForm,
        onClose = onClose,
        submitLabel = stringResource(R.string.tx_form_save),
        onSubmit = { viewModel.submit() },
        submitting = state.isLoading,
        error = state.errorMessage,
        validationError = state.validationError,
        // ac: currency-books — the wallet list holds the book's accounts only
        walletOptions = AccountStore.all.filter { book.holdsWallet(it.id) },
        budgetLines = budgetLines,
        frequentByType = frequent,
        extraContent = {
            // ac: on-device-ai — hidden when the suggestion is what the form already has
            suggestion?.takeIf { it.category != state.form.category || it.subcategory != state.form.subcategory }?.let {
                AssistChip(
                    onClick = viewModel::applySuggestion,
                    label = { Text(stringResource(R.string.ai_suggest_chip, listOfNotNull(it.category, it.subcategory).joinToString(" › "))) },
                    leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        },
    )
}
