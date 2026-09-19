package de.willigering.workingtime.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.willigering.workingtime.R
import de.willigering.workingtime.export.ExportBuilder
import de.willigering.workingtime.export.ExportFilter
import de.willigering.workingtime.export.ExportFormat
import de.willigering.workingtime.export.ExportScope
import de.willigering.workingtime.export.ExportShare
import de.willigering.workingtime.ui.components.SeparatedDropdownItems
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    viewModel: TimeTrackerViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var format by remember { mutableStateOf(ExportFormat.CSV) }
    var scopeFilter by remember { mutableStateOf(ExportScope.ALL) }

    val month = remember { ExportBuilder.currentMonthRange() }
    var periodFrom by remember { mutableStateOf(month.first) }
    var periodTo by remember { mutableStateOf(month.second) }

    var projectId by remember {
        mutableStateOf(state.projects.firstOrNull()?.id ?: state.selectedProjectId)
    }
    val clients = remember(state.clients, state.projects, state.sessions) {
        viewModel.uniqueClientNames()
    }
    var clientName by remember { mutableStateOf(clients.firstOrNull().orEmpty()) }

    var projectMenuOpen by remember { mutableStateOf(false) }
    var clientMenuOpen by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    fun pickDate(current: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val c = Calendar.getInstance()
                c.set(y, m, d, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                onPicked(c.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    AlertDialog(
        onDismissRequest = { if (!exporting) onDismiss() },
        containerColor = AppColors.Surface,
        title = { Text(stringResource(R.string.export_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.export_format),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextMuted,
                )
                FormatChipRow(format) { format = it }

                Text(
                    stringResource(R.string.export_scope),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextMuted,
                )
                ScopeChipRow(scopeFilter) { scopeFilter = it }

                when (scopeFilter) {
                    ExportScope.PERIOD -> {
                        TextButton(onClick = {
                            pickDate(periodFrom) { periodFrom = ExportBuilder.startOfDay(it) }
                        }) {
                            Text("${stringResource(R.string.export_from)}: ${Formatters.date(periodFrom)}")
                        }
                        TextButton(onClick = {
                            pickDate(periodTo) { periodTo = ExportBuilder.endOfDay(it) }
                        }) {
                            Text("${stringResource(R.string.export_to)}: ${Formatters.date(periodTo)}")
                        }
                    }
                    ExportScope.PROJECT -> {
                        val selectedName = state.projects.find { it.id == projectId }?.name
                            ?: stringResource(R.string.export_pick_project)
                        ExposedDropdownMenuBox(
                            expanded = projectMenuOpen,
                            onExpandedChange = { projectMenuOpen = it },
                        ) {
                            OutlinedTextField(
                                value = selectedName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.export_label_project)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(projectMenuOpen) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = projectMenuOpen,
                                onDismissRequest = { projectMenuOpen = false },
                            ) {
                                SeparatedDropdownItems(
                                    items = state.projects,
                                    selected = state.projects.find { it.id == projectId },
                                    label = { it.name },
                                    leadingIcon = Icons.Rounded.Folder,
                                    onSelect = {
                                        projectId = it.id
                                        projectMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    ExportScope.CLIENT -> {
                        ExposedDropdownMenuBox(
                            expanded = clientMenuOpen,
                            onExpandedChange = { clientMenuOpen = it },
                        ) {
                            OutlinedTextField(
                                value = clientName.ifBlank { stringResource(R.string.export_pick_client) },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.export_label_client)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(clientMenuOpen) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = clientMenuOpen,
                                onDismissRequest = { clientMenuOpen = false },
                            ) {
                                SeparatedDropdownItems(
                                    items = clients,
                                    selected = clientName.trim().ifBlank { null },
                                    label = { it },
                                    isSame = { a, b -> a.equals(b, ignoreCase = true) },
                                    onSelect = {
                                        clientName = it
                                        clientMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    ExportScope.ALL -> Unit
                }

                Text(
                    stringResource(R.string.export_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !exporting && state.sessions.isNotEmpty(),
                onClick = {
                    if (scopeFilter == ExportScope.PROJECT && projectId == null) {
                        Toast.makeText(context, R.string.export_pick_project, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (scopeFilter == ExportScope.CLIENT && clientName.isBlank()) {
                        Toast.makeText(context, R.string.export_pick_client, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    exporting = true
                    val filter = ExportFilter(
                        format = format,
                        scope = scopeFilter,
                        periodFrom = if (scopeFilter == ExportScope.PERIOD) {
                            ExportBuilder.startOfDay(periodFrom)
                        } else {
                            null
                        },
                        periodTo = if (scopeFilter == ExportScope.PERIOD) {
                            ExportBuilder.endOfDay(periodTo)
                        } else {
                            null
                        },
                        projectId = if (scopeFilter == ExportScope.PROJECT) projectId else null,
                        clientName = if (scopeFilter == ExportScope.CLIENT) clientName else null,
                    )
                    scope.launch {
                        try {
                            val file = viewModel.createExportFile(context, filter)
                            if (file == null) {
                                Toast.makeText(context, R.string.export_empty, Toast.LENGTH_SHORT).show()
                            } else {
                                ExportShare.shareFile(
                                    context = context,
                                    file = file,
                                    mimeType = ExportShare.mimeFor(format),
                                    chooserTitle = context.getString(R.string.export_share),
                                )
                                onDismiss()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.export_failed, e.message ?: ""),
                                Toast.LENGTH_LONG,
                            ).show()
                        } finally {
                            exporting = false
                        }
                    }
                },
            ) {
                Text(
                    if (exporting) stringResource(R.string.export_working)
                    else stringResource(R.string.export_action),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !exporting) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun FormatChipRow(selected: ExportFormat, onSelect: (ExportFormat) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selected == ExportFormat.CSV,
                onClick = { onSelect(ExportFormat.CSV) },
                label = { Text(stringResource(R.string.export_format_csv)) },
            )
            FilterChip(
                selected = selected == ExportFormat.XLSX,
                onClick = { onSelect(ExportFormat.XLSX) },
                label = { Text(stringResource(R.string.export_format_xlsx)) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selected == ExportFormat.PDF_TIMESHEET,
                onClick = { onSelect(ExportFormat.PDF_TIMESHEET) },
                label = { Text(stringResource(R.string.export_format_pdf_timesheet)) },
            )
            FilterChip(
                selected = selected == ExportFormat.PDF_MONTHLY,
                onClick = { onSelect(ExportFormat.PDF_MONTHLY) },
                label = { Text(stringResource(R.string.export_format_pdf_monthly)) },
            )
        }
    }
}

@Composable
private fun ScopeChipRow(selected: ExportScope, onSelect: (ExportScope) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selected == ExportScope.ALL,
                onClick = { onSelect(ExportScope.ALL) },
                label = { Text(stringResource(R.string.export_scope_all)) },
            )
            FilterChip(
                selected = selected == ExportScope.PERIOD,
                onClick = { onSelect(ExportScope.PERIOD) },
                label = { Text(stringResource(R.string.export_scope_period)) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selected == ExportScope.PROJECT,
                onClick = { onSelect(ExportScope.PROJECT) },
                label = { Text(stringResource(R.string.export_scope_project)) },
            )
            FilterChip(
                selected = selected == ExportScope.CLIENT,
                onClick = { onSelect(ExportScope.CLIENT) },
                label = { Text(stringResource(R.string.export_scope_client)) },
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}
