package de.willigering.workingtime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import de.willigering.workingtime.R
import de.willigering.workingtime.data.AppState
import de.willigering.workingtime.data.Project
import de.willigering.workingtime.ui.components.AnimatedClock
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel

@Composable
fun TimerScreen(
    viewModel: TimeTrackerViewModel,
) {
    val state by viewModel.state.collectAsState()
    val tick by viewModel.tick.collectAsState()
    var notes by remember { mutableStateOf("") }
    var showCreateProject by remember { mutableStateOf(false) }

    val active = state.activeSession
    val elapsed = if (active != null) tick - active.start else 0L
    val activeProject = active?.let { viewModel.projectById(it.projectId) }
        ?: state.selectedProjectId?.let { viewModel.projectById(it) }

    val (todayStart, todayEnd) = viewModel.todayRange()
    val (weekStart, weekEnd) = viewModel.weekRange()
    val (monthStart, monthEnd) = viewModel.monthRange()
    val todayMins = viewModel.totalMinutes(state.sessions, todayStart, todayEnd)
    val weekMins = viewModel.totalMinutes(state.sessions, weekStart, weekEnd)
    val monthMins = viewModel.totalMinutes(state.sessions, monthStart, monthEnd)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.Accent,
            fontWeight = FontWeight.SemiBold,
        )
        if (active != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.timer_started_at, Formatters.time(active.start)),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextMuted,
            )
        }
        Spacer(Modifier.height(10.dp))

        HeroTimer(elapsed = elapsed)

        Spacer(Modifier.height(12.dp))

        ProjectSelector(
            state = state,
            viewModel = viewModel,
            displayProject = activeProject,
            canSwitch = active == null,
            onCreateProject = { showCreateProject = true },
        )
        Spacer(Modifier.height(12.dp))

        if (active == null) {
            Button(
                onClick = { viewModel.startSession() },
                enabled = activeProject != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Anthracite,
                    contentColor = AppColors.TextPrimary,
                    disabledContainerColor = AppColors.Anthracite.copy(alpha = 0.45f),
                    disabledContentColor = AppColors.TextMuted,
                ),
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = if (activeProject != null) AppColors.Accent else AppColors.TextMuted,
                )
                Text(
                    "  ${stringResource(R.string.timer_start)}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.timer_note_label)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Accent,
                    unfocusedBorderColor = AppColors.GlassBorder,
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    focusedContainerColor = AppColors.Surface,
                    unfocusedContainerColor = AppColors.Surface,
                ),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.stopSession(notes)
                    notes = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent,
                    contentColor = AppColors.Background,
                ),
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = null)
                Text(
                    "  ${stringResource(R.string.timer_stop)}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedClock(modifier = Modifier.fillMaxSize())
        }

        MiniStatsRow(
            today = todayMins,
            week = weekMins,
            month = monthMins,
        )
        Spacer(Modifier.height(12.dp))
    }

    if (showCreateProject) {
        ProjectDialog(
            project = null,
            knownClients = viewModel.uniqueClientNames(),
            onDismiss = { showCreateProject = false },
            onSave = { name, client, rate, color, billable ->
                viewModel.addProject(name, client, rate, color, billable)
                showCreateProject = false
            },
        )
    }
}

@Composable
private fun HeroTimer(elapsed: Long) {
    val (hours, minutes, seconds) = Formatters.timerParts(elapsed)
    val digitStyle = MaterialTheme.typography.headlineLarge.copy(
        color = AppColors.TextPrimary,
        fontWeight = FontWeight.Light,
    )
    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        color = AppColors.TextMuted,
        letterSpacing = 1.6.sp,
        fontSize = 10.sp,
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeBlock(hours, stringResource(R.string.timer_hours), digitStyle, labelStyle)
            Text(":", style = digitStyle, color = AppColors.TextPrimary, modifier = Modifier.padding(bottom = 16.dp))
            TimeBlock(minutes, stringResource(R.string.timer_minutes), digitStyle, labelStyle)
            Text(":", style = digitStyle, color = AppColors.TextPrimary, modifier = Modifier.padding(bottom = 16.dp))
            TimeBlock(seconds, stringResource(R.string.timer_seconds), digitStyle, labelStyle)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .width(168.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        )
    }
}

