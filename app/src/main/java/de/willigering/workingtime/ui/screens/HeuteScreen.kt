package de.willigering.workingtime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.willigering.workingtime.R
import de.willigering.workingtime.data.Project
import de.willigering.workingtime.data.WorkSession
import de.willigering.workingtime.ui.components.BrandMark
import de.willigering.workingtime.ui.components.ConfirmDeleteDialog
import de.willigering.workingtime.ui.components.GoldButton
import de.willigering.workingtime.ui.components.GoldOutlineButton
import de.willigering.workingtime.ui.components.GoldTimerRing
import de.willigering.workingtime.ui.components.ProjectMark
import de.willigering.workingtime.ui.components.ScreenHeading
import de.willigering.workingtime.ui.components.StatCard
import de.willigering.workingtime.ui.components.TimeEntryRow
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters
import de.willigering.workingtime.util.Periods
import de.willigering.workingtime.util.TimeMath
import de.willigering.workingtime.util.includingRunning
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel

@Composable
fun HeuteScreen(
    viewModel: TimeTrackerViewModel,
    visible: Boolean,
    onOpenSettings: () -> Unit,
    onOpenAllTimes: () -> Unit,
    onEditSession: (WorkSession) -> Unit,
    onNewEntry: () -> Unit,
    onOpenClock: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tick = if (visible) viewModel.tick.collectAsStateWithLifecycle().value else System.currentTimeMillis()
    var showCreate by remember { mutableStateOf(false) }
    var playMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WorkSession?>(null) }

    val active = state.activeSession
    val projects = state.projects.filter { !it.archived }
    val activeProject = active?.let { viewModel.projectById(it.projectId) }
        ?: state.selectedProjectId?.let { viewModel.projectById(it) }?.takeUnless { it.archived }
    val elapsed = if (active != null) (tick - active.start).coerceAtLeast(0L) else 0L

    val live = remember(state.sessions, state.activeSession, state.projects, tick / 60_000) {
        state.includingRunning(tick)
    }
    val (todayStart, todayEnd) = Periods.dayBounds(tick)
    val (weekStart, weekEnd) = Periods.weekBounds(tick)
    val todayMins = viewModel.totalMinutes(live, todayStart, todayEnd)
    val weekMins = viewModel.totalMinutes(live, weekStart, weekEnd)
    val todayEarn = viewModel.totalEarnings(live, todayStart, todayEnd)
    val weekEarn = viewModel.totalEarnings(live, weekStart, weekEnd)
    val recent = remember(state.sessions) { state.sessions.sortedByDescending { it.start }.take(3) }

    fun begin(project: Project) {
        viewModel.selectProject(project.id)
        viewModel.startSession()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        if (active != null) {
            RunningHeute(
                elapsed = elapsed,
                project = activeProject,
                todayMins = todayMins,
                todayEarn = todayEarn,
                onOpenSettings = onOpenSettings,
                onOpenClock = onOpenClock,
                onStop = { viewModel.stopSession() },
            )
        } else {
            IdleHeute(
                todayMins = todayMins,
                todayEarn = todayEarn,
                weekMins = weekMins,
                weekEarn = weekEarn,
                recent = recent,
                projects = projects,
                playMenu = playMenu,
                onPlayMenu = { playMenu = it },
                onPlay = {
                    when {
                        projects.isEmpty() -> showCreate = true
                        projects.size == 1 -> begin(projects.first())
                        else -> playMenu = true
                    }
                },
                onPick = { begin(it); playMenu = false },
                onCreate = { playMenu = false; showCreate = true },
                viewModel = viewModel,
                onEditSession = onEditSession,
                onDeleteSession = { pendingDelete = it },
                onOpenAllTimes = onOpenAllTimes,
                onNewEntry = onNewEntry,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    pendingDelete?.let { session ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_session_title),
            message = stringResource(R.string.delete_session_message),
            detail = viewModel.sessionProjectName(session),
            onConfirm = {
                viewModel.deleteSession(session.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (showCreate) {
        ProjectDialog(
            project = null,
            knownClients = viewModel.uniqueClientNames(),
            onDismiss = { showCreate = false },
            onSave = { name, client, rate, color, billable ->
                viewModel.addProject(name, client, rate, color, billable)
                showCreate = false
            },
        )
    }
}

@Composable
private fun RunningHeute(
    elapsed: Long,
    project: Project?,
    todayMins: Long,
    todayEarn: Double,
    onOpenSettings: () -> Unit,
    onOpenClock: () -> Unit,
    onStop: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BrandMark()
        Text(
            stringResource(R.string.brand_mark),
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 1.6.sp,
        )
        Icon(
            Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.tab_settings),
            tint = AppColors.TextSecondary,
            modifier = Modifier
                .size(22.dp)
                .clickable(onClick = onOpenSettings),
        )
    }
    Spacer(Modifier.height(18.dp))
    Text(Formatters.dateLong(System.currentTimeMillis()), color = AppColors.TextMuted, fontSize = 14.sp)
    Spacer(Modifier.height(6.dp))
    ScreenHeading(stringResource(R.string.tab_today))
    Spacer(Modifier.height(16.dp))
    ProjectChip(project = project)
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GoldTimerRing(
            elapsedMs = elapsed,
            caption = stringResource(R.string.home_running),
            onClick = onOpenClock,
        )
    }
    Spacer(Modifier.height(8.dp))
    GoldButton(
        text = stringResource(R.string.timer_stop),
        icon = Icons.Rounded.Stop,
        onClick = onStop,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            value = Formatters.hoursLabel(todayMins),
            label = stringResource(R.string.home_work_time),
            icon = Icons.Rounded.Schedule,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = Formatters.money(todayEarn),
            label = stringResource(R.string.home_revenue_today),
            icon = Icons.Rounded.Payments,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun IdleHeute(
    todayMins: Long,
    todayEarn: Double,
    weekMins: Long,
    weekEarn: Double,
    recent: List<WorkSession>,
    projects: List<Project>,
    playMenu: Boolean,
    onPlayMenu: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onPick: (Project) -> Unit,
    onCreate: () -> Unit,
    viewModel: TimeTrackerViewModel,
    onEditSession: (WorkSession) -> Unit,
    onDeleteSession: (WorkSession) -> Unit,
    onOpenAllTimes: () -> Unit,
    onNewEntry: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ScreenHeading(stringResource(R.string.tab_today), modifier = Modifier.weight(1f))
        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.Accent.copy(alpha = 0.16f))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.timer_start),
                    tint = AppColors.Accent,
                )
            }
            DropdownMenu(
                expanded = playMenu,
                onDismissRequest = { onPlayMenu(false) },
            ) {
                projects.forEach { project ->
                    DropdownMenuItem(
                        text = {
                            Text(project.name, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        onClick = { onPick(project) },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.create_new_project), color = AppColors.Accent) },
                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, tint = AppColors.Accent) },
                    onClick = onCreate,
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(Formatters.dateLong(System.currentTimeMillis()), color = AppColors.TextMuted, fontSize = 14.sp)
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            value = Formatters.hoursLabel(todayMins),
            label = stringResource(R.string.home_work_time),
            icon = Icons.Rounded.Schedule,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = Formatters.money(todayEarn),
            label = stringResource(R.string.home_revenue),
            icon = Icons.Rounded.Payments,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            value = Formatters.hoursLabel(weekMins),
            label = stringResource(R.string.home_this_week),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = Formatters.money(weekEarn),
            label = stringResource(R.string.home_revenue_week),
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(22.dp))
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.home_recent),
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(R.string.home_show_all),
            color = AppColors.Accent,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onOpenAllTimes),
        )
    }
    Spacer(Modifier.height(8.dp))
    if (recent.isEmpty()) {
        Text(stringResource(R.string.sessions_empty_title), color = AppColors.TextMuted, fontSize = 14.sp)
    } else {
        recent.forEach { session ->
            val mins = TimeMath.minutes(session.start, session.end)
            TimeEntryRow(
                name = viewModel.sessionProjectName(session),
                subtitle = "${Formatters.time(session.start)} – ${Formatters.time(session.end)}",
                duration = Formatters.hoursLabel(mins),
                color = Color(viewModel.sessionColor(session)),
                onClick = { onEditSession(session) },
                onEdit = { onEditSession(session) },
                onDelete = { onDeleteSession(session) },
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    GoldOutlineButton(
        text = stringResource(R.string.home_new_entry),
        icon = Icons.Rounded.Add,
        onClick = onNewEntry,
        enabled = projects.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProjectChip(project: Project?) {
    val shape = RoundedCornerShape(16.dp)
    Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AppColors.Surface)
                .border(1.dp, AppColors.GlassBorder, shape)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (project != null) {
                ProjectMark(project.name, Color(project.colorArgb))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        project.name,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (project.clientName.isNotBlank()) {
                        Text(
                            stringResource(R.string.home_client_line, project.clientName),
                            color = AppColors.TextMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            } else {
                Text(
                    stringResource(R.string.no_project_title),
                    color = AppColors.TextSecondary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                )
            }
    }
}
