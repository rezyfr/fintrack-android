package com.fidriyanto.banktracker.ui.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

private fun formatAmount(currency: String, amount: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    return when (currency) {
        "THB" -> {
            fmt.minimumFractionDigits = 0
            fmt.maximumFractionDigits = 0
            "฿${fmt.format(amount.roundToLong())}"
        }
        else -> "Rp ${fmt.format(amount.roundToLong())}"
    }
}

// ac: set-category-budget — a Budget screen is accessible from the bottom navigation bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val pullState = rememberPullToRefreshState()
    LaunchedEffect(pullState.isRefreshing) { if (pullState.isRefreshing) viewModel.refresh() }
    LaunchedEffect(state.isRefreshing) { if (!state.isRefreshing && pullState.isRefreshing) pullState.endRefresh() }

    Box(Modifier.nestedScroll(pullState.nestedScrollConnection)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.budget_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp),
            )

            // ac: set-category-budget — currency selector lets the user switch between THB and IDR budgets
            CurrencySelector(
                selected = state.currency,
                onSelect = viewModel::selectCurrency,
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                SummaryCard(state = state)

                state.rows.forEach { row ->
                    BudgetCategoryCard(
                        row = row,
                        currency = state.currency,
                        isEditing = state.editingCategory == row.category,
                        editValue = state.editValue,
                        isSaving = state.isSaving,
                        onTap = { viewModel.startEditing(row.category) },
                        onEditChange = viewModel::updateEditValue,
                        onSave = viewModel::saveEdit,
                        onCancel = viewModel::cancelEditing,
                    )
                }

                state.error?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        PullToRefreshContainer(pullState, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun CurrencySelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        stringResource(R.string.budget_currency_thb) to "THB",
        stringResource(R.string.budget_currency_idr) to "IDR",
    )
    val selectedIndex = options.indexOfFirst { it.second == selected }.coerceAtLeast(0)
    TabRow(selectedTabIndex = selectedIndex) {
        options.forEachIndexed { index, (label, code) ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(code) },
                text = { Text(label) },
            )
        }
    }
}

@Composable
private fun SummaryCard(state: BudgetUiState) {
    val colors = LocalAppColors.current
    val progress = if (state.totalBudget > 0)
        (state.totalSpent / state.totalBudget).toFloat().coerceIn(0f, 1f)
    else 0f
    val isOver = state.totalBudget > 0 && state.totalSpent > state.totalBudget
    val progressColor = if (isOver) MaterialTheme.colorScheme.error else colors.gold

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        stringResource(R.string.budget_total_label),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatAmount(state.currency, state.totalBudget),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.budget_spent_label),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatAmount(state.currency, state.totalSpent),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            // ac: set-category-budget — a linear progress bar in each row shows how much of the budget has been used
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
private fun BudgetCategoryCard(
    row: BudgetCategoryRow,
    currency: String,
    isEditing: Boolean,
    editValue: String,
    isSaving: Boolean,
    onTap: () -> Unit,
    onEditChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalAppColors.current
    // ac: set-category-budget — rows where spending exceeds the budget are highlighted in the error colour
    val containerColor = if (row.isOverBudget)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
    else
        MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.category,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (row.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                if (row.budgetLimit > 0) {
                    Text(
                        "${formatAmount(currency, row.amountSpent)} / ${formatAmount(currency, row.budgetLimit)}",
                        fontSize = 12.sp,
                        color = if (row.isOverBudget)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        stringResource(R.string.budget_no_limit),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (row.budgetLimit > 0) {
                val progressColor = if (row.isOverBudget) MaterialTheme.colorScheme.error else colors.gold
                // ac: set-category-budget — a linear progress bar in each row shows how much of the budget has been used
                LinearProgressIndicator(
                    progress = { row.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                )
            }

            // ac: set-category-budget — tapping a row opens an inline edit field to update the budget limit
            AnimatedVisibility(visible = isEditing) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = onEditChange,
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.budget_edit_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onSave() }),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    )
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = onSave) {
                            Text(stringResource(R.string.action_save))
                        }
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            }
        }
    }
}
