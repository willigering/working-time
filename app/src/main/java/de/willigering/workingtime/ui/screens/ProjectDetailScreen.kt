package de.willigering.workingtime.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.willigering.workingtime.R
import de.willigering.workingtime.data.Project
import de.willigering.workingtime.data.TimeTrackerRepository
import de.willigering.workingtime.ui.components.DeleteProjectDialog
import de.willigering.workingtime.ui.components.DetailRow
import de.willigering.workingtime.ui.components.MetricLine
import de.willigering.workingtime.ui.components.ProjectMark
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters
import de.willigering.workingtime.util.TimeMath
import de.willigering.workingtime.util.includingRunning
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel

@Composable
fun ProjectDetailScreen(
    projectId: String,
    viewModel: TimeTrackerViewModel,
    onBack: () -> Unit,
    onOpenTimes: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tick by viewModel.tick.collectAsStateWithLifecycle()
    val project = state.projects.find { it.id == projectId }
    var editing by remember { mutableStateOf(false) }
    var pickingColor by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    if (project == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(AppColors.Background))
        return
    }

    val mine = remember(state.sessions, state.activeSession, state.projects, project.id, tick / 60_000) {
        state.includingRunning(tick).filter { it.projectId == project.id }
    }
    val minutes = mine.sumOf { TimeMath.minutes(it.start, it.end) }
    val earnings = mine.sumOf { viewModel.sessionEarnings(it) }
    val color = Color(project.colorArgb)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppColors.TextPrimary)
            }
        }
        ProjectMark(project.name, color, size = 72.dp, corner = 20.dp, fontSize = 28.sp)
        Spacer(Modifier.height(14.dp))
        Text(project.name, color = AppColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (project.clientName.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(project.clientName, color = AppColors.TextMuted, fontSize = 15.sp)
        }
        Spacer(Modifier.height(20.dp))
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AppColors.Surface)
                .border(1.dp, AppColors.GlassBorder, shape),
        ) {
            MetricLine(stringResource(R.string.label_hourly), Formatters.money(project.hourlyRate))
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.GlassBorder))
            MetricLine(stringResource(R.string.project_total_time), Formatters.hoursLabel(minutes))
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.GlassBorder))
            MetricLine(stringResource(R.string.home_revenue), Formatters.money(earnings), AppColors.Accent)
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DetailRow(Icons.Rounded.Schedule, stringResource(R.string.tab_times), stringResource(R.string.project_open_times_sub)) {
                onOpenTimes(project.id)
            }
            DetailRow(Icons.Rounded.Edit, stringResource(R.string.edit), stringResource(R.string.project_edit_sub)) {
                editing = true
            }
            DetailRow(Icons.Rounded.Image, stringResource(R.string.project_logo), stringResource(R.string.project_logo_sub)) {
                pickingColor = true
            }
            if (project.archived) {
                DetailRow(Icons.Rounded.Unarchive, stringResource(R.string.project_restore), stringResource(R.string.project_restore_sub)) {
                    viewModel.updateProject(project.copy(archived = false))
                }
            } else {
                DetailRow(Icons.Rounded.Archive, stringResource(R.string.project_archive), stringResource(R.string.project_archive_sub)) {
                    viewModel.updateProject(project.copy(archived = true))
                    onBack()
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier
                .clickable { confirmDelete = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Delete, contentDescription = null, tint = AppColors.Error)
            Text(
                stringResource(R.string.project_delete_action),
                color = AppColors.Error,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (editing) {
        ProjectDialog(
            project = project,
            knownClients = viewModel.uniqueClientNames(),
            onDismiss = { editing = false },
            onSave = { name, client, rate, colorArgb, billable ->
                viewModel.updateProject(
                    project.copy(
                        name = name,
                        clientName = client,
                        hourlyRate = rate,
                        colorArgb = colorArgb,
                        billable = billable,
                    ),
                )
                editing = false
            },
        )
    }

    if (pickingColor) {
        ColorMarkDialog(
            current = project.colorArgb,
            onDismiss = { pickingColor = false },
            onPick = { colorArgb ->
                viewModel.updateProject(project.copy(colorArgb = colorArgb))
                pickingColor = false
            },
        )
    }

    if (confirmDelete) {
        DeleteProjectDialog(
            projectName = project.name,
            onConfirm = { deleteSessions ->
                viewModel.deleteProject(project.id, deleteSessions)
                confirmDelete = false
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ColorMarkDialog(
    current: Long,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    var selected by remember { mutableLongStateOf(current) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        title = { Text(stringResource(R.string.logo_mark_title), color = AppColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TimeTrackerRepository.PROJECT_COLORS.chunked(4).forEach { rowColors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowColors.forEach { color ->
                            val on = selected == color
                            Box(
                                modifier = Modifier
                                    .size(if (on) 28.dp else 24.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(if (on) 2.dp else 0.dp, AppColors.TextPrimary, CircleShape)
                                    .clickable { selected = color },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onPick(selected) }) {
                Text(stringResource(R.string.save), color = AppColors.Accent)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = AppColors.TextMuted)
            }
        },
    )
}
