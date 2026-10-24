package com.fidriyanto.banktracker.ui.settings.schedules

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
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Schedule
import com.fidriyanto.banktracker.domain.model.UpcomingPayment
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.theme.Space
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ac: scheduled-transactions - the home of the feature: what is due soon, then every schedule with its auto-post switch
@Composable
fun SchedulesScreen(onBack: () -> Unit, onNew: () -> Unit, onEdit: (String) -> Unit, viewModel: SchedulesViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    var warnFor by remember { mutableStateOf<String?>(null) }
    val currencyOf = s.accounts.associate { it.id to it.currency }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = Space.sm, end = Space.lg, top = Space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.schedules_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.lg, vertical = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            if (s.schedules.isEmpty()) {
                EmptySchedules()
            } else {
                DueCard(s.dueByCurrency, s.upcoming)
                Text(stringResource(R.string.schedules_coming_up), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (s.upcoming.isEmpty()) Text(stringResource(R.string.schedules_nothing_due), color = MaterialTheme.colorScheme.onSurfaceVariant)
                s.upcoming.take(8).forEach { UpcomingRow(it, currencyOf[it.schedule.walletId].orEmpty()) }
                Text(stringResource(R.string.schedules_all), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                s.schedules.forEach { sc ->
                    ScheduleRow(sc, onClick = { onEdit(sc.id) }, onToggle = { on -> if (on) warnFor = sc.id else viewModel.setAutoPost(sc.id, false) })
                }
            }
        }
        Button(onClick = onNew, Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md)) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(stringResource(R.string.schedules_new), Modifier.padding(start = Space.sm))
        }
    }
    warnFor?.let { id -> AutoPostWarning(onConfirm = { viewModel.setAutoPost(id, true); warnFor = null }, onDismiss = { warnFor = null }) }
}

@Composable
private fun EmptySchedules() {
    Column(Modifier.fillMaxWidth().padding(vertical = Space.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.md)) {
        IconBadge(Icons.Outlined.Event, size = 64)
        Text(stringResource(R.string.schedules_empty_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.schedules_empty_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DueCard(due: Map<String, Double>, upcoming: List<UpcomingPayment>) {
    val next = upcoming.firstOrNull()
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(stringResource(R.string.schedules_due_label, UPCOMING_DAYS), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            if (due.isEmpty()) {
                Text(stringResource(R.string.schedules_due_none), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            } else {
                due.forEach { (cur, total) -> Text(formatMoney(cur, total), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            if (next != null) {
                val days = ChronoUnit.DAYS.between(LocalDate.now(), next.date).toInt()
                Text(
                    pluralStringResource(R.plurals.schedules_next_in, days, upcoming.size, days),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun UpcomingRow(p: UpcomingPayment, currency: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        Column(Modifier.weight(1f)) {
            Text(p.schedule.name, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(p.date.label(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatMoney(currency, p.schedule.amount), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ScheduleRow(s: Schedule, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            IconBadge(Icons.Outlined.Event)
            Column(Modifier.weight(1f)) {
                Text(s.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(repeatSummary(s.repeat, s.every), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(if (s.autoPost) R.string.schedules_auto_on else R.string.schedules_auto_off), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = s.autoPost, onCheckedChange = onToggle)
        }
    }
}
