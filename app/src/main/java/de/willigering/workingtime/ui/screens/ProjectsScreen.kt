package de.willigering.workingtime.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import de.willigering.workingtime.data.Client
import de.willigering.workingtime.data.Project
import de.willigering.workingtime.ui.components.ConfirmDeleteDialog
import de.willigering.workingtime.ui.components.DeleteProjectDialog
import de.willigering.workingtime.ui.components.GlassCard
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel

private enum class ProjectsPane { Projects, Clients }

@Composable
fun ProjectsScreen(viewModel: TimeTrackerViewModel) {
    val state by viewModel.state.collectAsState()
    var pane by remember { mutableStateOf(ProjectsPane.Projects) }
    var showProjectDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var projectPendingDelete by remember { mutableStateOf<Project?>(null) }
    var showClientDialog by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<Client?>(null) }
    var clientPendingDelete by remember { mutableStateOf<Client?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pane == ProjectsPane.Projects) {
                        editingProject = null
                        showProjectDialog = true
                    } else {
                        editingClient = null
                        showClientDialog = true
                    }
                },
                containerColor = AppColors.Accent,
                contentColor = AppColors.Background,
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = if (pane == ProjectsPane.Projects) {
                        stringResource(R.string.add_project)
                    } else {
                        stringResource(R.string.add_client)
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.projects_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.projects_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(16.dp))
            SegmentToggle(
                selected = pane,
                onSelect = { pane = it },
            )
            Spacer(Modifier.height(16.dp))

            if (pane == ProjectsPane.Projects) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.projects, key = { it.id }) { project ->
                        ProjectItem(
                            project = project,
                            onEdit = {
                                editingProject = project
                                showProjectDialog = true
                            },
                            onDelete = { projectPendingDelete = project },
                        )
                    }
                    if (state.projects.size <= 1) {
                        item {
                            ProjectsEmptyState(hasOne = state.projects.size == 1)
                        }
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.clients, key = { it.id }) { client ->
                        val count = state.projects.count {
                            it.clientName.equals(client.name, ignoreCase = true)
                        }
                        ClientItem(
                            client = client,
                            projectCount = count,
                            onEdit = {
                                editingClient = client
                                showClientDialog = true
                            },
                            onDelete = { clientPendingDelete = client },
                        )
                    }
                    if (state.clients.isEmpty()) {
                        item { ClientsEmptyState() }
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }

    if (showProjectDialog) {
        ProjectDialog(
            project = editingProject,
            knownClients = viewModel.uniqueClientNames(),
            onDismiss = { showProjectDialog = false },
            onSave = { name, client, rate, color, billable ->
                if (editingProject != null) {
                    viewModel.updateProject(
                        editingProject!!.copy(
                            name = name,
                            clientName = client,
                            hourlyRate = rate,
                            colorArgb = color,
                            billable = billable,
                        ),
                    )
                } else {
                    viewModel.addProject(name, client, rate, color, billable)
                }
                showProjectDialog = false
            },
        )
    }

    if (showClientDialog) {
        ClientDialog(
            clientName = editingClient?.name,
            onDismiss = { showClientDialog = false },
            onSave = { name ->
                val existing = editingClient
                if (existing != null) {
                    viewModel.updateClient(existing.copy(name = name))
                } else {
                    viewModel.addClient(name)
                }
                showClientDialog = false
            },
        )
    }

    projectPendingDelete?.let { project ->
        DeleteProjectDialog(
            projectName = project.name,
            onConfirm = { deleteSessions ->
                viewModel.deleteProject(project.id, deleteSessions = deleteSessions)
                projectPendingDelete = null
            },
            onDismiss = { projectPendingDelete = null },
        )
    }

    clientPendingDelete?.let { client ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_client_title),
            message = stringResource(R.string.delete_client_message, client.name),
            onConfirm = {
                viewModel.deleteClient(client.id)
                clientPendingDelete = null
            },
            onDismiss = { clientPendingDelete = null },
        )
    }
}

@Composable
private fun SegmentToggle(
    selected: ProjectsPane,
    onSelect: (ProjectsPane) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .padding(4.dp),
    ) {
        SegmentChip(
            label = stringResource(R.string.segment_projects),
            selected = selected == ProjectsPane.Projects,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(ProjectsPane.Projects) },
        )
        SegmentChip(
            label = stringResource(R.string.segment_clients),
            selected = selected == ProjectsPane.Clients,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(ProjectsPane.Clients) },
        )
    }
}

@Composable
private fun SegmentChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AppColors.Anthracite else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) AppColors.TextPrimary else AppColors.TextMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun ProjectItem(
    project: Project,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = Color(project.colorArgb)
    var menuOpen by remember { mutableStateOf(false) }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = AppColors.Accent,
        leftAccent = true,
        padding = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (project.clientName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        project.clientName,
                        color = AppColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (project.hourlyRate > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.hourly_rate_value, project.hourlyRate),
                        color = AppColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.more_actions),
                        tint = AppColors.TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = AppColors.Error) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientItem(
    client: Client,
    projectCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        leftAccent = true,
        padding = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.Anthracite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Business,
                    contentDescription = null,
                    tint = AppColors.Accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    client.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (projectCount == 1) {
                        stringResource(R.string.client_project_count_one)
                    } else {
                        stringResource(R.string.client_project_count, projectCount)
                    },
                    color = AppColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.more_actions),
                        tint = AppColors.TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = AppColors.Error) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectsEmptyState(hasOne: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AppColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.FolderOff,
                contentDescription = null,
                tint = AppColors.TextMuted,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(
                if (hasOne) R.string.projects_empty_more_title else R.string.projects_empty_title,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.projects_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextMuted,
        )
    }
}

@Composable
private fun ClientsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AppColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Business,
                contentDescription = null,
                tint = AppColors.TextMuted,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.clients_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.clients_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextMuted,
        )
    }
}
