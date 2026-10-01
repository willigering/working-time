package de.willigering.workingtime.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.willigering.workingtime.R
import de.willigering.workingtime.ui.components.SwipeUndoBanner
import de.willigering.workingtime.ui.screens.ClockScreen
import de.willigering.workingtime.ui.screens.EntryEditorScreen
import de.willigering.workingtime.ui.screens.ExportDialog
import de.willigering.workingtime.ui.screens.HeuteScreen
import de.willigering.workingtime.ui.screens.ProjectDetailScreen
import de.willigering.workingtime.ui.screens.ProjectsScreen
import de.willigering.workingtime.ui.screens.SessionsScreen
import de.willigering.workingtime.ui.screens.SettingsHome
import de.willigering.workingtime.ui.screens.SettingsSubpage
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.launch

private data class Tab(val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(R.string.tab_today, Icons.Rounded.Home),
    Tab(R.string.tab_times, Icons.Rounded.Schedule),
    Tab(R.string.tab_projects, Icons.Rounded.Folder),
    Tab(R.string.tab_settings, Icons.Rounded.Settings),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkingTimeApp(viewModel: TimeTrackerViewModel = viewModel()) {
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val undo by viewModel.undoBanner.collectAsStateWithLifecycle()
    val appState by viewModel.state.collectAsStateWithLifecycle()

    var clock by rememberSaveable { mutableStateOf(false) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editorSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsPage by rememberSaveable { mutableStateOf<String?>(null) }
    var timesProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var showExport by rememberSaveable { mutableStateOf(false) }
    var pendingPage by remember { mutableIntStateOf(-1) }

    val overlay = clock || detailId != null || editorOpen || settingsPage != null
    LaunchedEffect(pendingPage, overlay) {
        if (!overlay && pendingPage >= 0) {
            pagerState.scrollToPage(pendingPage)
            pendingPage = -1
        }
    }

    fun openTimes(projectId: String?) {
        timesProjectId = projectId
        detailId = null
        settingsPage = null
        pendingPage = 1
    }

    Box(Modifier.fillMaxSize().background(AppColors.Background)) {
        when {
            clock -> ClockScreen(viewModel) { clock = false }
            editorOpen -> {
                val session = editorSessionId?.let { id -> appState.sessions.find { it.id == id } }
                LaunchedEffect(editorSessionId, session?.id) {
                    if (editorSessionId != null && session == null) editorOpen = false
                }
                if (session != null || editorSessionId == null) {
                    EntryEditorScreen(
                        session = session,
                        projects = appState.projects,
                        onBack = { editorOpen = false },
                        onSave = {
                            viewModel.saveSession(it)
                            editorOpen = false
                        },
                        onDelete = session?.let {
                            {
                                viewModel.deleteSession(it.id)
                                editorOpen = false
                            }
                        },
                    )
                }
            }
            detailId != null -> ProjectDetailScreen(
                projectId = detailId!!,
                viewModel = viewModel,
                onBack = { detailId = null },
                onOpenTimes = { openTimes(it) },
            )
            settingsPage != null -> SettingsSubpage(
                page = settingsPage!!,
                viewModel = viewModel,
                onBack = { settingsPage = null },
            )
            else -> Scaffold(
                containerColor = AppColors.Background,
                bottomBar = {
                    Column(Modifier.background(AppColors.Background).navigationBarsPadding()) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.GlassBorder))
                        Row(Modifier.fillMaxWidth().height(62.dp)) {
                            tabs.forEachIndexed { index, tab ->
                                val selected = pagerState.currentPage == index
                                val label = stringResource(tab.labelRes)
                                val tint = if (selected) AppColors.Accent else AppColors.TextMuted
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clickable {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                                ) {
                                    Icon(tab.icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
                                    Text(
                                        label,
                                        color = tint,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                },
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(AppColors.Background)
                        .padding(padding),
                ) {
                    if (appState.storageError) {
                        Text(
                            stringResource(R.string.storage_failed),
                            color = AppColors.Error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        )
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondBoundsPageCount = 1,
                        userScrollEnabled = undo == null,
                    ) { page ->
                        when (page) {
                            0 -> HeuteScreen(
                                viewModel = viewModel,
                                visible = pagerState.currentPage == 0,
                                onOpenSettings = { scope.launch { pagerState.animateScrollToPage(3) } },
                                onOpenAllTimes = { openTimes(null) },
                                onEditSession = {
                                    editorSessionId = it.id
                                    editorOpen = true
                                },
                                onNewEntry = {
                                    editorSessionId = null
                                    editorOpen = true
                                },
                                onOpenClock = { clock = true },
                            )
                            1 -> SessionsScreen(
                                viewModel = viewModel,
                                projectFilter = timesProjectId,
                                onProjectFilter = { timesProjectId = it },
                                onEditSession = {
                                    editorSessionId = it.id
                                    editorOpen = true
                                },
                            )
                            2 -> ProjectsScreen(viewModel) { detailId = it }
                            else -> SettingsHome(
                                onOpen = { settingsPage = it },
                                onExport = { showExport = true },
                            )
                        }
                    }
                }
            }
        }

        undo?.let { banner ->
            SwipeUndoBanner(
                message = stringResource(R.string.delete_undo_action),
                remainingMs = banner.remainingMs,
                onUndo = { viewModel.undoLastDelete() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
                    .padding(start = 16.dp, end = 16.dp, bottom = 78.dp),
            )
        }
    }

    if (showExport) {
        ExportDialog(viewModel = viewModel, onDismiss = { showExport = false })
    }
}
