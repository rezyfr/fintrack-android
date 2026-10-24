package com.fidriyanto.banktracker.ui.settings.schedules

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.ScheduleRepeat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

fun weekdayName(day: Int): String = DayOfWeek.of(day.coerceIn(1, 7)).getDisplayName(TextStyle.FULL, Locale.getDefault())

// "Every month, day 1" and the like, for a list row
@Composable
fun repeatSummary(repeat: ScheduleRepeat, every: Int): String = when (repeat) {
    ScheduleRepeat.MONTHLY -> stringResource(R.string.schedule_summary_monthly, every)
    ScheduleRepeat.WEEKLY -> stringResource(R.string.schedule_summary_weekly, weekdayName(every))
    ScheduleRepeat.EVERY_N_DAYS -> stringResource(R.string.schedule_summary_days, every)
}
