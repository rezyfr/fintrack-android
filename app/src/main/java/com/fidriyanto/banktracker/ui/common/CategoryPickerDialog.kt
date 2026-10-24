package com.fidriyanto.banktracker.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space

// ac: category-picker - a full-screen list of every category with a search field. A dialog, not a
// bottom sheet, because the Add form already sits in a bottom sheet. Tapping a category picks it.
// Tapping a subcategory chip picks the category and that subcategory.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPickerDialog(
    categories: List<PickerCategory>,
    selected: String,
    onPick: (category: String, subcategory: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val shown = filterCategories(categories, query)
    LaunchedEffect(Unit) { focus.requestFocus() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.category_picker_search)) },
                        singleLine = true,
                        shape = AppShape.pill,
                        modifier = Modifier.weight(1f).focusRequester(focus).testTag("category_search"),
                    )
                }
                if (shown.isEmpty()) {
                    Text(
                        stringResource(R.string.category_picker_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(Space.lg),
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        items(shown, key = { it.name }) { c ->
                            Column(Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm)) {
                                Row(
                                    Modifier.fillMaxWidth().clickable { onPick(c.name, null) }.padding(vertical = Space.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                                ) {
                                    CategoryBadge(c.name)
                                    Text(
                                        c.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (c.name == selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                                if (c.subcategories.isNotEmpty()) {
                                    FlowRow(
                                        Modifier.padding(top = Space.xs),
                                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                                    ) {
                                        c.subcategories.forEach { sub ->
                                            FilterChip(
                                                selected = false,
                                                onClick = { onPick(c.name, sub) },
                                                label = { Text(sub, style = MaterialTheme.typography.bodyMedium) },
                                                shape = AppShape.pill,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
