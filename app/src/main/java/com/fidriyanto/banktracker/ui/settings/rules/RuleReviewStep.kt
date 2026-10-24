package com.fidriyanto.banktracker.ui.settings.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.theme.Space

// The last step: the rule in plain words, a name and a box to try it on a sample transaction.
@Composable
fun RuleReviewStep(state: EditorState, onEdit: ((RuleDraft) -> RuleDraft) -> Unit, onTest: (String?, String?, String?) -> Unit, onPast: () -> Unit) {
    val d = state.draft
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.rules_review_title), "")
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(stringResource(R.string.rules_plain_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (d.whenError() == null && d.thenError() == null) Text(ruleSentence(d.toRule("preview")), color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                OutlinedTextField(d.name, { v -> onEdit { it.copy(name = v) } }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.rules_name_label)) }, singleLine = true, isError = state.showErrors && d.name.isBlank())
            }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text(stringResource(R.string.rules_try_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                OutlinedTextField(state.testText, { onTest(it, null, null) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.rules_try_text)) }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    OutlinedTextField(state.testAmount, { onTest(null, it, null) }, Modifier.weight(1f), label = { Text(stringResource(R.string.rules_try_amount)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(state.testTime, { onTest(null, null, it) }, Modifier.weight(1f), label = { Text(stringResource(R.string.rules_try_time)) }, placeholder = { Text("13:00") }, singleLine = true)
                }
                state.test?.let { t ->
                    if (t.matched) {
                        val result = if (t.outcome.ignore) stringResource(R.string.rules_sum_ignore) else actionSentence(d.toRule("test").action)
                        Text(stringResource(R.string.rules_try_match, result), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = Space.xs))
                    } else {
                        Text(stringResource(R.string.rules_try_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        OptionCard(Icons.Outlined.History, stringResource(R.string.rules_past_title), stringResource(R.string.rules_past_hint), stringResource(R.string.rules_past_review), onPast)
    }
}
