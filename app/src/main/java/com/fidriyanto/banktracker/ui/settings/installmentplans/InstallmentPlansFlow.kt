package com.fidriyanto.banktracker.ui.settings.installmentplans

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Where the user is inside Installment plans: the list, or the editor of a new plan.
private enum class PlansPage { List, Editor }

// ac: installment-plan-add, installment-plan-delete
@Composable
fun InstallmentPlansFlow(onBack: () -> Unit) {
    var page by remember { mutableStateOf(PlansPage.List) }
    // Each editor and each list that opens gets its own view model, so it never starts with the state of the last visit.
    var opens by remember { mutableIntStateOf(0) }
    val go = { next: PlansPage -> opens++; page = next }
    when (page) {
        PlansPage.List -> InstallmentPlansScreen(onBack = onBack, onAdd = { go(PlansPage.Editor) }, vmKey = "plans-list-$opens")
        PlansPage.Editor -> InstallmentPlanEditorScreen(onClose = { go(PlansPage.List) }, vmKey = "plans-editor-$opens")
    }
}
