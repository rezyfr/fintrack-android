package com.fidriyanto.banktracker.ui.settings.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.ScheduleRepeat
import com.fidriyanto.banktracker.ui.common.PickDateDialog
import com.fidriyanto.banktracker.ui.settings.rules.ErrorText
import com.fidriyanto.banktracker.ui.settings.rules.OpenCard
import com.fidriyanto.banktracker.ui.settings.rules.OptionCard
import com.fidriyanto.banktracker.ui.settings.rules.StepHeading
import com.fidriyanto.banktracker.ui.theme.Space
import java.time.LocalDate

private val repeats = listOf(
    ScheduleRepeat.MONTHLY to R.string.schedule_repeat_monthly,
    ScheduleRepeat.WEEKLY to R.string.schedule_repeat_weekly,
    ScheduleRepeat.EVERY_N_DAYS to R.string.schedule_repeat_days,
)

// ac: scheduled-transactions - step 2: when, said as a sentence, with a month that shows the dates
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleWhenStep(d: ScheduleDraft, showErrors: Boolean, onEdit: ((ScheduleDraft) -> ScheduleDraft) -> Unit) {
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    val next = d.nextDates(3)
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.schedule_when_title), stringResource(R.string.schedule_when_body))
        Text(sentence(d), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            repeats.forEach { (r, label) ->
                FilterChip(selected = d.repeat == r, onClick = { onEdit { it.copy(repeat = r, every = defaultEvery(r, it.startDate)) } }, label = { Text(stringResource(label)) })
            }
        }
        when (d.repeat) {
            ScheduleRepeat.MONTHLY -> FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                (1..31).forEach { day -> FilterChip(selected = d.every == day, onClick = { onEdit { it.copy(every = day) } }, label = { Text(day.toString()) }) }
            }
            ScheduleRepeat.WEEKLY -> FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                (1..7).forEach { day -> FilterChip(selected = d.every == day, onClick = { onEdit { it.copy(every = day) } }, label = { Text(weekdayName(day)) }) }
            }
            ScheduleRepeat.EVERY_N_DAYS -> OutlinedTextField(
                value = d.every.toString(), onValueChange = { v -> onEdit { it.copy(every = v.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 365) ?: 1) } },
                label = { Text(stringResource(R.string.schedule_every_days)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        if (d.repeat == ScheduleRepeat.MONTHLY && d.every > 28) Text(stringResource(R.string.schedule_short_month), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Text(stringResource(R.string.schedule_starts), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { pickStart = true }) { Text(d.startDate.label()) }
        }
        MiniMonth(month = d.startDate, highlighted = d.nextDates(12).toSet())
        if (next.isNotEmpty()) Text(stringResource(R.string.schedule_next_dates, next.joinToString(", ") { it.label() }), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (d.endOn) {
            OpenCard(Icons.Outlined.EventBusy, stringResource(R.string.schedule_end_title), onRemove = { onEdit { it.copy(endOn = false) } }) {
                OutlinedButton(onClick = { pickEnd = true }) { Text(d.endDate.label()) }
                if (showErrors && d.endError()) ErrorText(stringResource(R.string.schedule_error_end))
            }
        } else {
            OptionCard(Icons.Outlined.EventBusy, stringResource(R.string.schedule_end_title), stringResource(R.string.schedule_end_hint), stringResource(R.string.schedule_end_add)) { onEdit { it.copy(endOn = true) } }
        }
        Text(stringResource(R.string.schedule_catch_up), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.xs))
    }
    if (pickStart) PickDateDialog(d.startDate, onPick = { date -> onEdit { it.copy(startDate = date, every = if (it.repeat == ScheduleRepeat.MONTHLY) date.dayOfMonth else it.every) } }, onDismiss = { pickStart = false })
    if (pickEnd) PickDateDialog(d.endDate, onPick = { date -> onEdit { it.copy(endDate = date) } }, onDismiss = { pickEnd = false })
}

private fun defaultEvery(r: ScheduleRepeat, start: LocalDate): Int = when (r) {
    ScheduleRepeat.MONTHLY -> start.dayOfMonth
    ScheduleRepeat.WEEKLY -> start.dayOfWeek.value
    ScheduleRepeat.EVERY_N_DAYS -> 14
}

@Composable
private fun sentence(d: ScheduleDraft): String = when (d.repeat) {
    ScheduleRepeat.MONTHLY -> stringResource(R.string.schedule_sentence_monthly, d.every, d.startDate.label())
    ScheduleRepeat.WEEKLY -> stringResource(R.string.schedule_sentence_weekly, weekdayName(d.every), d.startDate.label())
    ScheduleRepeat.EVERY_N_DAYS -> stringResource(R.string.schedule_sentence_days, d.every, d.startDate.label())
}
