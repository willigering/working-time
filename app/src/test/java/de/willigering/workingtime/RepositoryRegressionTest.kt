package de.willigering.workingtime

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import de.willigering.workingtime.data.*
import de.willigering.workingtime.export.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryRegressionTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var context: Context
    private lateinit var repo: TimeTrackerRepository

    @Before fun setup() {
        val directory = temporary.newFolder()
        context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getFilesDir(): File = directory
            override fun getApplicationContext(): Context = this
        }
        repo = TimeTrackerRepository(context)
    }

    private fun flush() = runBlocking { withTimeout(10_000) { repo.awaitWrites() } }
    @After fun finishWrites() { flush() }

    private fun project(): Project {
        repo.addProject("Example", "Client", 30.0, 0xFFFFB300, true)
        return repo.state.value.projects.last()
    }
    private fun entry(p: Project, start: Long = 0L, end: Long = 3_600_000L) =
        WorkSession(projectId = p.id, projectName = p.name, clientName = p.clientName,
            hourlyRate = p.hourlyRate, billable = p.billable, start = start, end = end)

    @Test fun deletingEditedProjectPreservesHistoricalBilling() {
        val p = project()
        repo.saveSession(entry(p))
        repo.updateProject(p.copy(hourlyRate = 50.0, billable = false))
        repo.deleteProject(p.id)
        val session = repo.state.value.sessions.single()
        assertEquals(30.0, repo.sessionEarnings(session), 0.0)
        assertTrue(session.billable)
        flush()
        val reloaded = TimeTrackerRepository(context)
        assertEquals(30.0, reloaded.sessionEarnings(reloaded.state.value.sessions.single()), 0.0)
    }

    @Test fun undoDoesNotOverwriteNewTimerOrProfileEdits() {
        val p = project()
        val s = entry(p)
        repo.saveSession(s)
        val before = repo.snapshot()
        repo.deleteSession(s.id)
        val after = repo.snapshot()
        repo.startSession(p.id)
        repo.updateUserProfile("New name", "New company")
        repo.restoreDeleted(before, after)
        assertNotNull(repo.state.value.activeSession)
        assertEquals("New name", repo.state.value.userProfile.displayName)
        assertEquals(s.id, repo.state.value.sessions.single().id)
    }

    @Test fun archivedClientsStayHiddenAfterReloadButRemainExportable() {
        project()
        val client = repo.state.value.clients.single()
        repo.deleteClient(client.id)
        flush()
        val reloaded = TimeTrackerRepository(context)
        assertTrue(reloaded.state.value.clients.single().archived)
        assertFalse("Client" in reloaded.uniqueClientNames())
        assertTrue("Client" in reloaded.uniqueClientNames(includeArchived = true))
    }

    @Test fun activeNotesSurviveReload() {
        val p = project()
        repo.startSession(p.id)
        repo.updateActiveNotes("Keep this note")
        flush()
        assertEquals("Keep this note", TimeTrackerRepository(context).state.value.activeSession?.notes)
    }

    @Test fun exportClipsToPeriodAndMatchesStatistics() {
        val p = project()
        val session = entry(p, 0, 7_200_000)
        val filter = ExportFilter(scope = ExportScope.PERIOD, periodFrom = 3_600_000, periodTo = 7_199_999)
        val selected = ExportBuilder.filterSessions(listOf(session), repo, filter)
        val rows = ExportBuilder.buildRows(selected, repo)
        assertEquals(60L, rows.single().workMinutes)
        assertEquals(30.0, rows.single().earnings, 0.0)
        assertEquals(repo.totalMinutes(listOf(session), 3_600_000, 7_200_000), rows.single().workMinutes)
        assertEquals(repo.totalEarnings(listOf(session), 3_600_000, 7_200_000), rows.single().earnings, 0.0)
    }

    @Test fun subMinuteEntryHasZeroEarnings() {
        assertEquals(0.0, repo.sessionEarnings(entry(project(), 0, 59_000)), 0.0)
    }

    @Test fun corruptHistoryIsReportedAndNotOverwritten() {
        val file = File(context.filesDir, "sessions.json")
        file.writeText("broken history")
        repo = TimeTrackerRepository(context)
        assertTrue(repo.state.value.storageError)
        repo.saveSession(entry(project()))
        flush()
        assertEquals("broken history", file.readText())
    }

    @Test fun failedLogoImportKeepsPreviousImageAndSquareCropWorks() {
        val image = File(context.filesDir, "input.png")
        val bitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        assertTrue(repo.setUserLogoFromUri(Uri.fromFile(image), turns = 1, crop = true))
        val oldPath = repo.state.value.userProfile.logoPath
        val saved = android.graphics.BitmapFactory.decodeFile(oldPath)
        assertEquals(200, saved.width)
        assertEquals(200, saved.height)
        saved.recycle()
        assertFalse(repo.setUserLogoFromUri(Uri.fromFile(File(context.filesDir, "missing.png"))))
        assertEquals(oldPath, repo.state.value.userProfile.logoPath)
        assertTrue(File(oldPath).exists())
    }
}
