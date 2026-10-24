package com.fidriyanto.banktracker.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.ui.theme.Space

@Composable
fun ManageCategoriesScreen(onBack: () -> Unit, viewModel: ManageCategoriesViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showAddCategory by remember { mutableStateOf(false) }
    var addSubcategoryFor by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.manage_categories_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            items(categories, key = { it.name }) { category ->
                CategoryRow(
                    category = category,
                    onDelete = { viewModel.delete(category) },
                    onDeleteSubcategory = { sub -> viewModel.deleteSubcategory(category.name, sub) },
                    onAddSubcategory = { addSubcategoryFor = category.name },
                )
            }
        }

        OutlinedButton(onClick = { showAddCategory = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.manage_categories_add))
        }
    }

    if (showAddCategory) {
        AddNameDialog(
            title = stringResource(R.string.manage_categories_add),
            label = stringResource(R.string.manage_categories_name_label),
            onDismiss = { showAddCategory = false },
            onConfirm = { name -> viewModel.addCategory(name); showAddCategory = false },
        )
    }

    addSubcategoryFor?.let { category ->
        AddNameDialog(
            title = stringResource(R.string.manage_categories_add_subcategory),
            label = stringResource(R.string.manage_categories_subcategory_name_label),
            onDismiss = { addSubcategoryFor = null },
            onConfirm = { name -> viewModel.addSubcategory(category, name); addSubcategoryFor = null },
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onDelete: () -> Unit,
    onDeleteSubcategory: (String) -> Unit,
    onAddSubcategory: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (category.isDefault) "${category.name} · ${stringResource(R.string.manage_categories_default_badge)}" else category.name,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // ac: manage-categories — a default category cannot be deleted
                if (!category.isDefault) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                category.subcategories.forEach { sub ->
                    AssistChip(
                        onClick = { onDeleteSubcategory(sub) },
                        label = { Text(sub, style = MaterialTheme.typography.bodySmall) },
                    )
                }
                AssistChip(
                    onClick = onAddSubcategory,
                    label = { Text(stringResource(R.string.manage_categories_add_subcategory), style = MaterialTheme.typography.bodySmall) },
                )
            }
        }
    }
}

@Composable
private fun AddNameDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
