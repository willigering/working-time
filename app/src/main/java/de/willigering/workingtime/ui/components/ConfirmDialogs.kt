package de.willigering.workingtime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.willigering.workingtime.R
import de.willigering.workingtime.ui.theme.AppColors

@Composable
fun AppConfirmDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AppColors.SurfaceElevated)
                .border(1.dp, AppColors.GlassBorder, shape)
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String = stringResource(R.string.delete),
    detail: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppConfirmDialog(onDismissRequest = onDismiss) {
        DialogWarningIcon()
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (!detail.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                detail,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(20.dp))
        ChoiceButton(
            title = confirmLabel,
            hint = "",
            icon = Icons.Rounded.DeleteForever,
            containerColor = AppColors.Error,
            contentColor = Color.White,
            onClick = onConfirm,
        )
        Spacer(Modifier.height(10.dp))
        CancelChoiceButton(onClick = onDismiss)
    }
}

private enum class ProjectDeleteChoice { KeepEntries, DeleteAll }

@Composable
fun DeleteProjectDialog(
    projectName: String,
    onConfirm: (deleteSessions: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var choice by remember { mutableStateOf<ProjectDeleteChoice?>(null) }

    AppConfirmDialog(onDismissRequest = onDismiss) {
        DialogWarningIcon()
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.delete_project_title),
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.delete_project_message, projectName),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        SelectableChoice(
            selected = choice == ProjectDeleteChoice.KeepEntries,
            title = stringResource(R.string.delete_project_keep_entries),
            hint = stringResource(R.string.delete_project_keep_hint),
            icon = Icons.Outlined.BookmarkBorder,
            accent = AppColors.Accent,
            onClick = { choice = ProjectDeleteChoice.KeepEntries },
        )
        Spacer(Modifier.height(10.dp))
        SelectableChoice(
            selected = choice == ProjectDeleteChoice.DeleteAll,
            title = stringResource(R.string.delete_project_and_entries),
            hint = stringResource(R.string.delete_project_all_hint),
            icon = Icons.Rounded.DeleteForever,
            accent = AppColors.Error,
            onClick = { choice = ProjectDeleteChoice.DeleteAll },
        )
        Spacer(Modifier.height(18.dp))
        ChoiceButton(
            title = stringResource(R.string.delete),
            hint = "",
            icon = Icons.Rounded.DeleteForever,
            containerColor = AppColors.Error,
            contentColor = Color.White,
            enabled = choice != null,
            onClick = {
                val selected = choice
                if (selected != null) {
                    onConfirm(selected == ProjectDeleteChoice.DeleteAll)
                }
            },
        )
        Spacer(Modifier.height(10.dp))
        CancelChoiceButton(onClick = onDismiss)
    }
}

@Composable
private fun DialogWarningIcon() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(AppColors.Error.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = AppColors.Error,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun SelectableChoice(
    selected: Boolean,
    title: String,
    hint: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (selected) accent else AppColors.GlassBorder
    val container = if (selected) accent.copy(alpha = 0.16f) else AppColors.Anthracite
    val content = if (selected) AppColors.TextPrimary else AppColors.TextSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) accent else AppColors.TextMuted, CircleShape)
                .background(if (selected) accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppColors.Background),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) accent else AppColors.TextMuted,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = content,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                hint,
                color = content.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ChoiceButton(
    title: String,
    hint: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    iconColor: Color = contentColor,
    borderColor: Color? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .background(containerColor)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.5.dp, borderColor, shape)
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (hint.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    hint,
                    color = contentColor.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun CancelChoiceButton(
    onClick: () -> Unit,
    label: String = stringResource(R.string.cancel),
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, AppColors.GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = AppColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
