package de.willigering.workingtime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.willigering.workingtime.R
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters
import de.willigering.workingtime.viewmodel.TimeTrackerViewModel

@Composable
fun ClockScreen(
    viewModel: TimeTrackerViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tick by viewModel.tick.collectAsStateWithLifecycle()
    val active = state.activeSession
    BackHandler(onBack = onClose)
    if (active == null) {
        Box(Modifier.fillMaxSize().background(AppColors.Background))
        return
    }
    val project = viewModel.projectById(active.projectId)
    val elapsed = (tick - active.start).coerceAtLeast(0L)

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(AppColors.Background)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x66C9963C), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.22f),
                    radius = size.width * 0.85f,
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * 0.78f, size.height * 0.22f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x33281C0C), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.72f),
                    radius = size.width * 0.7f,
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.15f, size.height * 0.72f),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                Formatters.time(tick),
                color = AppColors.TextPrimary,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp,
            )
            Text(
                Formatters.dateLong(tick),
                color = AppColors.TextMuted,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(56.dp))
            Text(
                Formatters.timer(elapsed),
                color = AppColors.TextPrimary,
                fontSize = 44.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                project?.name ?: stringResource(R.string.unknown_project),
                color = AppColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            if (!project?.clientName.isNullOrBlank()) {
                Text(project?.clientName.orEmpty(), color = AppColors.TextMuted, fontSize = 15.sp)
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = active.notes,
                onValueChange = { viewModel.updateActiveNotes(it) },
                label = { Text(stringResource(R.string.label_note)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Accent,
                    unfocusedBorderColor = AppColors.GlassBorder,
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    focusedLabelColor = AppColors.Accent,
                    unfocusedLabelColor = AppColors.TextMuted,
                    cursorColor = AppColors.Accent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .border(1.5.dp, AppColors.Accent, CircleShape)
                        .clickable {
                            viewModel.stopSession()
                            onClose()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .background(AppColors.Accent, RoundedCornerShape(3.dp)),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.clock_stop), color = AppColors.TextPrimary, fontSize = 15.sp)
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}