@Composable
private fun TimeBlock(
    value: String,
    label: String,
    digitStyle: androidx.compose.ui.text.TextStyle,
    labelStyle: androidx.compose.ui.text.TextStyle,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(value, style = digitStyle)
        Text(label, style = labelStyle)
    }
}

@Composable
private fun MiniStatsRow(today: Long, week: Long, month: Long) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.GlassBorder, shape)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniStat(
            label = stringResource(R.string.stats_today),
            value = Formatters.compactHours(today),
        )
        Box(
            Modifier
                .width(1.dp)
                .height(28.dp)
                .background(AppColors.GlassBorder),
        )
        MiniStat(
            label = stringResource(R.string.stats_this_week),
            value = Formatters.compactHours(week),
        )
        Box(
            Modifier
                .width(1.dp)
                .height(28.dp)
                .background(AppColors.GlassBorder),
        )
        MiniStat(
            label = stringResource(R.string.stats_this_month),
            value = Formatters.compactHours(month),
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.TextMuted,
            fontSize = 9.sp,
            letterSpacing = 1.1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ProjectSelector(
    state: AppState,
    viewModel: TimeTrackerViewModel,
    displayProject: Project?,
    canSwitch: Boolean,
    onCreateProject: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val canExpand = canSwitch

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.current_project).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.TextMuted,
            letterSpacing = 1.4.sp,
        )
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            if (displayProject != null) {
                ProjectCard(
                    project = displayProject,
                    selected = true,
                    showExpand = canExpand,
                    expanded = expanded,
                    onClick = {
                        if (canExpand) {
                            expanded = !expanded
                        }
                    },
                )
            } else {
                EmptyProjectCard(
                    canExpand = canExpand,
                    hasProjects = state.projects.isNotEmpty(),
                    expanded = expanded,
                    onClick = {
                        if (!canExpand) return@EmptyProjectCard
                        if (state.projects.isEmpty()) {
                            onCreateProject()
                        } else {
                            expanded = !expanded
                        }
                    },
                )
            }
            DropdownMenu(
                expanded = expanded && canExpand,
                onDismissRequest = { expanded = false },
            ) {
                val others = state.projects.filter { it.id != state.selectedProjectId }
                for (project in others) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(project.name, color = AppColors.TextPrimary)
                                if (project.clientName.isNotBlank()) {
                                    Text(
                                        project.clientName,
                                        color = AppColors.TextMuted,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        },
                        onClick = {
                            viewModel.selectProject(project.id)
                            expanded = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.create_new_project),
                            color = AppColors.Accent,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    onClick = {
                        expanded = false
                        onCreateProject()
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyProjectCard(
    canExpand: Boolean,
    hasProjects: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.GlassBorder, shape)
            .clickable(enabled = canExpand, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.no_project_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                )
                Text(
                    stringResource(R.string.no_project_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextMuted,
                    letterSpacing = 0.2.sp,
                )
            }
            if (canExpand && hasProjects) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(R.string.switch_project),
                    tint = AppColors.TextMuted,
                )
            } else if (canExpand) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.create_new_project),
                    tint = AppColors.Accent,
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    selected: Boolean,
    showExpand: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val color = Color(project.colorArgb)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.GlassBorder, shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(AppColors.Accent),
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) AppColors.Accent else color),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp),
            ) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (project.clientName.isNotBlank()) {
                    Text(
                        project.clientName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (project.hourlyRate > 0) {
                Text(
                    stringResource(R.string.hourly_rate_value, project.hourlyRate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                )
            }
            if (showExpand) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(R.string.switch_project),
                    tint = AppColors.TextMuted,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}
