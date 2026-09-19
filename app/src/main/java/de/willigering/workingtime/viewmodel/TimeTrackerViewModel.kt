package de.willigering.workingtime.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.willigering.workingtime.data.AppState
import de.willigering.workingtime.data.Client
import de.willigering.workingtime.data.Project
import de.willigering.workingtime.data.TimeTrackerRepository
import de.willigering.workingtime.data.WorkSession
import de.willigering.workingtime.export.CsvExporter
import de.willigering.workingtime.export.ExportBuilder
import de.willigering.workingtime.export.ExportFilter
import de.willigering.workingtime.export.ExportFormat
import de.willigering.workingtime.export.ExportShare
import de.willigering.workingtime.export.ExportSummary
import de.willigering.workingtime.export.PdfExporter
import de.willigering.workingtime.export.XlsxExporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class UndoKind { Project, ProjectAndEntries, Session, Client }

data class UndoBannerState(
    val kind: UndoKind,
    val remainingMs: Long,
)

class TimeTrackerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val UNDO_MS = 5_000L
    }

    private val repository = TimeTrackerRepository(application.applicationContext)

    private var undoSnapshot: AppState? = null
    private var undoJob: Job? = null
    private val _undoBanner = MutableStateFlow<UndoBannerState?>(null)
    val undoBanner: StateFlow<UndoBannerState?> = _undoBanner.asStateFlow()

    val state: StateFlow<AppState> = repository.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppState(),
    )

    private val _tick = MutableStateFlow(System.currentTimeMillis())
    val tick: StateFlow<Long> = _tick.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                _tick.value = System.currentTimeMillis()
            }
        }
    }

    fun selectProject(projectId: String) = repository.selectProject(projectId)

    fun startSession() {
        val projectId = repository.selectedProject()?.id ?: return
        repository.startSession(projectId)
    }

    fun stopSession(notes: String = "") = repository.stopSession(notes)

    fun deleteSession(sessionId: String) =
        performWithUndo(UndoKind.Session) { repository.deleteSession(sessionId) }

    fun addProject(
        name: String,
        clientName: String,
        hourlyRate: Double,
        colorArgb: Long,
        billable: Boolean,
    ) = repository.addProject(name, clientName, hourlyRate, colorArgb, billable)

    fun updateProject(project: Project) = repository.updateProject(project)

    fun deleteProject(projectId: String, deleteSessions: Boolean = false) =
        performWithUndo(
            if (deleteSessions) UndoKind.ProjectAndEntries else UndoKind.Project,
        ) {
            repository.deleteProject(projectId, deleteSessions)
        }

    fun sessionEarnings(session: WorkSession) = repository.sessionEarnings(session)

    fun sessionProjectName(session: WorkSession) = repository.sessionProjectName(session)

    fun sessionClientName(session: WorkSession) = repository.sessionClientName(session)

    fun sessionRate(session: WorkSession) = repository.sessionRate(session)

    fun sessionColor(session: WorkSession) = repository.sessionColor(session)

    fun uniqueClientNames(): List<String> = repository.uniqueClientNames()

    fun addClient(name: String) = repository.addClient(name)

    fun updateClient(client: Client) = repository.updateClient(client)

    fun deleteClient(clientId: String) =
        performWithUndo(UndoKind.Client) { repository.deleteClient(clientId) }

    fun undoLastDelete() {
        val snap = undoSnapshot ?: return
        undoJob?.cancel()
        undoJob = null
        undoSnapshot = null
        _undoBanner.value = null
        repository.restoreState(snap)
    }

    private fun performWithUndo(kind: UndoKind, action: () -> Unit) {
        undoJob?.cancel()
        undoSnapshot = repository.snapshot()
        action()
        undoJob = viewModelScope.launch {
            var left = UNDO_MS
            _undoBanner.value = UndoBannerState(kind, left)
            while (left > 0L) {
                delay(50)
                left -= 50
                _undoBanner.value = UndoBannerState(kind, left.coerceAtLeast(0L))
            }
            _undoBanner.value = null
            undoSnapshot = null
            undoJob = null
        }
    }

    fun updateUserProfile(displayName: String, companyName: String) =
        repository.updateUserProfile(displayName, companyName)

    fun setUserLogoFromUri(uri: Uri): Boolean = repository.setUserLogoFromUri(uri)

    fun clearUserLogo() = repository.clearUserLogo()

    fun buildExportSummary(context: Context, filter: ExportFilter): ExportSummary {
        val filtered = ExportBuilder.filterSessions(state.value.sessions, repository, filter)
        val rows = ExportBuilder.buildRows(filtered, repository)
        return ExportBuilder.buildSummary(context, rows, filter, repository)
    }

    /**
     * Builds the export file on a background thread and returns it (or null if empty).
     */
    suspend fun createExportFile(context: Context, filter: ExportFilter): File? =
        withContext(Dispatchers.IO) {
            val summary = buildExportSummary(context, filter)
            if (summary.rows.isEmpty()) return@withContext null
            val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
            val name = ExportShare.fileNameFor(filter.format, stamp, summary.title)
            val out = File(ExportShare.exportDir(context), name)
            when (filter.format) {
                ExportFormat.CSV -> CsvExporter.export(context, summary, out)
                ExportFormat.XLSX -> XlsxExporter.export(context, summary, out)
                ExportFormat.PDF_TIMESHEET -> PdfExporter.exportTimesheet(context, summary, out)
                ExportFormat.PDF_MONTHLY -> PdfExporter.exportMonthlyReport(context, summary, out)
            }
        }

    fun todayRange() = repository.todayRange()
    fun weekRange() = repository.weekRange()
    fun monthRange() = repository.monthRange()

    fun totalMinutes(sessions: List<WorkSession>, from: Long, to: Long) =
        repository.totalMinutes(sessions, from, to)

    fun totalEarnings(sessions: List<WorkSession>, from: Long, to: Long) =
        repository.totalEarnings(sessions, from, to)

    fun projectById(id: String) = repository.projectById(id)
}
