package de.willigering.workingtime.ui.screens

import androidx.compose.foundation.Canvas
import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import java.util.Calendar
import de.willigering.workingtime.export.ExportBuilder
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.willigering.workingtime.R
import de.willigering.workingtime.data.WorkSession
import de.willigering.workingtime.ui.components.ConfirmDeleteDialog
import de.willigering.workingtime.ui.components.GlassCard
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionsScreen(viewModel: TimeTrackerViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var projectFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var from by rememberSaveable { mutableStateOf<Long?>(null) }
    var to by rememberSaveable { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<WorkSession?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    val visible = remember(state.sessions, state.projects, query, projectFilter, from, to) {
        state.sessions.filter { session ->
            (projectFilter == null || session.projectId == projectFilter) &&
            (from == null || session.end > from!!) && (to == null || session.start <= to!!) &&
            listOf(viewModel.sessionProjectName(session), viewModel.sessionClientName(session), session.notes)
                .any { it.contains(query.trim(), ignoreCase = true) }
        }.sortedByDescending { it.start }
    }
    fun pickDate(current: Long?, select: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { if (current != null) timeInMillis = current }
        DatePickerDialog(context, { _, y, m, d ->
            val date = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
            select(date.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }
    var showExport by remember { mutableStateOf(false) }
    var sessionPendingDelete by remember { mutableStateOf<WorkSession?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.sessions_title), style = MaterialTheme.typography.titleLarge)
            OutlinedButton(
                onClick = { showExport = true },
                enabled = state.sessions.isNotEmpty(),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  ${stringResource(R.string.export_action)}")
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
            label = { Text(stringResource(R.string.search_sessions)) }, modifier = Modifier.fillMaxWidth())
        Row {
            TextButton(onClick = { pickDate(from) { from = ExportBuilder.startOfDay(it) } }) {
                Text(stringResource(R.string.export_from) + (from?.let { ": " + Formatters.date(it) } ?: ""))
            }
            TextButton(onClick = { pickDate(to) { to = ExportBuilder.endOfDay(it) } }) {
                Text(stringResource(R.string.export_to) + (to?.let { ": " + Formatters.date(it) } ?: ""))
            }
            TextButton(onClick = { from = null; to = null; projectFilter = null; query = "" }) {
                Text(stringResource(R.string.reset_filters))
            }
        }
        OutlinedButton(onClick = { editing = null; showEdit = true }, enabled = state.projects.isNotEmpty()) {
            Text(stringResource(R.string.add_session))
        }
        if (state.sessions.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sessions_empty_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.sessions_empty_hint),
                    color = AppColors.TextSecondary,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = projectFilter == null, onClick = { projectFilter = null },
                            label = { Text(stringResource(R.string.export_scope_all)) })
                        state.sessions.distinctBy { it.projectId }.forEach { sample ->
                            FilterChip(selected = projectFilter == sample.projectId,
                                onClick = { projectFilter = sample.projectId },
                                label = { Text(viewModel.sessionProjectName(sample)) })
                        }
                    }
                }
                if (visible.isEmpty()) item { Text(stringResource(R.string.export_empty)) }
                visible.groupBy { Formatters.date(it.start) }.forEach { (date, entries) ->
                item { Text(date, style = MaterialTheme.typography.titleSmall) }
                items(entries, key = { it.id }) { session ->
                    SessionItem(
                        session = session,
                        viewModel = viewModel,
                        onDelete = { sessionPendingDelete = session },
                        onEdit = { editing = session; showEdit = true },
                    )
                }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showEdit) SessionDialog(editing, state.projects,
        onDismiss = { showEdit = false },
        onSave = { viewModel.saveSession(it); showEdit = false })

    if (showExport) {
        ExportDialog(
            viewModel = viewModel,
            onDismiss = { showExport = false },
        )
    }

    sessionPendingDelete?.let { session ->
        val projectName = viewModel.sessionProjectName(session)
        val detail = buildString {
            append(projectName)
            append(" \u00B7 ")
            append(Formatters.date(session.start))
            append(" \u00B7 ")
            append(Formatters.time(session.start))
            append(" \u2013 ")
            append(Formatters.time(session.end))
        }
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_session_title),
            message = stringResource(R.string.delete_session_message),
            detail = detail,
            onConfirm = {
                viewModel.deleteSession(session.id)
                sessionPendingDelete = null
            },
            onDismiss = { sessionPendingDelete = null },
        )
    }
}

@Composable
private fun SessionItem(
    session: WorkSession,
    viewModel: TimeTrackerViewModel,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val projectName = viewModel.sessionProjectName(session)
    val clientName = viewModel.sessionClientName(session)
    val rate = viewModel.sessionRate(session)
    val color = Color(viewModel.sessionColor(session))
    val mins = (session.end - session.start) / 60_000
    val earnings = viewModel.sessionEarnings(session)

    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit), accentColor = color) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(10.dp)) { drawCircle(color = color) }
                    Text(
                        "  $projectName",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                    )
                }
                if (clientName.isNotBlank()) {
                    Text(
                        clientName,
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextSecondary,
                    )
                }
                Text(
                    Formatters.date(session.start),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextMuted,
                )
                Text(
                    // en-dash via escape (avoids UTF-8 mojibake in source)
                    "${Formatters.time(session.start)} \u2013 ${if (Formatters.date(session.start) != Formatters.date(session.end)) Formatters.date(session.end) + " " else ""}${Formatters.time(session.end)}",
                    color = AppColors.TextSecondary,
                )
                val meta = buildString {
                    append(Formatters.duration(context, mins))
                    // middle dot · via escape
                    append(" \u00B7 ")
                    append(Formatters.currency(context, earnings))
                    if (rate > 0) {
                        append(" \u00B7 ")
                        append(context.getString(R.string.hourly_rate_value, rate))
                    }
                }
                Text(meta, color = AppColors.Success)
                if (session.notes.isNotBlank()) {
                    Text(session.notes, color = AppColors.TextMuted)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = AppColors.Error,
                )
            }
        }
    }
}
