package com.fidriyanto.banktracker.ui.settings.rules

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.usecase.PastSupport
import com.fidriyanto.banktracker.domain.usecase.RuleChange
import com.fidriyanto.banktracker.ui.theme.Space

private const val SHOWN = 30
private val RANGES = listOf(30 to R.string.rules_past_days_30, 90 to R.string.rules_past_days_90, 365 to R.string.rules_past_days_365)

// Shows which saved transactions a rule would change. Nothing changes until the user confirms.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PastRulesScreen(rule: Rule, onClose: () -> Unit, vmKey: String? = null, viewModel: PastRulesViewModel = hiltViewModel(key = vmKey)) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(rule) { viewModel.start(rule) }
    BackHandler(onBack = onClose)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = Space.sm, end = Space.lg, top = Space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.rules_past_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.lg, vertical = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            StepHeading(stringResource(R.string.rules_past_heading), stringResource(R.string.rules_past_body))
            when (s.support) {
                PastSupport.HAS_TIME -> NoteCard(stringResource(R.string.rules_past_time))
                PastSupport.IGNORE -> NoteCard(stringResource(R.string.rules_past_ignore))
                PastSupport.OK -> {
                    if (s.phase == PastPhase.READY || s.phase == PastPhase.LOADING || s.phase == PastPhase.ERROR) {
                        Text(stringResource(R.string.rules_past_range), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                            RANGES.forEach { (days, label) -> FilterChip(selected = s.days == days, onClick = { viewModel.load(days) }, label = { Text(stringResource(label)) }) }
                        }
                    }
                    PastBody(s, viewModel::load)
                }
            }
        }
        PastAction(s, onApply = { confirm = true }, onUndo = viewModel::undo, onClose = onClose)
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.rules_past_confirm_title)) },
            text = { Text(stringResource(R.string.rules_past_confirm_body, s.changes.size)) },
            confirmButton = { TextButton(onClick = { confirm = false; viewModel.apply() }) { Text(stringResource(R.string.rules_past_apply, s.changes.size)) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun PastBody(s: PastState, onRetry: (Int) -> Unit) {
    when (s.phase) {
        PastPhase.LOADING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            CircularProgressIndicator(Modifier.padding(Space.xs), strokeWidth = 2.dp)
            Text(stringResource(R.string.rules_past_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PastPhase.ERROR -> {
            ErrorText(stringResource(R.string.rules_past_error))
            OutlinedButton(onClick = { onRetry(s.days) }) { Text(stringResource(R.string.rules_past_retry)) }
        }
        PastPhase.READY -> if (s.changes.isEmpty()) NoteCard(stringResource(R.string.rules_past_none)) else {
            Text(stringResource(R.string.rules_past_count, s.changes.size), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            s.changes.take(SHOWN).forEach { ChangeCard(it) }
            if (s.changes.size > SHOWN) Text(stringResource(R.string.rules_past_more, s.changes.size - SHOWN), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PastPhase.APPLYING -> {
            Text(stringResource(R.string.rules_past_progress, s.progress, s.changes.size), color = MaterialTheme.colorScheme.onBackground)
            LinearProgressIndicator(progress = { if (s.changes.isEmpty()) 0f else s.progress.toFloat() / s.changes.size }, Modifier.fillMaxWidth())
        }
        PastPhase.DONE -> s.result?.let { r ->
            NoteCard(stringResource(R.string.rules_past_done, r.done))
            if (r.failed > 0) ErrorText(stringResource(R.string.rules_past_failed, r.failed))
        }
        PastPhase.UNDONE -> NoteCard(stringResource(R.string.rules_past_undone))
    }
}

@Composable
private fun PastAction(s: PastState, onApply: () -> Unit, onUndo: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        when {
            s.phase == PastPhase.READY && s.support == PastSupport.OK && s.changes.isNotEmpty() ->
                Button(onClick = onApply, Modifier.fillMaxWidth()) { Text(stringResource(R.string.rules_past_apply, s.changes.size)) }
            s.phase == PastPhase.DONE -> {
                if ((s.result?.done ?: 0) > 0) OutlinedButton(onClick = onUndo, Modifier.fillMaxWidth()) { Text(stringResource(R.string.rules_past_undo)) }
                Button(onClick = onClose, Modifier.fillMaxWidth()) { Text(stringResource(R.string.rules_past_close)) }
            }
            s.phase != PastPhase.APPLYING -> Button(onClick = onClose, Modifier.fillMaxWidth()) { Text(stringResource(R.string.rules_past_close)) }
        }
    }
}

@Composable
private fun NoteCard(text: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text(text, Modifier.padding(Space.lg), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ChangeCard(c: RuleChange) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(c.row.item, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.rules_past_meta, c.row.dateIso.take(10), c.row.amount.toString()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.rules_past_change, listOfNotNull(c.row.category, c.row.subcategory).joinToString(" / "), listOfNotNull(c.category, c.subcategory).joinToString(" / ")), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            if (c.item != c.row.item) Text(stringResource(R.string.rules_past_change, c.row.item, c.item), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
