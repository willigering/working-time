package de.willigering.workingtime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import de.willigering.workingtime.R
import de.willigering.workingtime.data.Project
import de.willigering.workingtime.data.TimeTrackerRepository
import de.willigering.workingtime.ui.components.SeparatedDropdownItems
import de.willigering.workingtime.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDialog(
    project: Project?,
    knownClients: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Long, Boolean) -> Unit,
) {
    var name by remember(project) { mutableStateOf(project?.name ?: "") }
    var client by remember(project) { mutableStateOf(project?.clientName ?: "") }
    var rateText by remember(project) {
        mutableStateOf(if (project != null && project.hourlyRate > 0) project.hourlyRate.toString() else "")
    }
    var billable by remember(project) { mutableStateOf(project?.billable ?: true) }
    var selectedColor by remember(project) {
        mutableLongStateOf(project?.colorArgb ?: TimeTrackerRepository.PROJECT_COLORS[0])
    }
    var clientMenuOpen by remember { mutableStateOf(false) }
    var filterWhileTyping by remember { mutableStateOf(false) }

    val displayedClients = remember(client, knownClients, filterWhileTyping) {
        val query = client.trim()
        if (!filterWhileTyping || query.isEmpty()) {
            knownClients
        } else {
            knownClients.filter { it.contains(query, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        title = {
            Text(
                if (project == null) {
                    stringResource(R.string.new_project)
                } else {
                    stringResource(R.string.edit_project)
                },
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.project_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors(),
                )
                ExposedDropdownMenuBox(
                    expanded = clientMenuOpen && displayedClients.isNotEmpty(),
                    onExpandedChange = { open ->
                        clientMenuOpen = open
                        if (open) filterWhileTyping = false
                    },
                ) {
                    OutlinedTextField(
                        value = client,
                        onValueChange = {
                            client = it
                            filterWhileTyping = true
                            clientMenuOpen = knownClients.any { name ->
                                name.contains(it.trim(), ignoreCase = true)
                            } || it.isBlank()
                        },
                        label = { Text(stringResource(R.string.client_optional)) },
                        placeholder = { Text(stringResource(R.string.client_select_hint)) },
                        singleLine = true,
                        trailingIcon = {
                            if (knownClients.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = clientMenuOpen && displayedClients.isNotEmpty(),
                                )
                            }
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = dialogFieldColors(),
                    )
                    if (knownClients.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = clientMenuOpen && displayedClients.isNotEmpty(),
                            onDismissRequest = { clientMenuOpen = false },
                        ) {
                            SeparatedDropdownItems(
                                items = displayedClients,
                                selected = client.trim().ifBlank { null },
                                label = { it },
                                isSame = { a, b -> a.equals(b, ignoreCase = true) },
                                onSelect = {
                                    client = it
                                    filterWhileTyping = false
                                    clientMenuOpen = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = rateText,
                    onValueChange = {
                        rateText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                            .replace(',', '.')
                    },
                    label = { Text(stringResource(R.string.hourly_rate)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.billable))
                    Switch(
                        checked = billable,
                        onCheckedChange = { billable = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColors.Background,
                            checkedTrackColor = AppColors.Accent,
                            uncheckedThumbColor = AppColors.TextMuted,
                            uncheckedTrackColor = AppColors.Anthracite,
                        ),
                    )
                }
                Text(
                    stringResource(R.string.color),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in TimeTrackerRepository.PROJECT_COLORS) {
                        val col = Color(c)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    2.dp,
                                    if (selectedColor == c) Color.White else Color.Transparent,
                                    CircleShape,
                                )
                                .clickable { selectedColor = c },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) onSave(name, client, rate, selectedColor, billable)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent,
                    contentColor = AppColors.Background,
                ),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
fun ClientDialog(
    clientName: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(clientName) { mutableStateOf(clientName.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        title = {
            Text(
                if (clientName == null) {
                    stringResource(R.string.new_client)
                } else {
                    stringResource(R.string.edit_client)
                },
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.client_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = dialogFieldColors(),
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent,
                    contentColor = AppColors.Background,
                ),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.Accent,
    unfocusedBorderColor = AppColors.GlassBorder,
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    focusedLabelColor = AppColors.TextSecondary,
    unfocusedLabelColor = AppColors.TextMuted,
    focusedPlaceholderColor = AppColors.TextMuted,
    unfocusedPlaceholderColor = AppColors.TextMuted,
    focusedContainerColor = AppColors.SurfaceElevated,
    unfocusedContainerColor = AppColors.SurfaceElevated,
)
