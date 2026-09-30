package de.willigering.workingtime.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.willigering.workingtime.R
import de.willigering.workingtime.data.Project
import de.willigering.workingtime.data.WorkSession
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters
import de.willigering.workingtime.util.TimeMath
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionDialog(session: WorkSession?, projects: List<Project>, onDismiss: () -> Unit, onSave: (WorkSession) -> Unit) {
    val context = LocalContext.current
    var projectId by rememberSaveable { mutableStateOf(session?.projectId ?: projects.firstOrNull()?.id) }
    var start by rememberSaveable { mutableStateOf(session?.start ?: System.currentTimeMillis() - 3_600_000) }
    var end by rememberSaveable { mutableStateOf(session?.end ?: System.currentTimeMillis()) }
    var notes by rememberSaveable { mutableStateOf(session?.notes.orEmpty()) }
    var rateText by rememberSaveable { mutableStateOf(String.format(Locale.getDefault(), "%.2f",
        session?.hourlyRate ?: projects.firstOrNull()?.hourlyRate ?: 0.0)) }
    var billable by rememberSaveable { mutableStateOf(session?.billable ?: projects.firstOrNull()?.billable ?: true) }
    val rate = TimeMath.parseRate(rateText)

    fun pick(value: Long, update: (Long) -> Unit) {
        val c = Calendar.getInstance().apply { timeInMillis = value }
        DatePickerDialog(context, { _, y, m, d ->
            c.set(Calendar.YEAR, y); c.set(Calendar.MONTH, m); c.set(Calendar.DAY_OF_MONTH, d)
            TimePickerDialog(context, { _, h, minute ->
                c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, minute)
                c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                update(c.timeInMillis)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppColors.Surface,
        title = { Text(stringResource(if (session == null) R.string.add_session else R.string.edit_session)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.project_name))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (session != null && projects.none { it.id == session.projectId }) {
                        FilterChip(selected = projectId == session.projectId,
                            onClick = { projectId = session.projectId },
                            label = { Text(session.projectName) })
                    }
                    projects.forEach { project ->
                        FilterChip(selected = projectId == project.id, onClick = {
                            projectId = project.id
                            rateText = String.format(Locale.getDefault(), "%.2f", project.hourlyRate)
                            billable = project.billable
                        }, label = { Text(project.name) })
                    }
                }
                TextButton(onClick = { pick(start) { start = it } }) {
                    Text(stringResource(R.string.export_col_start) + ": " + Formatters.date(start) + " " + Formatters.time(start))
                }
                TextButton(onClick = { pick(end) { end = it } }) {
                    Text(stringResource(R.string.export_col_end) + ": " + Formatters.date(end) + " " + Formatters.time(end))
                }
                if (end <= start) Text(stringResource(R.string.invalid_session_time), color = AppColors.Error)
                OutlinedTextField(value = rateText, onValueChange = { rateText = it },
                    label = { Text(stringResource(R.string.hourly_rate)) }, isError = rate == null,
                    supportingText = { if (rate == null) Text(stringResource(R.string.invalid_rate)) })
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.billable))
                    Switch(checked = billable, onCheckedChange = { billable = it })
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.timer_note_label)) })
                Text(stringResource(R.string.minute_policy), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(enabled = projectId != null && end > start && rate != null, onClick = {
                val project = projects.find { it.id == projectId }
                val original = session?.takeIf { it.projectId == projectId }
                val edited = (original ?: WorkSession(
                    projectId = projectId ?: return@Button, projectName = project?.name.orEmpty(),
                    clientName = project?.clientName.orEmpty(), colorArgb = project?.colorArgb ?: 0xFFFFB300,
                    start = start, end = end,
                )).copy(
                    id = session?.id ?: java.util.UUID.randomUUID().toString(),
                    start = start, end = end, notes = notes.trim(), hourlyRate = rate ?: return@Button,
                    billable = billable,
                )
                onSave(edited)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
