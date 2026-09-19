package de.willigering.workingtime.data

import java.util.UUID

data class Client(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
)

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val clientName: String = "",
    val hourlyRate: Double = 0.0,
    val colorArgb: Long = 0xFFFFB300,
    val billable: Boolean = true,
)

data class WorkSession(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    /** Snapshot so history survives project deletion. */
    val projectName: String = "",
    val clientName: String = "",
    val hourlyRate: Double = 0.0,
    val colorArgb: Long = 0xFFFFB300,
    val billable: Boolean = true,
    val start: Long,
    val end: Long,
    val notes: String = "",
)

data class ActiveSession(
    val start: Long,
    val projectId: String,
)

/** User self-presentation for exports (name, optional company, logo). */
data class UserProfile(
    val displayName: String = "",
    val companyName: String = "",
    /** Absolute path to logo file in app storage, or empty. */
    val logoPath: String = "",
) {
    val hasLogo: Boolean get() = logoPath.isNotBlank() && java.io.File(logoPath).exists()

    fun issuerTitle(): String = when {
        displayName.isNotBlank() -> displayName.trim()
        companyName.isNotBlank() -> companyName.trim()
        else -> ""
    }
}

data class AppState(
    val projects: List<Project> = emptyList(),
    val clients: List<Client> = emptyList(),
    val sessions: List<WorkSession> = emptyList(),
    val activeSession: ActiveSession? = null,
    val selectedProjectId: String? = null,
    val userProfile: UserProfile = UserProfile(),
)