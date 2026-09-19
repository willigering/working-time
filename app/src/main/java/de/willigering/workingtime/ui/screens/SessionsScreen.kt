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

@Composable
fun SessionsScreen(viewModel: TimeTrackerViewModel) {
    val state by viewModel.state.collectAsState()
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
                items(state.sessions, key = { it.id }) { session ->
                    SessionItem(
                        session = session,
                        viewModel = viewModel,
                        onDelete = { sessionPendingDelete = session },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

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
) {
    val context = LocalContext.current
    val projectName = viewModel.sessionProjectName(session)
    val clientName = viewModel.sessionClientName(session)
    val rate = viewModel.sessionRate(session)
    val color = Color(viewModel.sessionColor(session))
    val mins = (session.end - session.start) / 60_000
    val earnings = viewModel.sessionEarnings(session)

    GlassCard(modifier = Modifier.fillMaxWidth(), accentColor = color) {
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
                    "${Formatters.time(session.start)} \u2013 ${Formatters.time(session.end)}",
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
