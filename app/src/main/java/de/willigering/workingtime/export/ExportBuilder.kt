package de.willigering.workingtime.export

import android.content.Context
import de.willigering.workingtime.R
import de.willigering.workingtime.data.TimeTrackerRepository
import de.willigering.workingtime.data.WorkSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ExportBuilder {

    fun filterSessions(
        sessions: List<WorkSession>,
        repository: TimeTrackerRepository,
        filter: ExportFilter,
    ): List<WorkSession> {
        return sessions.filter { s ->
            when (filter.scope) {
                ExportScope.ALL -> true
                ExportScope.PERIOD -> {
                    val from = filter.periodFrom ?: return@filter true
                    val to = filter.periodTo ?: return@filter true
                    s.start in from..to || s.end in from..to ||
                        (s.start <= from && s.end >= to)
                }
                ExportScope.PROJECT -> {
                    val id = filter.projectId ?: return@filter true
                    s.projectId == id
                }
                ExportScope.CLIENT -> {
                    val client = filter.clientName?.trim()?.lowercase(Locale.getDefault())
                        ?: return@filter true
                    repository.sessionClientName(s).trim().lowercase(Locale.getDefault()) == client
                }
            }
        }.sortedBy { it.start }
    }

    fun buildRows(
        sessions: List<WorkSession>,
        repository: TimeTrackerRepository,
        locale: Locale = Locale.getDefault(),
    ): List<ExportRow> {
        val dateFmt = SimpleDateFormat(if (locale.language == "de") "dd.MM.yyyy" else "MM/dd/yyyy", locale)
        val timeFmt = SimpleDateFormat("HH:mm", locale)
        return sessions.map { s ->
            val mins = ((s.end - s.start) / 60_000).coerceAtLeast(0)
            ExportRow(
                session = s,
                date = dateFmt.format(s.start),
                project = repository.sessionProjectName(s),
                client = repository.sessionClientName(s),
                start = timeFmt.format(s.start),
                end = timeFmt.format(s.end),
                pauseMinutes = 0L,
                workMinutes = mins,
                hourlyRate = repository.sessionRate(s),
                earnings = repository.sessionEarnings(s),
                notes = s.notes,
            )
        }
    }

    fun buildSummary(
        context: Context,
        rows: List<ExportRow>,
        filter: ExportFilter,
        repository: TimeTrackerRepository,
        locale: Locale = Locale.getDefault(),
    ): ExportSummary {
        val totalMinutes = rows.sumOf { it.workMinutes }
        val totalEarnings = rows.sumOf { it.earnings }
        val rates = rows.map { it.hourlyRate }.filter { it > 0 }
        val avgRate = if (rates.isEmpty()) 0.0 else rates.average()

        val projectLabel = when {
            filter.scope == ExportScope.PROJECT && filter.projectId != null -> {
                rows.firstOrNull()?.project
                    ?: repository.projectById(filter.projectId)?.name
                    ?: "—"
            }
            rows.map { it.project }.distinct().size == 1 -> rows.first().project
            else -> context.getString(R.string.export_all_projects)
        }

        val clientLabel = when {
            filter.scope == ExportScope.CLIENT && !filter.clientName.isNullOrBlank() ->
                filter.clientName
            rows.map { it.client }.filter { it.isNotBlank() }.distinct().size == 1 ->
                rows.first { it.client.isNotBlank() }.client
            else -> {
                val clients = rows.map { it.client }.filter { it.isNotBlank() }.distinct()
                if (clients.isEmpty()) "—" else clients.joinToString(", ")
            }
        }

        val periodLabel = periodLabel(context, rows, filter, locale)

        val profile = repository.state.value.userProfile
        val issuer = profile.issuerTitle().ifBlank {
            context.getString(R.string.export_issuer_fallback)
        }
        val logoPath = profile.logoPath.takeIf { it.isNotBlank() && java.io.File(it).exists() }

        return ExportSummary(
            title = issuer,
            companyName = profile.companyName.trim(),
            logoPath = logoPath,
            projectLabel = projectLabel,
            clientLabel = clientLabel,
            periodLabel = periodLabel,
            totalMinutes = totalMinutes,
            averageRate = avgRate,
            totalEarnings = totalEarnings,
            rows = rows,
        )
    }

    fun durationHm(totalMinutes: Long): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return String.format(Locale.GERMAN, "%d:%02d", h, m)
    }

    fun currencyDe(amount: Double): String =
        String.format(Locale.GERMAN, "%,.2f €", amount)

    fun rateDe(rate: Double): String =
        if (rate <= 0) "—" else String.format(Locale.GERMAN, "%,.2f €", rate)

    fun pauseHm(minutes: Long): String = durationHm(minutes)

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        return from to cal.timeInMillis
    }

    private fun periodLabel(
        context: Context,
        rows: List<ExportRow>,
        filter: ExportFilter,
        locale: Locale,
    ): String {
        val dateFmt = SimpleDateFormat(if (locale.language == "de") "dd.MM.yyyy" else "MM/dd/yyyy", locale)
        if (filter.scope == ExportScope.PERIOD && filter.periodFrom != null && filter.periodTo != null) {
            return "${dateFmt.format(filter.periodFrom)} - ${dateFmt.format(filter.periodTo)}"
        }
        if (rows.isEmpty()) return "—"
        val first = rows.minOf { it.session.start }
        val last = rows.maxOf { it.session.start }
        return "${dateFmt.format(first)} - ${dateFmt.format(last)}"
    }
}
