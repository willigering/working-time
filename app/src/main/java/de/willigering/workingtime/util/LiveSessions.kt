package de.willigering.workingtime.util

import de.willigering.workingtime.data.AppState
import de.willigering.workingtime.data.WorkSession

/** Completed sessions plus the open timer, so totals stay live. */
fun AppState.includingRunning(tick: Long): List<WorkSession> {
    val active = activeSession ?: return sessions
    val project = projects.find { it.id == active.projectId }
    val running = WorkSession(
        id = "running",
        projectId = active.projectId,
        projectName = project?.name.orEmpty(),
        clientName = project?.clientName.orEmpty(),
        hourlyRate = project?.hourlyRate ?: 0.0,
        colorArgb = project?.colorArgb ?: 0xFFE0B15A,
        billable = project?.billable ?: false,
        start = active.start,
        end = tick.coerceAtLeast(active.start),
        notes = active.notes,
    )
    return sessions + running
}
