package com.fidriyanto.banktracker.ui.settings.schedules

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Where the user is inside Scheduled: the list, the idea picker of a new schedule, or the editor.
private sealed interface SchedulesPage {
    data object List : SchedulesPage
    data object Start : SchedulesPage
    data class Editor(val scheduleId: String?, val template: ScheduleTemplate?) : SchedulesPage
}

@Composable
fun SchedulesFlow(onBack: () -> Unit) {
    var page by remember { mutableStateOf<SchedulesPage>(SchedulesPage.List) }
    // Each editor that opens gets its own view model, so it never starts with the state of the last visit.
    var opens by remember { mutableIntStateOf(0) }
    val go = { next: SchedulesPage -> opens++; page = next }
    when (val p = page) {
        SchedulesPage.List -> SchedulesScreen(onBack = onBack, onNew = { go(SchedulesPage.Start) }, onEdit = { go(SchedulesPage.Editor(it, null)) })
        SchedulesPage.Start -> ScheduleStartScreen(onBack = { go(SchedulesPage.List) }, onPick = { go(SchedulesPage.Editor(null, it)) })
        is SchedulesPage.Editor -> ScheduleEditorScreen(p.scheduleId, p.template, onClose = { go(SchedulesPage.List) }, vmKey = "schedule-$opens")
    }
}
