package de.willigering.workingtime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import de.willigering.workingtime.ui.components.AddIconButton
import de.willigering.workingtime.ui.components.ProjectMark
import de.willigering.workingtime.ui.components.ScreenHeading
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters
import de.willigering.workingtime.util.TimeMath
import de.willigering.workingtime.util.includingRunning
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel

@Composable
fun ProjectsScreen(
    viewModel: TimeTrackerViewModel,
    onOpenProject: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tick by viewModel.tick.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }
    val active = state.projects.filter { !it.archived }
    val archived = state.projects.filter { it.archived }
    val totals = remember(state.sessions, state.activeSession, state.projects, tick / 60_000) {
        state.includingRunning(tick).groupBy { it.projectId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScreenHeading(stringResource(R.string.projects_title), modifier = Modifier.weight(1f))
            AddIconButton(onClick = { showCreate = true }, description = stringResource(R.string.add_project))
        }
        Spacer(Modifier.height(14.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(active, key = { it.id }) { project ->
                val mine = totals[project.id].orEmpty()
                ProjectCard(
                    project = project,
                    minutes = mine.sumOf { TimeMath.minutes(it.start, it.end) },
                    earnings = mine.sumOf { viewModel.sessionEarnings(it) },
                    onClick = { onOpenProject(project.id) },
                )
                Spacer(Modifier.height(10.dp))
            }
            if (active.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.projects_empty_hint),
                        color = AppColors.TextMuted,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
            if (archived.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.project_archive_section, archived.size),
                        color = AppColors.Accent,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clickable { showArchive = !showArchive },
                    )
                }
                if (showArchive) {
                    items(archived, key = { "arch-${it.id}" }) { project ->
                        val mine = totals[project.id].orEmpty()
                        ProjectCard(
                            project = project,
                            minutes = mine.sumOf { TimeMath.minutes(it.start, it.end) },
                            earnings = mine.sumOf { viewModel.sessionEarnings(it) },
                            onClick = { onOpenProject(project.id) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
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
private fun ProjectCard(
    project: Project,
    minutes: Long,
    earnings: Double,
    onClick: () -> Unit,
) {
    val color = Color(project.colorArgb)
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProjectMark(project.name, color)
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
                    project.clientName,
                    color = AppColors.TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Formatters.hoursLabel(minutes), color = AppColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(Formatters.money(earnings), color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
