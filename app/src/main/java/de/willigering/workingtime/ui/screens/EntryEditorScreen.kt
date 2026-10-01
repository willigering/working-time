package de.willigering.workingtime.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.willigering.workingtime.R
import de.willigering.workingtime.data.Project
import de.willigering.workingtime.data.WorkSession
import de.willigering.workingtime.ui.components.ConfirmDeleteDialog
import de.willigering.workingtime.ui.components.ProjectMark
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters
import de.willigering.workingtime.util.TimeMath
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Composable
fun EntryEditorScreen(
    session: WorkSession?,
    projects: List<Project>,
    onBack: () -> Unit,
    onSave: (WorkSession) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    val choices = projects.filter { !it.archived || it.id == session?.projectId }
    var projectId by rememberSaveable(session?.id) {
        mutableStateOf(session?.projectId ?: choices.firstOrNull()?.id)
    }
    var start by rememberSaveable(session?.id) {
        mutableStateOf(session?.start ?: System.currentTimeMillis() - 3_600_000)
    }
    var end by rememberSaveable(session?.id) {
        mutableStateOf(session?.end ?: System.currentTimeMillis())
    }
    var notes by rememberSaveable(session?.id) { mutableStateOf(session?.notes.orEmpty()) }
    var rateText by rememberSaveable(session?.id) {
        mutableStateOf(
            String.format(
                Locale.getDefault(),
                "%.2f",
                session?.hourlyRate ?: choices.firstOrNull()?.hourlyRate ?: 0.0,
            ),
        )
    }
    var billable by rememberSaveable(session?.id) {
        mutableStateOf(session?.billable ?: choices.firstOrNull()?.billable ?: true)
    }
    var projectMenu by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val rate = TimeMath.parseRate(rateText)
    val valid = projectId != null && end > start && rate != null
    val selected = choices.find { it.id == projectId }
        ?: session?.takeIf { it.projectId == projectId }?.let {
            Project(id = it.projectId, name = it.projectName, clientName = it.clientName, colorArgb = it.colorArgb)
        }

    fun pickDate(value: Long, update: (Long) -> Unit) {
        val c = Calendar.getInstance().apply { timeInMillis = value }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                c.set(Calendar.YEAR, y)
                c.set(Calendar.MONTH, m)
                c.set(Calendar.DAY_OF_MONTH, d)
                update(c.timeInMillis)
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    fun pickTime(value: Long, update: (Long) -> Unit) {
        val c = Calendar.getInstance().apply { timeInMillis = value }
        TimePickerDialog(
            context,
            { _, h, minute ->
                c.set(Calendar.HOUR_OF_DAY, h)
                c.set(Calendar.MINUTE, minute)
                c.set(Calendar.SECOND, 0)
                c.set(Calendar.MILLISECOND, 0)
                update(c.timeInMillis)
            },
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            true,
        ).show()
    }

    fun save() {
        val id = projectId ?: return
        val parsed = rate ?: return
        if (end <= start) return
        val project = projects.find { it.id == id }
        val base = session?.takeIf { it.projectId == id } ?: WorkSession(
            projectId = id,
            projectName = project?.name ?: session?.projectName.orEmpty(),
            clientName = project?.clientName ?: session?.clientName.orEmpty(),
            colorArgb = project?.colorArgb ?: session?.colorArgb ?: 0xFFE0B15A,
            start = start,
            end = end,
        )
        onSave(
            base.copy(
                id = session?.id ?: UUID.randomUUID().toString(),
                start = start,
                end = end,
                notes = notes.trim(),
                hourlyRate = parsed,
                billable = billable,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = AppColors.TextPrimary)
            }
            Text(
                stringResource(if (session == null) R.string.home_new_entry else R.string.session_edit_title),
                modifier = Modifier.weight(1f),
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            TextButton(onClick = { save() }, enabled = valid) {
                Text(
                    stringResource(R.string.save),
                    color = if (valid) AppColors.Accent else AppColors.TextMuted,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        FieldLabel(stringResource(R.string.export_label_project))
        Box {
            EditorCard(onClick = { projectMenu = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProjectMark(
                        selected?.name ?: stringResource(R.string.unknown_project),
                        Color(selected?.colorArgb ?: 0xFFE0B15A),
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            selected?.name ?: stringResource(R.string.export_pick_project),
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!selected?.clientName.isNullOrBlank()) {
                            Text(selected?.clientName.orEmpty(), color = AppColors.TextMuted, fontSize = 13.sp)
                        }
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AppColors.TextMuted)
                }
            }
            DropdownMenu(
                expanded = projectMenu,
                onDismissRequest = { projectMenu = false },
            ) {
                choices.forEach { project ->
                    DropdownMenuItem(
                        text = { Text(project.name, color = AppColors.TextPrimary) },
                        onClick = {
                            projectId = project.id
                            rateText = String.format(Locale.getDefault(), "%.2f", project.hourlyRate)
                            billable = project.billable
                            projectMenu = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.label_start))
        TimePair(
            millis = start,
            onDate = { pickDate(start) { start = it } },
            onTime = { pickTime(start) { start = it } },
        )
        Spacer(Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.label_end))
        TimePair(
            millis = end,
            onDate = { pickDate(end) { end = it } },
            onTime = { pickTime(end) { end = it } },
        )
        Spacer(Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.label_duration))
        EditorCard {
            Text(
                if (end > start) Formatters.hoursLabel(TimeMath.minutes(start, end))
                else stringResource(R.string.invalid_session_time),
                color = if (end > start) AppColors.TextPrimary else AppColors.Error,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.label_note))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            colors = fieldColors(),
        )
        Spacer(Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.hourly_rate))
        OutlinedTextField(
            value = rateText,
            onValueChange = { rateText = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = rate == null,
            supportingText = { if (rate == null) Text(stringResource(R.string.invalid_rate)) },
            colors = fieldColors(),
        )
        EditorCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.billable), color = AppColors.TextPrimary)
                Switch(
                    checked = billable,
                    onCheckedChange = { billable = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AppColors.Accent,
                        checkedThumbColor = AppColors.Ink,
                    ),
                )
            }
        }
        Text(
            stringResource(R.string.minute_policy),
            color = AppColors.TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (onDelete != null) {
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { confirmDelete = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = AppColors.Error, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.entry_delete_action), color = AppColors.Error, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete && onDelete != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_session_title),
            message = stringResource(R.string.delete_session_message),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = AppColors.TextMuted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp, start = 2.dp))
}

@Composable
private fun EditorCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.GlassBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) { content() }
}

@Composable
private fun TimePair(millis: Long, onDate: () -> Unit, onTime: () -> Unit) {
    EditorCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                Formatters.date(millis),
                color = AppColors.TextPrimary,
                fontSize = 16.sp,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onDate),
            )
            Text(
                Formatters.time(millis),
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.clickable(onClick = onTime),
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.Accent,
    unfocusedBorderColor = AppColors.GlassBorder,
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    focusedLabelColor = AppColors.Accent,
    unfocusedLabelColor = AppColors.TextMuted,
    cursorColor = AppColors.Accent,
    focusedContainerColor = AppColors.Surface,
    unfocusedContainerColor = AppColors.Surface,
)
