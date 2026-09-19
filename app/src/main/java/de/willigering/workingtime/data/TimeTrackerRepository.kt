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

    init {
        load()
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

    fun stopSession(notes: String = "") {
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
            end = System.currentTimeMillis(),
            notes = notes.trim(),
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
        val trimmed = project.copy(clientName = project.clientName.trim())
        ensureClient(trimmed.clientName)
        _state.update {
            it.copy(projects = it.projects.map { p -> if (p.id == trimmed.id) trimmed else p })
        }
        saveProjects()
    }

    fun addClient(name: String): Client? = ensureClient(name)

    fun updateClient(client: Client) {
        val newName = client.name.trim()
        if (newName.isEmpty()) return
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
        _state.update { it.copy(clients = it.clients.filter { c -> c.id != clientId }) }
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

    fun restoreState(snapshot: AppState) {
        _state.value = snapshot
        saveProjects()
        saveSessions()
        saveActive()
        saveClients()
    }

    fun deleteProject(projectId: String, deleteSessions: Boolean = false) {
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
        val hours = (session.end - session.start) / 3_600_000.0
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
            total += (end - start) / 60_000
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
            val hours = (end - start) / 3_600_000.0
            total += hours * rate
        }
        return total
    }

    fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        return start to start + 86_400_000
    }

    fun weekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis to System.currentTimeMillis()
    }

    fun monthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis to System.currentTimeMillis()
    }

    fun uniqueClientNames(): List<String> {
        val fromClients = _state.value.clients.map { it.name.trim() }
        val fromProjects = _state.value.projects.map { it.clientName.trim() }
        val fromSessions = _state.value.sessions.map { sessionClientName(it).trim() }
        return (fromClients + fromProjects + fromSessions)
            .filter { it.isNotEmpty() }
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
    fun setUserLogoFromUri(uri: android.net.Uri): Boolean {
        return try {
            val resolver = appContext.contentResolver
            val input = resolver.openInputStream(uri) ?: return false
            val decodeOpts = android.graphics.BitmapFactory.Options().apply {
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                inScaled = false
            }
            val original = android.graphics.BitmapFactory.decodeStream(input, null, decodeOpts)
            input.close()
            if (original == null) return false

            // Keep enough pixels for sharp PDF rendering (~300 dpi at ~1" logo)
            val maxSide = 2048
            val w = original.width
            val h = original.height
            val scale = minOf(1f, maxSide.toFloat() / maxOf(w, h))
            val tw = (w * scale).toInt().coerceAtLeast(1)
            val th = (h * scale).toInt().coerceAtLeast(1)
            val scaled = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(original, tw, th, true)
            } else {
                original
            }
            if (scaled !== original) original.recycle()

            val outFile = File(filesDir, LOGO_FILE_NAME)
            java.io.FileOutputStream(outFile).use { fos ->
                scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos)
            }
            scaled.recycle()

            _state.update {
                it.copy(userProfile = it.userProfile.copy(logoPath = outFile.absolutePath))
            }
            saveProfile()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearUserLogo() {
        val path = _state.value.userProfile.logoPath
        if (path.isNotBlank()) {
            try {
                File(path).delete()
            } catch (_: Exception) {
            }
        }
        val defaultLogo = File(filesDir, LOGO_FILE_NAME)
        if (defaultLogo.exists()) defaultLogo.delete()
        _state.update {
            it.copy(userProfile = it.userProfile.copy(logoPath = ""))
        }
        saveProfile()
    }

    fun logoFile(): File? {
        val path = _state.value.userProfile.logoPath
        if (path.isBlank()) return null
        val f = File(path)
        return if (f.exists()) f else null
    }

    private fun stampSessionFromProject(session: WorkSession, project: Project?): WorkSession {
        if (project == null) {
            // Keep whatever snapshot already exists.
            return session
        }
        // Always refresh snapshot from live project when project is deleted but entries stay.
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
        val active = loadActive()
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
        )
        if (clients.size != storedClients.size) {
            saveClients()
        }
    }

    private fun loadProfile(): UserProfile {
        val file = File(filesDir, "profile.json")
        val logoDefault = File(filesDir, LOGO_FILE_NAME)
        if (!file.exists()) {
            return UserProfile(
                logoPath = if (logoDefault.exists()) logoDefault.absolutePath else "",
            )
        }
        return try {
            val o = JSONObject(file.readText())
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
            UserProfile(
                logoPath = if (logoDefault.exists()) logoDefault.absolutePath else "",
            )
        }
    }

    private fun saveProfile() {
        val p = _state.value.userProfile
        File(filesDir, "profile.json").writeText(
            JSONObject()
                .put("displayName", p.displayName)
                .put("companyName", p.companyName)
                .put("logoPath", p.logoPath)
                .toString(),
        )
    }

    private fun loadProjects(): List<Project> {
        val file = File(filesDir, "projects.json")
        if (!file.exists()) {
            // Start empty — user creates the first project on the homescreen.
            return emptyList()
        }
        return try {
            val arr = JSONArray(file.readText())
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
            emptyList()
        }
    }

    private fun loadSessions(projects: List<Project>): List<WorkSession> {
        val file = File(filesDir, "sessions.json")
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
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
            emptyList()
        }
    }

    private fun loadActive(): ActiveSession? {
        val jsonFile = File(filesDir, "active.json")
        if (jsonFile.exists()) {
            return try {
                val o = JSONObject(jsonFile.readText())
                ActiveSession(o.getLong("start"), o.getString("projectId"))
            } catch (_: Exception) {
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
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val name = o.optString("name", "").trim()
                if (name.isEmpty()) null
                else Client(
                    id = o.optString("id", java.util.UUID.randomUUID().toString()),
                    name = name,
                )
            }
        } catch (_: Exception) {
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
        val arr = JSONArray()
        for (c in _state.value.clients) {
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name),
            )
        }
        File(filesDir, "clients.json").writeText(arr.toString())
    }

    private fun saveProjects() {
        File(filesDir, "projects.json").writeText(projectsToJson(_state.value.projects).toString())
    }

    private fun saveSessions() {
        val arr = JSONArray()
        for (s in _state.value.sessions) {
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("projectId", s.projectId)
                    .put("projectName", s.projectName)
                    .put("clientName", s.clientName)
                    .put("hourlyRate", s.hourlyRate)
                    .put("colorArgb", s.colorArgb)
                    .put("billable", s.billable)
                    .put("start", s.start)
                    .put("end", s.end)
                    .put("notes", s.notes)
            )
        }
        File(filesDir, "sessions.json").writeText(arr.toString())
    }

    private fun saveActive() {
        val file = File(filesDir, "active.json")
        val active = _state.value.activeSession
        if (active == null) {
            file.writeText("")
        } else {
            file.writeText(
                JSONObject()
                    .put("start", active.start)
                    .put("projectId", active.projectId)
                    .toString()
            )
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
