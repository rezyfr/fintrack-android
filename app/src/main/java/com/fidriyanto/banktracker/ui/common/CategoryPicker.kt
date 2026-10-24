package com.fidriyanto.banktracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space
import com.fidriyanto.banktracker.ui.theme.categoryColor

// ac: category-picker - one category picker for the Add and Edit forms and for the schedule, budget
// and rule editors. A compact row shows the selected category and the most used ones. The All chip
// opens a full-screen list with search. onPick gets the category and, when the user tapped one, its subcategory.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPicker(
    categories: List<PickerCategory>,
    frequent: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    onPick: (category: String, subcategory: String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    FlowRow(
        modifier = modifier.testTag("category_row"),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        chipNames(categories, frequent, selected).forEach { name ->
            CategoryChip(name, selected = name == selected) { onPick(name, null) }
        }
        AllChip { open = true }
    }
    if (open) {
        CategoryPickerDialog(
            categories = categories,
            selected = selected,
            onPick = { category, sub -> open = false; onPick(category, sub) },
            onDismiss = { open = false },
        )
    }
}

@Composable
fun CategoryChip(category: String, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .clip(AppShape.pill)
            .background(container)
            .then(if (selected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShape.pill))
            .clickable(onClick = onClick)
            .padding(start = Space.sm, end = Space.lg, top = Space.sm, bottom = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category)
        Spacer(Modifier.width(10.dp))
        Text(category, color = content, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun CategoryBadge(category: String) {
    Box(Modifier.size(32.dp).clip(CircleShape).background(categoryColor(category)), contentAlignment = Alignment.Center) {
        Icon(categoryIcon(category), contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AllChip(onClick: () -> Unit) {
    Row(
        Modifier
            .testTag("category_all")
            .clip(AppShape.pill)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShape.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.category_picker_all), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
