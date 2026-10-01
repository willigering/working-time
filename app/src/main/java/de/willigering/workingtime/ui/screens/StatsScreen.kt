package de.willigering.workingtime.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import de.willigering.workingtime.data.WorkSession
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.willigering.workingtime.R
import de.willigering.workingtime.core.AppInfo
import de.willigering.workingtime.ui.components.GlassCard
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel

private data class ProjectStats(
    val name: String,
    val colorArgb: Long,
    val minutes: Long,
    val earnings: Double,
)

@Composable
fun StatsScreen(viewModel: TimeTrackerViewModel, visible: Boolean = true) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tick = if (visible) viewModel.tick.collectAsStateWithLifecycle().value else System.currentTimeMillis()
    val minute = tick / 60_000
    val activeMinute = state.activeSession?.let { (tick - it.start).coerceAtLeast(0) / 60_000 }
    val sessions = remember(state.sessions, state.activeSession, state.projects, minute, activeMinute) {
        val active = state.activeSession
        val project = active?.let { viewModel.projectById(it.projectId) }
        if (active == null) state.sessions else state.sessions + WorkSession(
            id = "running", projectId = active.projectId, projectName = project?.name.orEmpty(),
            hourlyRate = project?.hourlyRate ?: 0.0, billable = project?.billable ?: false,
            colorArgb = project?.colorArgb ?: 0xFFFFB300,
            start = active.start, end = tick, notes = active.notes,
        )
    }

    val (todayStart, todayEnd) = viewModel.todayRange()
    val (weekStart, weekEnd) = viewModel.weekRange()
    val (monthStart, monthEnd) = viewModel.monthRange()

    val todayMins = viewModel.totalMinutes(sessions, todayStart, todayEnd)
    val weekMins = viewModel.totalMinutes(sessions, weekStart, weekEnd)
    val monthMins = viewModel.totalMinutes(sessions, monthStart, monthEnd)
    val todayEarn = viewModel.totalEarnings(sessions, todayStart, todayEnd)
    val weekEarn = viewModel.totalEarnings(sessions, weekStart, weekEnd)
    val monthEarn = viewModel.totalEarnings(sessions, monthStart, monthEnd)

    val projectStats = remember(sessions, state.projects, monthStart, monthEnd / 60_000) {
        sessions
            .groupBy { it.projectId }
            .mapNotNull { (_, sessions) ->
                val minutes = viewModel.totalMinutes(sessions, monthStart, monthEnd)
                if (minutes <= 0) return@mapNotNull null
                val sample = sessions.first()
                ProjectStats(
                    name = viewModel.sessionProjectName(sample),
                    colorArgb = viewModel.sessionColor(sample),
                    minutes = minutes,
                    earnings = viewModel.totalEarnings(sessions, monthStart, monthEnd),
                )
            }
            .sortedByDescending { it.earnings }
    }

    val maxMins = projectStats.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.stats_title),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary,
            )
            Text(
                stringResource(R.string.stats_subtitle),
                color = AppColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
        }

        item {
            StatPeriodCard(stringResource(R.string.stats_today), todayMins, todayEarn, AppColors.Accent)
            Spacer(Modifier.height(10.dp))
            StatPeriodCard(stringResource(R.string.stats_this_week), weekMins, weekEarn, AppColors.AccentOrange)
            Spacer(Modifier.height(10.dp))
            StatPeriodCard(stringResource(R.string.stats_this_month), monthMins, monthEarn, AppColors.AccentBlue)
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.stats_by_project),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
        }

        if (projectStats.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.stats_no_data))
                }
            }
        } else {
            items(projectStats) { stats ->
                ProjectStatRow(stats, maxMins)
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), accentColor = AppColors.Accent) {
                Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                InfoRow(stringResource(R.string.label_app), stringResource(R.string.app_name))
                InfoRow(
                    stringResource(R.string.label_version),
                    stringResource(R.string.version_label, AppInfo.VERSION, AppInfo.BUILD_NUMBER),
                )
                InfoRow(stringResource(R.string.label_developer), AppInfo.DEVELOPER)
            }
            Spacer(Modifier.height(20.dp))
            SupportProjectSection()
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            value,
            color = AppColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatPeriodCard(label: String, minutes: Long, earnings: Double, accent: Color) {
    val context = LocalContext.current
    GlassCard(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.hours), style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
                Text(
                    Formatters.duration(context, minutes),
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(stringResource(R.string.earnings), style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
                Text(
                    Formatters.currency(context, earnings),
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.Success,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun MonthProjectBreakdown(viewModel: TimeTrackerViewModel, anchor: Long) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val (monthStart, monthEnd) = de.willigering.workingtime.util.Periods.monthBounds(anchor)
    val projectStats = remember(state.sessions, state.projects, monthStart, monthEnd) {
        state.sessions
            .groupBy { it.projectId }
            .mapNotNull { (_, sessions) ->
                val minutes = viewModel.totalMinutes(sessions, monthStart, monthEnd)
                if (minutes <= 0) return@mapNotNull null
                val sample = sessions.first()
                ProjectStats(
                    name = viewModel.sessionProjectName(sample),
                    colorArgb = viewModel.sessionColor(sample),
                    minutes = minutes,
                    earnings = viewModel.totalEarnings(sessions, monthStart, monthEnd),
                )
            }
            .sortedByDescending { it.minutes }
    }
    val maxMins = projectStats.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1L
    Text(
        stringResource(R.string.stats_by_project),
        style = MaterialTheme.typography.titleMedium,
        color = AppColors.TextPrimary,
    )
    Spacer(Modifier.height(10.dp))
    if (projectStats.isEmpty()) {
        Text(stringResource(R.string.stats_no_data), color = AppColors.TextMuted)
    } else {
        projectStats.forEach { stats ->
            ProjectStatRow(stats, maxMins)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProjectStatRow(stats: ProjectStats, maxMins: Long) {
    val context = LocalContext.current
    val color = Color(stats.colorArgb)
    val progress = stats.minutes.toFloat() / maxMins.toFloat()

    GlassCard(modifier = Modifier.fillMaxWidth(), accentColor = color) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(10.dp)) { drawCircle(color = color) }
            Text(
                stats.name,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                Formatters.duration(context, stats.minutes),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
            )
            Text(
                Formatters.currency(context, stats.earnings),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Success,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = AppColors.GlassBorder,
            strokeCap = StrokeCap.Round,
        )
    }
}
