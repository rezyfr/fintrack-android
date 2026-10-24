package com.fidriyanto.banktracker.ui.add

import androidx.compose.material.icons.outlined.Savings
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import com.fidriyanto.banktracker.domain.usecase.BudgetAttribution
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.BudgetLine
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.NO_ACCOUNT
import com.fidriyanto.banktracker.domain.model.currencySymbol
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.PickDateDialog
import com.fidriyanto.banktracker.ui.common.SegmentedToggle
import com.fidriyanto.banktracker.ui.common.PickerCategory
import com.fidriyanto.banktracker.ui.common.CategoryPicker as SharedCategoryPicker
import com.fidriyanto.banktracker.ui.common.subcategoriesFor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space

// ac: transaction-form-redesign — the values the Add and Edit forms share
data class TxFormValues(
    val wallet: Account = NO_ACCOUNT,
    val txType: TxType = TxType.EXPENSE,
    val toWallet: Account? = null,
    // ac: add-transfer-target-amount — received amount for a cross-currency transfer (destination currency)
    val toAmount: String = "",
    val amount: String = "",
    val description: String = "",
    val category: String = "Other",
    val subcategory: String? = null,
    val date: LocalDate = LocalDate.now(),
) {
    val isCrossCurrencyTransfer: Boolean
        get() = txType == TxType.TRANSFER && toWallet != null && toWallet.currency != wallet.currency

    // ac: transaction-form-validation — the same required fields as the web form. Returns the first
    // problem, or null when the values can be saved.
    fun validate(): TxFormError? = when {
        wallet.id.isEmpty() -> TxFormError.WALLET
        (amount.toDoubleOrNull() ?: 0.0) <= 0.0 -> TxFormError.AMOUNT
        description.isBlank() -> TxFormError.DESCRIPTION
        txType == TxType.TRANSFER && toWallet == null -> TxFormError.TO_WALLET
        isCrossCurrencyTransfer && (toAmount.toDoubleOrNull() ?: 0.0) <= 0.0 -> TxFormError.RECEIVED
        else -> null
    }
}

enum class TxFormError { WALLET, AMOUNT, DESCRIPTION, TO_WALLET, RECEIVED }

private val TxFormError.messageRes: Int
    get() = when (this) {
        TxFormError.WALLET -> R.string.tx_form_error_wallet
        TxFormError.AMOUNT -> R.string.tx_form_error_amount
        TxFormError.DESCRIPTION -> R.string.tx_form_error_description
        TxFormError.TO_WALLET -> R.string.tx_form_error_to_wallet
        TxFormError.RECEIVED -> R.string.tx_form_error_received
    }

