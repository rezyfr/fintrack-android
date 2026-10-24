package com.fidriyanto.banktracker.ui.settings.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.settings.schedules.IconBadge
import com.fidriyanto.banktracker.ui.theme.Space

// The budget lines, grouped as Flexible and Fixed. A tap opens the line in the editor.
@Composable
fun BudgetsScreen(onBack: () -> Unit, onNew: () -> Unit, onEdit: (Long) -> Unit, viewModel: BudgetsViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = Space.sm, end = Space.lg, top = Space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.budgets_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.lg, vertical = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            if (s.failed) Text(stringResource(R.string.budgets_load_failed), color = MaterialTheme.colorScheme.error)
            if (!s.loading && !s.failed && s.lines.isEmpty()) EmptyBudgets()
            BudgetGroup(R.string.budgets_group_flex, s.flexible, onEdit)
            BudgetGroup(R.string.budgets_group_fixed, s.fixed, onEdit)
        }
        Button(onClick = onNew, Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md)) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(stringResource(R.string.budgets_new), Modifier.padding(start = Space.sm))
        }
    }
}

@Composable
private fun EmptyBudgets() {
    Column(Modifier.fillMaxWidth().padding(vertical = Space.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.md)) {
        IconBadge(Icons.Outlined.PieChart, size = 64)
        Text(stringResource(R.string.budgets_empty_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.budgets_empty_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BudgetGroup(title: Int, lines: List<BudgetLine>, onEdit: (Long) -> Unit) {
    if (lines.isEmpty()) return
    Text(stringResource(title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    lines.forEach { BudgetRow(it, onClick = { onEdit(it.id) }) }
}

@Composable
private fun BudgetRow(line: BudgetLine, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            IconBadge(Icons.Outlined.PieChart)
            Column(Modifier.weight(1f)) {
                Text(line.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(coverText(line), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatMoney(line.currency, line.target), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// What the line covers: the category and subcategory, or a note for a line that matches by text
@Composable
private fun coverText(line: BudgetLine): String = when {
    line.category == null -> stringResource(R.string.budgets_matches_text)
    line.subcategory == null -> line.category
    else -> stringResource(R.string.budgets_cover_sub, line.category, line.subcategory)
}
