package com.fidriyanto.banktracker.ui.settings.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.usecase.RuleEngine
import com.fidriyanto.banktracker.ui.theme.Space

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesScreen(onBack: () -> Unit, onEdit: (String?) -> Unit, onSuggest: () -> Unit, viewModel: RulesViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.rules_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        // The buttons sit under the title and wrap, so a narrow screen or a large font never squeezes the title.
        if (s.rules.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                AssistChip(onClick = onSuggest, label = { Text(stringResource(R.string.rules_suggest_open)) }, leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, Modifier.size(18.dp)) })
                AssistChip(onClick = { onEdit(null) }, label = { Text(stringResource(R.string.rules_new)) }, leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, Modifier.size(18.dp)) })
            }
        }
        when {
            !s.loaded -> Unit
            s.rules.isEmpty() -> EmptyRules(onCreate = { onEdit(null) }, onSuggest = onSuggest)
            else -> {
                Text(stringResource(R.string.rules_list_intro), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                s.rules.forEachIndexed { i, rule ->
                    RuleRow(rule, canUp = i > 0, canDown = i < s.rules.lastIndex, onOpen = { onEdit(rule.id) }, onToggle = { viewModel.toggle(rule) }, onMove = { viewModel.move(rule, it) })
                }
                s.conflicts.forEach { (a, b) -> ConflictCard(a, b) }
            }
        }
    }
}

@Composable
private fun EmptyRules(onCreate: () -> Unit, onSuggest: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Text(stringResource(R.string.rules_empty_title), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.rules_empty_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(stringResource(R.string.rules_example_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.rules_example_when), color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.rules_example_then), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.padding(Space.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.rules_runs_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = onCreate, Modifier.fillMaxWidth().padding(top = Space.sm)) { Text(stringResource(R.string.rules_create_first)) }
        Text(stringResource(R.string.rules_suggest_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onSuggest, Modifier.fillMaxWidth()) { Text(stringResource(R.string.rules_suggest_empty_cta)) }
    }
}

@Composable
private fun RuleRow(rule: Rule, canUp: Boolean, canDown: Boolean, onOpen: () -> Unit, onToggle: () -> Unit, onMove: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(start = Space.xs, end = Space.md, top = Space.sm, bottom = Space.sm), verticalAlignment = Alignment.CenterVertically) {
            Column {
                IconButton(onClick = { onMove(-1) }, enabled = canUp, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.ArrowUpward, contentDescription = stringResource(R.string.rules_move_up), Modifier.size(18.dp)) }
                IconButton(onClick = { onMove(1) }, enabled = canDown, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.ArrowDownward, contentDescription = stringResource(R.string.rules_move_down), Modifier.size(18.dp)) }
            }
            Column(Modifier.weight(1f).padding(horizontal = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(rule.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(ruleSentence(rule), style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DetailDots(RuleEngine.detail(rule.conditions))
            }
            Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun ConflictCard(a: Rule, b: Rule) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(stringResource(R.string.rules_conflict, a.name, b.name), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
