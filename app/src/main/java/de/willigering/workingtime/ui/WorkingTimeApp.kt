package de.willigering.workingtime.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.willigering.workingtime.R
import de.willigering.workingtime.ui.components.SwipeUndoBanner
import de.willigering.workingtime.ui.screens.ProfileScreen
import de.willigering.workingtime.ui.screens.ProjectsScreen
import de.willigering.workingtime.ui.screens.SessionsScreen
import de.willigering.workingtime.ui.screens.StatsScreen
import de.willigering.workingtime.ui.screens.TimerScreen
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.launch

private data class Tab(val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(R.string.tab_timer, Icons.Rounded.Timer),
    Tab(R.string.tab_projects, Icons.Rounded.Folder),
    Tab(R.string.tab_sessions, Icons.Rounded.History),
    Tab(R.string.tab_profile, Icons.Rounded.Person),
    Tab(R.string.tab_stats, Icons.Rounded.BarChart),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkingTimeApp(viewModel: TimeTrackerViewModel = viewModel()) {
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val undo by viewModel.undoBanner.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppColors.Background,
        bottomBar = {
            NavigationBar(
                containerColor = AppColors.Background,
                tonalElevation = 0.dp,
            ) {
                for ((index, tab) in tabs.withIndex()) {
                    val selected = pagerState.currentPage == index
                    val label = stringResource(tab.labelRes)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = label,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        label = {
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                letterSpacing = 0.1.sp,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.Accent,
                            selectedTextColor = AppColors.Accent,
                            unselectedIconColor = AppColors.TextMuted,
                            unselectedTextColor = AppColors.TextMuted,
                            indicatorColor = AppColors.Surface,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(padding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = 1,
                userScrollEnabled = undo == null,
            ) { page ->
                when (page) {
                    0 -> TimerScreen(viewModel = viewModel)
                    1 -> ProjectsScreen(viewModel)
                    2 -> SessionsScreen(viewModel)
                    3 -> ProfileScreen(viewModel)
                    4 -> StatsScreen(viewModel)
                }
            }
            undo?.let { banner ->
                SwipeUndoBanner(
                    message = stringResource(R.string.delete_undo_action),
                    remainingMs = banner.remainingMs,
                    onUndo = { viewModel.undoLastDelete() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(1f)
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        }
    }
}
