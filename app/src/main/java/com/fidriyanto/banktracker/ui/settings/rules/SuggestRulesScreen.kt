package com.fidriyanto.banktracker.ui.settings.rules

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.usecase.RuleSuggestion
import com.fidriyanto.banktracker.domain.usecase.SuggestionKind
import com.fidriyanto.banktracker.ui.theme.Space

// Rules found in the saved transactions. The user picks a category and adds the rule, edits it first, or skips it.
@Composable
fun SuggestRulesScreen(onBack: () -> Unit, onEdit: (Rule) -> Unit, onApply: (Rule) -> Unit, vmKey: String? = null, viewModel: SuggestRulesViewModel = hiltViewModel(key = vmKey)) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.rules_suggest_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        StepHeading(stringResource(R.string.rules_suggest_heading), stringResource(R.string.rules_suggest_body))
        when {
            s.loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                CircularProgressIndicator(Modifier.padding(Space.xs), strokeWidth = 2.dp)
                Text(stringResource(R.string.rules_suggest_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            s.failed -> {
                ErrorText(stringResource(R.string.rules_suggest_error))
                OutlinedButton(onClick = viewModel::load) { Text(stringResource(R.string.rules_past_retry)) }
            }
            s.items.none { it.status != SuggestStatus.SKIPPED } -> Text(stringResource(R.string.rules_suggest_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> s.items.filter { it.status != SuggestStatus.SKIPPED }.forEach { item ->
                SuggestionCard(item, categories.map { it.name }, viewModel::choose, viewModel::add, viewModel::skip, onEdit, onApply)
            }
        }
    }
}

@Composable
private fun suggestionName(s: RuleSuggestion): String {
    val amount = s.conditions.amount?.min?.let(::plain).orEmpty()
    return when (s.kind) {
        SuggestionKind.TEXT -> stringResource(R.string.rules_suggest_name_text, s.conditions.text?.value.orEmpty().replaceFirstChar { it.uppercase() })
        SuggestionKind.AMOUNT -> stringResource(R.string.rules_suggest_name_amount, amount)
        SuggestionKind.RECURRING -> stringResource(R.string.rules_suggest_name_day, amount, s.day ?: 0)
    }
}

@Composable
private fun SuggestionCard(
    item: SuggestionItem, categories: List<String>,
    onChoose: (String, String) -> Unit, onAdd: (String, String) -> Unit, onSkip: (String) -> Unit, onEdit: (Rule) -> Unit, onApply: (Rule) -> Unit,
) {
    val s = item.suggestion
    val name = suggestionName(s)
    val sentence = stringResource(R.string.rules_suggest_when, joinNatural(conditionParts(s.conditions)))
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(sentence, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.rules_suggest_rows, s.rows), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
            Text(
                when (s.kind) {
                    SuggestionKind.TEXT -> stringResource(R.string.rules_suggest_ev_text, s.repeats)
                    SuggestionKind.AMOUNT -> stringResource(R.string.rules_suggest_ev_amount, s.repeats)
                    SuggestionKind.RECURRING -> stringResource(R.string.rules_suggest_ev_recurring, s.day ?: 0, s.months)
                },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (s.hintCategory != null) Text(stringResource(R.string.rules_suggest_ev_hint, s.hintRows, s.hintOf, s.hintCategory), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.status == SuggestStatus.ADDED) {
                Text(stringResource(R.string.rules_suggest_added), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                item.rule?.let { Button(onClick = { onApply(it) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.rules_suggest_apply)) } }
                return@Column
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                // The chosen category comes first, so it is in view without scrolling.
                (listOfNotNull(item.category) + categories.filter { it != item.category }).forEach { c -> FilterChip(selected = item.category == c, onClick = { onChoose(s.key, c) }, label = { Text(c) }) }
            }
            if (item.category == null) Text(stringResource(R.string.rules_suggest_choose), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onAdd(s.key, name) }, enabled = item.category != null) { Text(stringResource(R.string.rules_suggest_add)) }
                OutlinedButton(onClick = {
                    onEdit(Rule("", name, true, s.conditions, com.fidriyanto.banktracker.domain.model.RuleAction(category = item.category)))
                }) { Text(stringResource(R.string.rules_suggest_edit)) }
                TextButton(onClick = { onSkip(s.key) }) { Text(stringResource(R.string.rules_suggest_skip)) }
            }
        }
    }
}
