package com.fidriyanto.banktracker.ui.settings.budgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Where the user is inside Budgets: the list, or the editor of one line. A new line has no id.
private sealed interface BudgetsPage {
    data object List : BudgetsPage
    data class Editor(val lineId: Long?) : BudgetsPage
}

@Composable
fun BudgetsFlow(onBack: () -> Unit) {
    var page by remember { mutableStateOf<BudgetsPage>(BudgetsPage.List) }
    // Each editor that opens gets its own view model, so it never starts with the state of the last visit.
    var opens by remember { mutableIntStateOf(0) }
    val go = { next: BudgetsPage -> opens++; page = next }
    when (val p = page) {
        BudgetsPage.List -> BudgetsScreen(onBack = onBack, onNew = { go(BudgetsPage.Editor(null)) }, onEdit = { go(BudgetsPage.Editor(it)) })
        is BudgetsPage.Editor -> BudgetEditorScreen(p.lineId, onClose = { go(BudgetsPage.List) }, vmKey = "budget-$opens")
    }
}
