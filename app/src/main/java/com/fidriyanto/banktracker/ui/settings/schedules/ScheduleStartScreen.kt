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
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.settings.rules.StepHeading
import com.fidriyanto.banktracker.ui.theme.Space

private data class Idea(val template: ScheduleTemplate, val icon: ImageVector, val title: Int, val hint: Int)

private val ideas = listOf(
    Idea(ScheduleTemplate.BILLS, Icons.Outlined.Home, R.string.schedule_idea_bills, R.string.schedule_idea_bills_hint),
    Idea(ScheduleTemplate.SUBSCRIPTION, Icons.Outlined.Subscriptions, R.string.schedule_idea_sub, R.string.schedule_idea_sub_hint),
    Idea(ScheduleTemplate.INSTALLMENT, Icons.Outlined.CreditCard, R.string.schedule_idea_installment, R.string.schedule_idea_installment_hint),
    Idea(ScheduleTemplate.INCOME, Icons.Outlined.Payments, R.string.schedule_idea_income, R.string.schedule_idea_income_hint),
    Idea(ScheduleTemplate.TRANSFER, Icons.Outlined.SwapHoriz, R.string.schedule_idea_transfer, R.string.schedule_idea_transfer_hint),
)

// ac: scheduled-transactions - the first page of a new schedule: pick the closest idea
@Composable
fun ScheduleStartScreen(onBack: () -> Unit, onPick: (ScheduleTemplate) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
        StepHeading(stringResource(R.string.schedule_start_title), stringResource(R.string.schedule_start_body))
        ideas.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                pair.forEach { IdeaTile(it, Modifier.weight(1f)) { onPick(it.template) } }
            }
        }
        IdeaTile(Idea(ScheduleTemplate.OTHER, Icons.Outlined.MoreHoriz, R.string.schedule_idea_other, R.string.schedule_idea_other_hint), Modifier.fillMaxWidth()) { onPick(ScheduleTemplate.OTHER) }
    }
}

@Composable
private fun IdeaTile(idea: Idea, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick, modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm), horizontalAlignment = Alignment.Start) {
            IconBadge(idea.icon)
            Text(stringResource(idea.title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(idea.hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
