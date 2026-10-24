package com.fidriyanto.banktracker.ui.settings.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.ui.common.CategoryPicker
import com.fidriyanto.banktracker.ui.common.PickerCategory
import com.fidriyanto.banktracker.ui.theme.Space

// The second step. The category is required. The rest turns on with its own button.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RuleThenStep(d: RuleDraft, categories: List<Category>, frequentByType: Map<String, List<String>>, showErrors: Boolean, onEdit: ((RuleDraft) -> RuleDraft) -> Unit) {
    val error = if (showErrors) d.thenError() else null
    val chosen = categories.firstOrNull { it.name == d.category }
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.rules_then_title), stringResource(R.string.rules_then_body))
        if (d.ignore) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text(stringResource(R.string.rules_ignore_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.rules_ignore_on), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { onEdit { it.copy(ignore = false) } }) { Text(stringResource(R.string.rules_ignore_off)) }
                }
            }
            return@Column
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text(stringResource(R.string.rules_category_label), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                CategoryPicker(categories.map { PickerCategory(it.name, it.subcategories) }, frequentByType["expense"].orEmpty(), d.category.orEmpty()) { name, s ->
                    onEdit { it.copy(category = name, subOn = s != null, subcategory = s) }
                }
                if (error == ThenError.CATEGORY) ErrorText(stringResource(R.string.rules_error_category))
            }
        }
        if (chosen != null && chosen.subcategories.isNotEmpty()) {
            if (d.subOn) OpenCard(Icons.Outlined.Category, stringResource(R.string.rules_sub_title), onRemove = { onEdit { it.copy(subOn = false, subcategory = null) } }) {
                Text(stringResource(R.string.rules_sub_hint, chosen.name), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    chosen.subcategories.forEach { s -> FilterChip(selected = d.subcategory == s, onClick = { onEdit { it.copy(subcategory = s) } }, label = { Text(s) }) }
                }
            } else OptionCard(Icons.Outlined.Category, stringResource(R.string.rules_sub_title), stringResource(R.string.rules_sub_hint, chosen.name), stringResource(R.string.rules_sub_add)) { onEdit { it.copy(subOn = true) } }
        }
        if (d.descOn) OpenCard(Icons.Outlined.Edit, stringResource(R.string.rules_desc_title), onRemove = { onEdit { it.copy(descOn = false, description = "") } }) {
            OutlinedTextField(d.description, { v -> onEdit { it.copy(description = v) } }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.rules_desc_label)) }, singleLine = true)
            Text(stringResource(R.string.rules_insert), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                AssistChip(onClick = { onEdit { it.copy(description = it.description + "{merchant}") } }, label = { Text(stringResource(R.string.rules_insert_merchant)) })
                AssistChip(onClick = { onEdit { it.copy(description = it.description + "{amount}") } }, label = { Text(stringResource(R.string.rules_insert_amount)) })
            }
            if (error == ThenError.DESCRIPTION) ErrorText(stringResource(R.string.rules_error_description))
        } else OptionCard(Icons.Outlined.Edit, stringResource(R.string.rules_desc_title), stringResource(R.string.rules_desc_hint), stringResource(R.string.rules_desc_add)) { onEdit { it.copy(descOn = true) } }
        OptionCard(Icons.Outlined.VisibilityOff, stringResource(R.string.rules_ignore_title), stringResource(R.string.rules_ignore_hint), stringResource(R.string.rules_ignore_add)) {
            onEdit { it.copy(ignore = true, category = null, subOn = false, subcategory = null, descOn = false, description = "") }
        }
    }
}