// ac: transaction-form-redesign — one form for Add and Edit: a header with close and an optional
// delete, the type toggle, a large amount, category chips, a card of detail rows, and a save button
// at the bottom. extraContent sits between the card and the save button.
@Composable
fun TransactionForm(
    title: String,
    values: TxFormValues,
    onChange: (TxFormValues) -> Unit,
    onClose: () -> Unit,
    submitLabel: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    submitting: Boolean = false,
    error: String? = null,
    validationError: TxFormError? = null,
    walletOptions: List<Account> = AccountStore.all,
    budgetLines: List<BudgetLine> = emptyList(),
    // ac: category-picker — the most used category names for each type id, loaded once by the screen
    frequentByType: Map<String, List<String>> = emptyMap(),
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl, vertical = Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        FormHeader(title, onClose, onDelete)

        // ac: transaction-form-redesign — changing the type resets the category to that type's first one
        val types = TxType.entries
        SegmentedToggle(
            options = types.map { stringResource(it.labelRes) },
            selectedIndex = types.indexOf(values.txType),
            onSelect = { i ->
                val picked = types[i]
                if (picked != values.txType) {
                    onChange(values.copy(txType = picked, toWallet = null, toAmount = "", category = categoriesFor(picked).first(), subcategory = null))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        AmountInput(values, onChange)

        if (values.txType != TxType.TRANSFER) CategoryPicker(values, frequentByType, onChange)

        DetailsCard(values, onChange, walletOptions, budgetLines)

        extraContent()

        (validationError?.let { stringResource(it.messageRes) } ?: error)?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = onSubmit,
            enabled = !submitting,
            shape = AppShape.xxl,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            if (submitting) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(submitLabel, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private val TxType.labelRes: Int
    get() = when (this) {
        TxType.EXPENSE -> R.string.tx_type_expense
        TxType.INCOME -> R.string.tx_type_income
        TxType.TRANSFER -> R.string.tx_type_transfer
        TxType.INVESTMENT -> R.string.tx_type_investment
    }

@Composable
private fun FormHeader(title: String, onClose: () -> Unit, onDelete: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(72.dp)) {
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.tx_form_close))
            }
        }
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.width(72.dp), contentAlignment = Alignment.CenterEnd) {
            if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.tx_form_delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ac: transaction-form-redesign — the amount in large type with the wallet's currency symbol. An
// expense shows a minus sign and income a plus sign. Digits are grouped by thousands as you type.
@Composable
private fun AmountInput(values: TxFormValues, onChange: (TxFormValues) -> Unit) {
    val sign = when (values.txType) {
        TxType.EXPENSE -> "−"
        TxType.INCOME -> "+"
        else -> ""
    }
    val big = MaterialTheme.typography.displayLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Start,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(
            sign + currencySymbol(values.wallet.currency).trim(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = values.amount,
            onValueChange = { raw -> cleanAmount(raw)?.let { onChange(values.copy(amount = it)) } },
            textStyle = big,
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            visualTransformation = ThousandsTransformation,
            modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 40.dp),
            decorationBox = { inner ->
                Box {
                    if (values.amount.isEmpty()) Text("0", style = big.copy(color = MaterialTheme.colorScheme.outline))
                    inner()
                }
            },
        )
    }
}

// Digits and at most one decimal point. Returns null to reject the edit.
private fun cleanAmount(raw: String): String? {
    val kept = raw.filter { it.isDigit() || it == '.' }
    return kept.takeIf { it.count { c -> c == '.' } <= 1 }
}

// Shows "1234567.5" as "1,234,567.5" and maps the cursor across the inserted commas.
private object ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val dot = raw.indexOf('.').let { if (it < 0) raw.length else it }
        val out = StringBuilder()
        raw.forEachIndexed { i, c ->
            if (i in 1 until dot && (dot - i) % 3 == 0) out.append(',')
            out.append(c)
        }
        val shown = out.toString()
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var seen = 0
                shown.forEachIndexed { t, c ->
                    if (seen == offset) return t
                    if (c != ',') seen++
                }
                return shown.length
            }

            override fun transformedToOriginal(offset: Int): Int = shown.take(offset).count { it != ',' }
        }
        return TransformedText(AnnotatedString(shown), mapping)
    }
}

// ac: category-picker — the category as the shared picker: the most used categories as chips and an
// All chip that opens the full list. When the category has subcategories, a second row of chips
// picks one. Tapping the selected subcategory clears it.
@Composable
private fun CategoryPicker(values: TxFormValues, frequentByType: Map<String, List<String>>, onChange: (TxFormValues) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.tx_form_category_label) + " ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            Text(values.category, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        }
        val withSubs = values.txType == TxType.EXPENSE
        val categories = categoriesFor(values.txType).map { PickerCategory(it, if (withSubs) subcategoriesFor(it) else emptyList()) }
        SharedCategoryPicker(categories, frequentByType[values.txType.id].orEmpty(), values.category) { cat, sub ->
            if (cat != values.category || sub != null) onChange(values.copy(category = cat, subcategory = sub))
        }
        val subs = subcategoriesFor(values.category)
        if (subs.isNotEmpty()) {
            val subState = rememberLazyListState()
            LaunchedEffect(values.category) {
                val i = subs.indexOf(values.subcategory)
                if (i >= 0) subState.scrollToItem(i)
            }
            LazyRow(state = subState, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                items(subs) { sub ->
                    FilterChip(
                        selected = values.subcategory == sub,
                        onClick = { onChange(values.copy(subcategory = if (values.subcategory == sub) null else sub)) },
                        label = { Text(sub, style = MaterialTheme.typography.bodyMedium) },
                        shape = AppShape.pill,
                    )
                }
            }
        }
    }
}

