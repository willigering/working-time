package de.willigering.workingtime.data

import android.content.Context
import de.willigering.workingtime.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.Locale
import de.willigering.workingtime.util.TimeMath
import android.util.AtomicFile
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class TimeTrackerRepository(context: Context) {

    private val appContext = context.applicationContext
    private val filesDir = context.filesDir
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    companion object {
        const val DEFAULT_PROJECT_ID = "default"
        const val LOGO_FILE_NAME = "user_logo.png"
        val PROJECT_COLORS = listOf(
            0xFFFFB300, 0xFFFF6D00, 0xFF00BFFF, 0xFF7B2FFF,
            0xFF00E676, 0xFFFF5252, 0xFFE040FB, 0xFF26C6DA,
        )
    }

    private val failedFiles = mutableSetOf<String>()
    private val writes = Channel<() -> Unit>(Channel.UNLIMITED)
    private val diskScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        load()
        diskScope.launch {
            for (write in writes) {
                try { write() } catch (_: Exception) {
                    _state.update { it.copy(storageError = true) }
                }
            }
        }
    }

    suspend fun awaitWrites() {
        val done = CompletableDeferred<Unit>()
        writes.send { done.complete(Unit) }
        done.await()
    }

    private fun readStored(file: File): String =
        AtomicFile(file).openRead().bufferedReader().use { it.readText() }

    private fun writeStored(file: File, text: String) {
        if (file.name in failedFiles) return
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try {
            output.write(text.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(output)
        } catch (e: Exception) {
            atomic.failWrite(output)
            throw e
        }
    }

    private fun enqueue(write: () -> Unit) {
        if (writes.trySend(write).isFailure) _state.update { it.copy(storageError = true) }
    }

    fun updateActiveNotes(notes: String) {
        _state.update { it.copy(activeSession = it.activeSession?.copy(notes = notes)) }
        saveActive()
    }

    fun saveSession(session: WorkSession) {
        require(session.end > session.start)
        require(session.hourlyRate.isFinite() && session.hourlyRate >= 0)
        _state.update { state ->
            state.copy(sessions = (state.sessions.filterNot { it.id == session.id } + session)
                .sortedByDescending { it.start })
        }
        saveSessions()
    }


    fun projectById(id: String): Project? =
        _state.value.projects.find { it.id == id }

    fun selectedProject(): Project? {
        val state = _state.value
        val id = state.selectedProjectId
            ?: state.activeSession?.projectId
        // Do not auto-pick the first project when nothing is selected —
        // the homescreen shows "create your first project" instead.
        return id?.let { projectById(it) }
    }

    fun selectProject(projectId: String) {
        _state.update { it.copy(selectedProjectId = projectId) }
    }

    fun startSession(projectId: String) {
        if (projectById(projectId) == null) return
        if (_state.value.activeSession != null) return
        _state.update {
            it.copy(
                activeSession = ActiveSession(
                    start = System.currentTimeMillis(),
                    projectId = projectId,
                ),
                selectedProjectId = projectId,
            )
        }
        saveActive()
    }

    fun stopSession(notes: String? = null) {
        val active = _state.value.activeSession ?: return
        val project = projectById(active.projectId)
        val session = WorkSession(
            projectId = active.projectId,
            projectName = project?.name.orEmpty(),
            clientName = project?.clientName.orEmpty(),
            hourlyRate = project?.hourlyRate ?: 0.0,
            colorArgb = project?.colorArgb ?: PROJECT_COLORS[0],
            billable = project?.billable ?: true,
            start = active.start,
            end = maxOf(active.start, System.currentTimeMillis()),
            notes = (notes ?: active.notes).trim(),
        )
        _state.update {
            it.copy(
                sessions = listOf(session) + it.sessions,
                activeSession = null,
            )
        }
        saveSessions()
        saveActive()
    }

    fun deleteSession(sessionId: String) {
        _state.update { it.copy(sessions = it.sessions.filter { s -> s.id != sessionId }) }
        saveSessions()
    }

    fun addProject(
        name: String,
        clientName: String,
        hourlyRate: Double,
        colorArgb: Long,
        billable: Boolean,
    ) {
        require(name.isNotBlank() && hourlyRate.isFinite() && hourlyRate >= 0.0)
        val trimmedClient = clientName.trim()
        ensureClient(trimmedClient)
        val project = Project(
            name = name.trim(),
            clientName = trimmedClient,
            hourlyRate = hourlyRate.coerceAtLeast(0.0),
            colorArgb = colorArgb,
            billable = billable,
        )
        _state.update { it.copy(projects = it.projects + project) }
        if (_state.value.selectedProjectId == null) {
            selectProject(project.id)
        }
        saveProjects()
    }

    fun updateProject(project: Project) {
        require(project.name.isNotBlank() && project.hourlyRate.isFinite() && project.hourlyRate >= 0.0)
        val trimmed = project.copy(clientName = project.clientName.trim())
        ensureClient(trimmed.clientName)
        _state.update {
            it.copy(projects = it.projects.map { p -> if (p.id == trimmed.id) trimmed else p })
        }
        saveProjects()
    }

    fun addClient(name: String): Client? {
        val client = ensureClient(name) ?: return null
        _state.update { state -> state.copy(clients = state.clients.map {
            if (it.id == client.id) it.copy(archived = false) else it
        }) }
        saveClients()
        return client.copy(archived = false)
    }

    fun updateClient(client: Client) {
        val newName = client.name.trim()
        if (newName.isEmpty()) return
        if (_state.value.clients.any { it.id != client.id && it.name.equals(newName, true) }) return
        val old = _state.value.clients.find { it.id == client.id } ?: run {
            ensureClient(newName)
            return
        }
        if (old.name == newName) return
        _state.update { state ->
            state.copy(
                clients = state.clients
                    .map { if (it.id == client.id) it.copy(name = newName) else it }
                    .sortedBy { it.name.lowercase(Locale.getDefault()) },
                projects = state.projects.map { p ->
                    if (p.clientName.equals(old.name, ignoreCase = true)) {
                        p.copy(clientName = newName)
                    } else {
                        p
                    }
                },
            )
        }
        saveClients()
        saveProjects()
    }

    fun deleteClient(clientId: String) {
        _state.update { it.copy(clients = it.clients.map { c -> if (c.id == clientId) c.copy(archived = true) else c }) }
        saveClients()
    }

    /** Inserts the name into the client list if it is new. Returns the stored client. */
    fun ensureClient(name: String): Client? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val existing = _state.value.clients.find {
            it.name.equals(trimmed, ignoreCase = true)
        }
        if (existing != null) return existing
        val client = Client(name = trimmed)
        _state.update { state ->
            state.copy(
                clients = (state.clients + client)
                    .sortedBy { it.name.lowercase(Locale.getDefault()) },
            )
        }
        saveClients()
        return client
    }

    /**
     * @param deleteSessions when true, remove all history entries for this project;
     * otherwise keep entries and stamp project details onto them so they still display.
     */
    fun snapshot(): AppState = _state.value

    /** Restore only deleted records; preserve all intervening edits and timer changes. */
    fun restoreDeleted(before: AppState, after: AppState) {
        val projects = before.projects.filter { old -> after.projects.none { it.id == old.id } }
        val sessions = before.sessions.filter { old -> after.sessions.none { it.id == old.id } }
        val clients = before.clients.filter { old ->
            !old.archived && after.clients.any { it.id == old.id && it.archived }
        }
        _state.update { now ->
            now.copy(
                projects = now.projects + projects.filter { p -> now.projects.none { it.id == p.id } },
                sessions = (now.sessions + sessions.filter { s -> now.sessions.none { it.id == s.id } })
                    .sortedByDescending { it.start },
                clients = now.clients.map { current ->
                    clients.find { it.id == current.id }?.let { current.copy(archived = false) } ?: current
                },
                selectedProjectId = now.selectedProjectId ?: projects.firstOrNull()?.id,
                activeSession = now.activeSession ?: before.activeSession?.takeIf {
                    after.activeSession == null && now.sessions.none { s ->
                        s.projectId == it.projectId && s.start == it.start
                    }
                },
            )
        }
        saveProjects(); saveSessions(); saveActive(); saveClients()
    }

    fun deleteProject(projectId: String, deleteSessions: Boolean = false) {
        if (!deleteSessions && _state.value.activeSession?.projectId == projectId) stopSession()
        val project = projectById(projectId)
        val remaining = _state.value.projects.filter { it.id != projectId }
        val fallbackId = remaining.firstOrNull()?.id
        _state.update { state ->
            val sessions = if (deleteSessions) {
                state.sessions.filter { it.projectId != projectId }
            } else {
                state.sessions.map { s ->
                    if (s.projectId != projectId) s
                    else stampSessionFromProject(s, project)
                }
            }
            state.copy(
                projects = remaining,
                sessions = sessions,
                selectedProjectId = when {
                    state.selectedProjectId == projectId -> fallbackId
                    state.selectedProjectId != null &&
                        remaining.none { p -> p.id == state.selectedProjectId } -> fallbackId
                    else -> state.selectedProjectId
                },
                activeSession = state.activeSession?.takeUnless {
                    deleteSessions && it.projectId == projectId
                },
            )
        }
        saveProjects()
        saveSessions()
        saveActive()
    }

    fun sessionEarnings(session: WorkSession): Double {
        val rate = sessionRate(session)
        val billable = sessionBillable(session)
        if (!billable || rate <= 0) return 0.0
        val hours = TimeMath.minutes(session.start, session.end) / 60.0
        return hours * rate
    }

    fun sessionProjectName(session: WorkSession): String {
        if (session.projectName.isNotBlank()) return session.projectName
        return projectById(session.projectId)?.name
            ?: appContext.getString(R.string.unknown_project)
    }

    fun sessionClientName(session: WorkSession): String {
        if (session.clientName.isNotBlank() || session.projectName.isNotBlank()) {
            return session.clientName
        }
        return projectById(session.projectId)?.clientName.orEmpty()
    }

    fun sessionRate(session: WorkSession): Double {
        if (session.projectName.isNotBlank() || session.hourlyRate > 0) {
            return session.hourlyRate
        }
        return projectById(session.projectId)?.hourlyRate ?: 0.0
    }

    fun sessionBillable(session: WorkSession): Boolean {
        if (session.projectName.isNotBlank() || session.hourlyRate > 0) {
            return session.billable
        }
        return projectById(session.projectId)?.billable ?: session.billable
    }

    fun sessionColor(session: WorkSession): Long {
        if (session.projectName.isNotBlank() || session.colorArgb != 0L) {
            // Prefer live project color while it still exists.
            return projectById(session.projectId)?.colorArgb ?: session.colorArgb
        }
        return projectById(session.projectId)?.colorArgb ?: PROJECT_COLORS[0]
    }

    fun totalMinutes(sessions: List<WorkSession>, fromMillis: Long, toMillis: Long): Long {
        var total = 0L
        for (s in sessions) {
            if (s.end < fromMillis || s.start > toMillis) continue
            val start = maxOf(s.start, fromMillis)
            val end = minOf(s.end, toMillis)
            total += TimeMath.minutes(start, end)
        }
        return total
    }

    fun totalEarnings(sessions: List<WorkSession>, fromMillis: Long, toMillis: Long): Double {
        var total = 0.0
        for (s in sessions) {
            if (s.end < fromMillis || s.start > toMillis) continue
            val rate = sessionRate(s)
            if (!sessionBillable(s) || rate <= 0) continue
            val start = maxOf(s.start, fromMillis)
            val end = minOf(s.end, toMillis)
            val hours = TimeMath.minutes(start, end) / 60.0
            total += hours * rate
        }
        return total
    }

    fun todayRange(): Pair<Long, Long> = TimeMath.dayRange()

    fun weekRange(): Pair<Long, Long> = TimeMath.weekRange()

    fun monthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis to System.currentTimeMillis()
    }

    fun uniqueClientNames(includeArchived: Boolean = false): List<String> {
        val fromClients = _state.value.clients.filter { includeArchived || !it.archived }.map { it.name.trim() }
        val archived = _state.value.clients.filter { it.archived }.map { it.name.lowercase(Locale.ROOT) }.toSet()
        val fromProjects = _state.value.projects.map { it.clientName.trim() }
        val fromSessions = _state.value.sessions.map { sessionClientName(it).trim() }
        return (fromClients + fromProjects + fromSessions)
            .filter { it.isNotEmpty() && (includeArchived || it.lowercase(Locale.ROOT) !in archived) }
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedBy { it.lowercase(Locale.getDefault()) }
    }

    fun updateUserProfile(displayName: String, companyName: String) {
        _state.update {
            it.copy(
                userProfile = it.userProfile.copy(
                    displayName = displayName.trim(),
                    companyName = companyName.trim(),
                ),
            )
        }
        saveProfile()
    }

    /**
     * Copies/resizes the picked image into app storage as PNG and stores the path.
     * @return true if logo was saved successfully
     */
    fun setUserLogoFromUri(uri: android.net.Uri, turns: Int = 0, crop: Boolean = false): Boolean {
        val target = File(filesDir, "logo-" + java.util.UUID.randomUUID() + ".png")
        return try {
            val bitmap = de.willigering.workingtime.util.LogoImages.decode(appContext, uri, 2048, turns, crop)
                ?: return false
            try {
                java.io.FileOutputStream(target).use {
                    check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
                    it.fd.sync()
                }
            } finally { bitmap.recycle() }
            // Unique files keep the previous logo safe and avoid export/import races.
            _state.update { it.copy(userProfile = it.userProfile.copy(logoPath = target.absolutePath)) }
            saveProfile()
            true
        } catch (_: Exception) {
            target.delete()
            false
        } catch (_: OutOfMemoryError) {
            target.delete()
            false
        }
    }

    fun clearUserLogo() {
        // Keep immutable image files for exports already in progress.
        _state.update { it.copy(userProfile = it.userProfile.copy(logoPath = "")) }
        saveProfile()
    }

    fun logoFile(): File? {
        val path = _state.value.userProfile.logoPath
        if (path.isBlank()) return null
        val f = File(path)
        return if (f.exists()) f else null
    }

    private fun stampSessionFromProject(session: WorkSession, project: Project?): WorkSession {
        if (session.projectName.isNotBlank() || project == null) {
            // Keep whatever snapshot already exists.
            return session
        }
        // Stamp only legacy records without a snapshot.
        return session.copy(
            projectName = project.name,
            clientName = project.clientName,
            hourlyRate = project.hourlyRate,
            colorArgb = project.colorArgb,
            billable = project.billable,
        )
    }

    private fun load() {
        val projects = loadProjects()
        val sessions = loadSessions(projects)
        val storedClients = loadClients()
        val clients = mergeClients(storedClients, projects, sessions)
        val active = loadActive()?.takeUnless { a -> sessions.any { it.projectId == a.projectId && it.start == a.start } }
        val profile = loadProfile()
        val activeProjectId = active?.projectId?.takeIf { id -> projects.any { it.id == id } }
        val selected = activeProjectId
            ?: projects.firstOrNull()?.id
        _state.value = AppState(
            projects = projects,
            clients = clients,
            sessions = sessions,
            activeSession = active,
            selectedProjectId = selected,
            userProfile = profile,
            storageError = failedFiles.isNotEmpty(),
        )
        if (clients.size != storedClients.size) {
            saveClients()
        }
    }

    private fun loadProfile(): UserProfile {
        val file = File(filesDir, "profile.json")
        val logoDefault = File(filesDir, LOGO_FILE_NAME)
        if (!file.exists() && !File(file.path + ".bak").exists()) {
            return UserProfile(
                logoPath = if (logoDefault.exists()) logoDefault.absolutePath else "",
            )
        }
        return try {
            val o = JSONObject(readStored(file))
            val path = o.optString("logoPath", "")
            val logoPath = when {
                path.isNotBlank() && File(path).exists() -> path
                logoDefault.exists() -> logoDefault.absolutePath
                else -> ""
            }
            UserProfile(
                displayName = o.optString("displayName", ""),
                companyName = o.optString("companyName", ""),
                logoPath = logoPath,
            )
        } catch (_: Exception) {
            failedFiles += file.name
            UserProfile(
                logoPath = if (logoDefault.exists()) logoDefault.absolutePath else "",
            )
        }
    }

    private fun saveProfile() {
        val p = _state.value.userProfile
        enqueue {
            writeStored(File(filesDir, "profile.json"), JSONObject()
                .put("displayName", p.displayName).put("companyName", p.companyName)
                .put("logoPath", p.logoPath).toString())
        }
    }

    private fun loadProjects(): List<Project> {
        val file = File(filesDir, "projects.json")
        if (!file.exists() && !File(file.path + ".bak").exists()) {
            // Start empty — user creates the first project on the homescreen.
            return emptyList()
        }
        return try {
            val arr = JSONArray(readStored(file))
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Project(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    clientName = o.optString("clientName", ""),
                    hourlyRate = o.optDouble("hourlyRate", 0.0),
                    colorArgb = o.optLong("colorArgb", PROJECT_COLORS[0]),
                    billable = o.optBoolean("billable", true),
                )
            }
        } catch (_: Exception) {
            failedFiles += file.name
            emptyList()
        }
    }

    private fun loadSessions(projects: List<Project>): List<WorkSession> {
        val file = File(filesDir, "sessions.json")
        if (!file.exists() && !File(file.path + ".bak").exists()) return emptyList()
        return try {
            val arr = JSONArray(readStored(file))
            val byId = projects.associateBy { it.id }
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val projectId = o.optString("projectId", DEFAULT_PROJECT_ID)
                val project = byId[projectId]
                val hasSnapshot = o.has("projectName")
                WorkSession(
                    id = o.optString("id", java.util.UUID.randomUUID().toString()),
                    projectId = projectId,
                    projectName = if (hasSnapshot) {
                        o.optString("projectName", "")
                    } else {
                        project?.name.orEmpty()
                    },
                    clientName = if (hasSnapshot) {
                        o.optString("clientName", "")
                    } else {
                        project?.clientName.orEmpty()
                    },
                    hourlyRate = if (hasSnapshot) {
                        o.optDouble("hourlyRate", 0.0)
                    } else {
                        project?.hourlyRate ?: 0.0
                    },
                    colorArgb = if (hasSnapshot) {
                        o.optLong("colorArgb", PROJECT_COLORS[0])
                    } else {
                        project?.colorArgb ?: PROJECT_COLORS[0]
                    },
                    billable = if (hasSnapshot) {
                        o.optBoolean("billable", true)
                    } else {
                        project?.billable ?: true
                    },
                    start = o.getLong("start"),
                    end = o.getLong("end"),
                    notes = o.optString("notes", ""),
                )
            }.sortedByDescending { it.start }
        } catch (_: Exception) {
            failedFiles += file.name
            emptyList()
        }
    }

    private fun loadActive(): ActiveSession? {
        val jsonFile = File(filesDir, "active.json")
        if (jsonFile.exists() || File(jsonFile.path + ".bak").exists()) {
            return try {
                val o = JSONObject(readStored(jsonFile))
                ActiveSession(o.getLong("start"), o.getString("projectId"), o.optString("notes", ""))
            } catch (_: Exception) {
                if (jsonFile.length() > 0) failedFiles += jsonFile.name
                null
            }
        }
        val legacy = File(filesDir, "active.txt")
        if (legacy.exists()) {
            return try {
                val start = legacy.readText().trim().toLong()
                if (start > 0) ActiveSession(start, DEFAULT_PROJECT_ID) else null
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    private fun loadClients(): List<Client> {
        val file = File(filesDir, "clients.json")
        if (!file.exists() && !File(file.path + ".bak").exists()) return emptyList()
        return try {
            val arr = JSONArray(readStored(file))
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val name = o.optString("name", "").trim()
                if (name.isEmpty()) null
                else Client(
                    id = o.optString("id", java.util.UUID.randomUUID().toString()),
                    name = name,
                    archived = o.optBoolean("archived", false),
                )
            }
        } catch (_: Exception) {
            failedFiles += file.name
            emptyList()
        }
    }

    private fun mergeClients(
        stored: List<Client>,
        projects: List<Project>,
        sessions: List<WorkSession>,
    ): List<Client> {
        val byLower = linkedMapOf<String, Client>()
        for (client in stored) {
            val key = client.name.trim().lowercase(Locale.getDefault())
            if (key.isNotEmpty()) byLower[key] = client
        }
        val extras = projects.map { it.clientName } + sessions.map { it.clientName }
        for (raw in extras) {
            val name = raw.trim()
            if (name.isEmpty()) continue
            val key = name.lowercase(Locale.getDefault())
            if (!byLower.containsKey(key)) {
                byLower[key] = Client(name = name)
            }
        }
        return byLower.values.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    private fun saveClients() {
        val clients = _state.value.clients
        enqueue {
            val arr = JSONArray()
            for (c in clients) arr.put(JSONObject().put("id", c.id).put("name", c.name).put("archived", c.archived))
            writeStored(File(filesDir, "clients.json"), arr.toString())
        }
    }

    private fun saveProjects() {
        val projects = _state.value.projects
        enqueue { writeStored(File(filesDir, "projects.json"), projectsToJson(projects).toString()) }
    }

    private fun saveSessions() {
        val sessions = _state.value.sessions
        enqueue {
            val arr = JSONArray()
            for (s in sessions) arr.put(JSONObject()
                .put("id", s.id).put("projectId", s.projectId).put("projectName", s.projectName)
                .put("clientName", s.clientName).put("hourlyRate", s.hourlyRate)
                .put("colorArgb", s.colorArgb).put("billable", s.billable)
                .put("start", s.start).put("end", s.end).put("notes", s.notes))
            writeStored(File(filesDir, "sessions.json"), arr.toString())
        }
    }

    private fun saveActive() {
        val active = _state.value.activeSession
        enqueue {
            writeStored(File(filesDir, "active.json"), active?.let {
                JSONObject().put("start", it.start).put("projectId", it.projectId)
                    .put("notes", it.notes).toString()
            }.orEmpty())
        }
    }

    private fun projectsToJson(projects: List<Project>): JSONArray {
        val arr = JSONArray()
        for (p in projects) {
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("clientName", p.clientName)
                    .put("hourlyRate", p.hourlyRate)
                    .put("colorArgb", p.colorArgb)
                    .put("billable", p.billable)
            )
        }
        return arr
    }
}
