package de.willigering.workingtime.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.willigering.workingtime.R
import de.willigering.workingtime.data.WorkSession
import de.willigering.workingtime.ui.components.ConfirmDeleteDialog
import de.willigering.workingtime.ui.components.RangeSegment
import de.willigering.workingtime.ui.components.ScreenHeading
import de.willigering.workingtime.ui.components.TimeEntryRow
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters
import de.willigering.workingtime.util.Periods
import de.willigering.workingtime.util.TimeMath
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel
import java.util.Calendar

@Composable
fun SessionsScreen(
    viewModel: TimeTrackerViewModel,
    projectFilter: String?,
    onProjectFilter: (String?) -> Unit,
    onEditSession: (WorkSession) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var range by rememberSaveable { mutableIntStateOf(0) }
    var anchor by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var dayOnly by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf<WorkSession?>(null) }

    val bounds = when (range) {
        1 -> Periods.weekBounds(anchor)
        2 -> Periods.monthBounds(anchor)
        else -> if (dayOnly) Periods.dayBounds(anchor) else Periods.weekBounds(anchor)
    }
    val days = Periods.weekDays(anchor)
    val visible = remember(state.sessions, query, projectFilter, bounds, state.projects) {
        state.sessions.filter { session ->
            session.end > bounds.first && session.start < bounds.second &&
                (projectFilter == null || session.projectId == projectFilter) &&
                listOf(viewModel.sessionProjectName(session), viewModel.sessionClientName(session), session.notes)
                    .any { it.contains(query.trim(), ignoreCase = true) }
        }.sortedByDescending { it.start }
    }
    val grouped = visible.groupBy { Periods.dayBounds(it.start).first }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScreenHeading(stringResource(R.string.tab_times), modifier = Modifier.weight(1f))
            Icon(
                Icons.Rounded.Search,
                contentDescription = stringResource(R.string.search_sessions),
                tint = if (searching) AppColors.Accent else AppColors.TextSecondary,
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(22.dp)
                    .clickable { searching = !searching },
            )
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = stringResource(R.string.pick_day),
                tint = AppColors.TextSecondary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = anchor }
                        DatePickerDialog(context, { _, y, m, d ->
                            val picked = Calendar.getInstance().apply {
                                set(y, m, d, 12, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            anchor = picked.timeInMillis
                            dayOnly = range == 0
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    },
            )
        }
        if (searching) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.search_sessions)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Accent,
                    unfocusedBorderColor = AppColors.GlassBorder,
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    focusedLabelColor = AppColors.Accent,
                    unfocusedLabelColor = AppColors.TextMuted,
                    cursorColor = AppColors.Accent,
                ),
            )
        }
        if (projectFilter != null) {
            val name = state.projects.find { it.id == projectFilter }?.name
                ?: state.sessions.find { it.projectId == projectFilter }?.let { viewModel.sessionProjectName(it) }
                ?: stringResource(R.string.unknown_project)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = AppColors.Accent, fontWeight = FontWeight.Medium)
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.reset_filters),
                    tint = AppColors.Accent,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(16.dp)
                        .clickable { onProjectFilter(null) },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        RangeSegment(
            labels = listOf(
                stringResource(R.string.times_day),
                stringResource(R.string.times_week),
                stringResource(R.string.times_month),
            ),
            selected = range,
            onSelect = {
                range = it
                if (it != 0) dayOnly = false
            },
        )
        if (range == 0) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    val selected = Periods.sameDay(day, anchor)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (selected && dayOnly) dayOnly = false else {
                                    anchor = day
                                    dayOnly = true
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(Formatters.weekdayShort(day), color = AppColors.TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .then(
                                    if (selected) Modifier.background(AppColors.Accent, CircleShape) else Modifier,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                Formatters.dayNumber(day),
                                color = if (selected) AppColors.Ink else AppColors.TextPrimary,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (visible.isEmpty()) {
            Text(
                stringResource(if (state.sessions.isEmpty()) R.string.sessions_empty_title else R.string.export_empty),
                color = AppColors.TextMuted,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                grouped.forEach { (day, entries) ->
                    item(key = "day-$day") {
                        val mins = entries.sumOf { TimeMath.minutes(it.start, it.end) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(Formatters.dateLong(day), color = AppColors.TextSecondary, fontSize = 14.sp)
                            Text(Formatters.hoursLabel(mins), color = AppColors.Accent, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                    items(entries.size, key = { entries[it].id }) { index ->
                        val session = entries[index]
                        TimeEntryRow(
                            name = viewModel.sessionProjectName(session),
                            subtitle = "${Formatters.time(session.start)} – ${Formatters.time(session.end)}",
                            duration = Formatters.hoursLabel(TimeMath.minutes(session.start, session.end)),
                            color = Color(viewModel.sessionColor(session)),
                            onClick = { onEditSession(session) },
                            onEdit = { onEditSession(session) },
                            onDelete = { pending = session },
                        )
                    }
                }
                if (range == 2) {
                    item(key = "month-stats") {
                        Spacer(Modifier.height(18.dp))
                        MonthProjectBreakdown(viewModel, anchor)
                        Spacer(Modifier.height(24.dp))
                    }
                } else {
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    pending?.let { session ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_session_title),
            message = stringResource(R.string.delete_session_message),
            detail = viewModel.sessionProjectName(session),
            onConfirm = {
                viewModel.deleteSession(session.id)
                pending = null
            },
            onDismiss = { pending = null },
        )
    }
}
