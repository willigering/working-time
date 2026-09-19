package de.willigering.workingtime.export

import de.willigering.workingtime.data.WorkSession

enum class ExportFormat {
    CSV,
    XLSX,
    PDF_TIMESHEET,
    PDF_MONTHLY,
}

enum class ExportScope {
    ALL,
    PERIOD,
    PROJECT,
    CLIENT,
}

data class ExportFilter(
    val format: ExportFormat = ExportFormat.CSV,
    val scope: ExportScope = ExportScope.ALL,
    /** Inclusive start of day (millis). */
    val periodFrom: Long? = null,
    /** Inclusive end of day (millis). */
    val periodTo: Long? = null,
    val projectId: String? = null,
    val clientName: String? = null,
)

/** Flattened row used by all exporters. */
data class ExportRow(
    val session: WorkSession,
    val date: String,
    val project: String,
    val client: String,
    val start: String,
    val end: String,
    /** Pause in minutes (0 until break tracking exists). */
    val pauseMinutes: Long,
    val workMinutes: Long,
    val hourlyRate: Double,
    val earnings: Double,
    val notes: String,
)

data class ExportSummary(
    /** Issuer display name (user), not the app name. */
    val title: String,
    val companyName: String,
    /** Absolute path to user logo, or null. */
    val logoPath: String?,
    val projectLabel: String,
    val clientLabel: String,
    val periodLabel: String,
    val totalMinutes: Long,
    val averageRate: Double,
    val totalEarnings: Double,
    val rows: List<ExportRow>,
)
