package de.willigering.workingtime.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.willigering.workingtime.R
import de.willigering.workingtime.ui.theme.AppColors
import de.willigering.workingtime.util.Formatters

val GoldBrush = Brush.horizontalGradient(
    listOf(Color(0xFFF3D48A), Color(0xFFE0B15A), Color(0xFFC9963C)),
)

fun markLetter(name: String): String =
    name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•"

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(28.dp)) {
        val r = size.minDimension / 2f
        drawCircle(color = AppColors.Accent.copy(alpha = 0.16f), radius = r)
        drawCircle(
            color = AppColors.Accent,
            radius = r - 1.2f,
            style = Stroke(width = 1.4.dp.toPx()),
        )
        val d = r * 0.42f
        val path = Path().apply {
            moveTo(center.x, center.y - d)
            lineTo(center.x + d, center.y)
            lineTo(center.x, center.y + d)
            lineTo(center.x - d, center.y)
            close()
        }
        drawPath(path, color = AppColors.Accent)
    }
}

@Composable
fun ProjectMark(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    corner: Dp = 12.dp,
    fontSize: TextUnit = 16.sp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            markLetter(name),
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(shape)
            .background(if (enabled) GoldBrush else Brush.linearGradient(listOf(AppColors.Surface, AppColors.Surface)))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) AppColors.Ink else AppColors.TextMuted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text,
            color = if (enabled) AppColors.Ink else AppColors.TextMuted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
    }
}

@Composable
fun GoldOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val color = if (enabled) AppColors.Accent else AppColors.TextMuted
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = color,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = AppColors.TextMuted,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.GlassBorder, shape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(8.dp))
        }
        Text(
            value,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(label, color = AppColors.TextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun GoldTimerRing(
    elapsedMs: Long,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sweep = ((elapsedMs / 1000f) % 3600f) / 3600f * 360f
    Box(
        modifier = modifier
            .size(248.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = 9.dp.toPx()
            val inset = stroke
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawCircle(
                color = AppColors.Accent.copy(alpha = 0.07f),
                radius = size.minDimension * 0.40f,
            )
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF8C6A30),
                        Color(0xFFF8E7B0),
                        AppColors.Accent,
                        Color(0xFFB8862F),
                        Color(0xFFF8E7B0),
                    ),
                ),
                startAngle = -90f,
                sweepAngle = sweep.coerceAtLeast(14f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                Formatters.timer(elapsedMs),
                color = AppColors.TextPrimary,
                fontSize = 40.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(caption, color = AppColors.TextMuted, fontSize = 14.sp)
        }
    }
}

@Composable
fun TimeEntryRow(
    name: String,
    subtitle: String,
    duration: String,
    color: Color,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProjectMark(name, color)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                name,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = AppColors.TextMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(duration, color = AppColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Box {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.more_actions),
                tint = AppColors.TextMuted,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp)
                    .clickable { menu = true },
            )
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit), color = AppColors.TextPrimary) },
                    onClick = { menu = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = AppColors.Error) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
fun ScreenHeading(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier = modifier,
        color = AppColors.TextPrimary,
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp,
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.Accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(20.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(subtitle, color = AppColors.TextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AppColors.TextMuted)
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.Accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(18.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(subtitle, color = AppColors.TextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AppColors.TextMuted)
    }
}

@Composable
fun RangeSegment(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .padding(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) AppColors.Accent else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) AppColors.Ink else AppColors.TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
fun AddIconButton(onClick: () -> Unit, description: String) {
    Icon(
        Icons.Rounded.Add,
        contentDescription = description,
        tint = AppColors.Accent,
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onClick),
    )
}

@Composable
fun MetricLine(label: String, value: String, valueColor: Color = AppColors.TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = AppColors.TextSecondary, fontSize = 15.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}
