package com.fidriyanto.banktracker.ui.settings.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.ui.common.CategoryPicker
import com.fidriyanto.banktracker.ui.common.PickerCategory
import com.fidriyanto.banktracker.ui.settings.rules.ErrorText
import com.fidriyanto.banktracker.ui.settings.rules.OpenCard
import com.fidriyanto.banktracker.ui.settings.rules.OptionCard
import com.fidriyanto.banktracker.ui.settings.rules.StepHeading
import com.fidriyanto.banktracker.ui.theme.Space

private val kinds = listOf(KIND_FLEX to R.string.budget_kind_flex, KIND_FIXED to R.string.budget_kind_fixed)

// Step 1: the type of budget and what it covers. A line that matches by text keeps its rule, so it shows a note and no category.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetWhatStep(d: BudgetDraft, showErrors: Boolean, categories: List<Category>, frequentByType: Map<String, List<String>>, onEdit: ((BudgetDraft) -> BudgetDraft) -> Unit) {
    val error = if (showErrors) d.whatError() else null
    val chosen = categories.firstOrNull { it.name == d.category }
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.budget_what_title), stringResource(R.string.budget_what_body))
        if (d.byRules) Text(stringResource(R.string.budget_rules_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            kinds.forEach { (id, label) ->
                FilterChip(selected = d.kind == id, onClick = { onEdit { it.copy(kind = id, dueOn = it.dueOn && id == KIND_FIXED) } }, label = { Text(stringResource(label)) })
            }
        }
        Text(stringResource(if (d.kind == KIND_FIXED) R.string.budget_kind_fixed_hint else R.string.budget_kind_flex_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!d.byRules) {
            Text(stringResource(R.string.budget_category), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            // A saved line keeps its category on screen even when the category was removed since.
            CategoryPicker(categories.map { PickerCategory(it.name, it.subcategories) }, frequentByType["expense"].orEmpty(), d.category) { name, s ->
                onEdit { it.copy(category = name, subOn = s != null, subcategory = s) }
            }
            if (error == BudgetError.CATEGORY) ErrorText(stringResource(R.string.budget_error_category))
            if (chosen != null && chosen.subcategories.isNotEmpty()) {
                if (d.subOn) {
                    OpenCard(Icons.Outlined.Category, stringResource(R.string.budget_sub_title), onRemove = { onEdit { it.copy(subOn = false, subcategory = null) } }) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.padding(top = Space.xs)) {
                            chosen.subcategories.forEach { s -> FilterChip(selected = d.subcategory == s, onClick = { onEdit { it.copy(subcategory = s) } }, label = { Text(s) }) }
                        }
                    }
                } else {
                    OptionCard(Icons.Outlined.Category, stringResource(R.string.budget_sub_title), stringResource(R.string.budget_sub_hint, chosen.name), stringResource(R.string.budget_sub_add)) { onEdit { it.copy(subOn = true) } }
                }
            }
        }
        OutlinedTextField(
            value = d.name, onValueChange = { v -> onEdit { it.copy(name = v) } }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.budget_name)) },
            supportingText = { Text(stringResource(if (d.byRules) R.string.budget_name_hint_rules else R.string.budget_name_hint)) },
        )
    }
}