// ac: transaction-form-redesign — wallet, transfer target, received amount, date and description as
// rows in one rounded card
@Composable
private fun DetailsCard(values: TxFormValues, onChange: (TxFormValues) -> Unit, walletOptions: List<Account>, budgetLines: List<BudgetLine>) {
    val transfer = values.txType == TxType.TRANSFER
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = AppShape.xl, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = Space.lg)) {
            WalletRow(
                icon = Icons.Outlined.AccountBalanceWallet,
                label = stringResource(if (transfer) R.string.tx_form_from_wallet else R.string.tx_form_wallet),
                selected = values.wallet.takeIf { it.id.isNotEmpty() },
                options = walletOptions,
                onPick = { w -> onChange(values.copy(wallet = w, toWallet = values.toWallet?.takeIf { it != w })) },
            )
            if (transfer) {
                RowDivider()
                WalletRow(
                    icon = Icons.Outlined.SwapHoriz,
                    label = stringResource(R.string.tx_form_to_wallet),
                    selected = values.toWallet,
                    options = AccountStore.all.filter { it != values.wallet },
                    onPick = { w -> onChange(values.copy(toWallet = w)) },
                )
            }
            // ac: add-transfer-target-amount — a cross-currency transfer also asks for the amount received
            val dest = values.toWallet
            if (values.isCrossCurrencyTransfer && dest != null) {
                RowDivider()
                DetailRow(Icons.Outlined.SwapHoriz, stringResource(R.string.tx_form_received, dest.currency)) {
                    InlineField(
                        value = values.toAmount,
                        onValueChange = { raw -> cleanAmount(raw)?.let { onChange(values.copy(toAmount = it)) } },
                        placeholder = "0",
                        keyboardType = KeyboardType.Decimal,
                        alignEnd = true,
                    )
                }
            }
            // ac: budget-by-category — which budget this expense counts toward. It follows the category,
            // so the way to move it is to pick another category or subcategory above.
            if (values.txType == TxType.EXPENSE && budgetLines.isNotEmpty()) {
                RowDivider()
                BudgetRow(values, budgetLines)
            }
            RowDivider()
            DateRow(values.date) { onChange(values.copy(date = it)) }
            RowDivider()
            DetailRow(Icons.Outlined.Edit, label = null) {
                InlineField(
                    value = values.description,
                    onValueChange = { onChange(values.copy(description = it)) },
                    placeholder = stringResource(R.string.tx_form_description_hint),
                )
            }
        }
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

@Composable
private fun DetailRow(icon: ImageVector, label: String?, onClick: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        if (label != null) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(12.dp))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

@Composable
private fun RowValue(text: String, onClick: Boolean) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    if (onClick) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Space.xs).size(20.dp),
        )
    }
}

@Composable
private fun WalletRow(icon: ImageVector, label: String, selected: Account?, options: List<Account>, onPick: (Account) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        DetailRow(icon, label, onClick = { open = true }) {
            RowValue(selected?.displayName ?: stringResource(R.string.tx_form_choose), onClick = true)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.align(Alignment.CenterEnd)) {
            options.forEach { w ->
                DropdownMenuItem(
                    text = { Text(w.displayName) },
                    trailingIcon = { Text(currencySymbol(w.currency).trim(), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { onPick(w); open = false },
                )
            }
        }
    }
}

// ac: transaction-form-redesign — the date reads Today or Yesterday when it is one of those days
@Composable
private fun DateRow(date: LocalDate, onPick: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val short = date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "d MMM" else "d MMM yyyy", Locale.US))
    val label = when (date) {
        today -> stringResource(R.string.tx_form_today, short)
        today.minusDays(1) -> stringResource(R.string.tx_form_yesterday, short)
        else -> date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "EEE, d MMM" else "EEE, d MMM yyyy", Locale.US))
    }
    DetailRow(Icons.Outlined.CalendarToday, stringResource(R.string.tx_form_date), onClick = { open = true }) {
        RowValue(label, onClick = true)
    }
    if (open) PickDateDialog(date, onPick = onPick, onDismiss = { open = false })
}

@Composable
private fun RowScope.InlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    alignEnd: Boolean = false,
) {
    val style = MaterialTheme.typography.titleLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = if (alignEnd) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (keyboardType == KeyboardType.Decimal) ThousandsTransformation else VisualTransformation.None,
        modifier = Modifier.weight(1f).padding(vertical = Space.lg),
        decorationBox = { inner ->
            Box(contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Normal))
                inner()
            }
        },
    )
}

@Composable
private fun BudgetRow(values: TxFormValues, budgetLines: List<BudgetLine>) {
    val probe = TransactionUiModel(
        id = 0, item = values.description, category = values.category, amount = values.amount.toDoubleOrNull() ?: 0.0,
        dateIso = values.date.toString(), wallet = values.wallet.id, txType = values.txType.id,
        status = TransactionStatus.PENDING_SYNC, subcategory = values.subcategory,
    )
    val currencyByWallet = AccountStore.all.associate { it.id to it.currency }
    val line = BudgetAttribution.lineFor(probe, budgetLines, currencyByWallet, CurrencySettingsStore.current.homeCode)
    DetailRow(Icons.Outlined.Savings, stringResource(R.string.tx_form_budget)) {
        Text(
            line?.name ?: stringResource(R.string.feed_no_budget),
            color = if (line == null) LocalAppColors.current.warning else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
