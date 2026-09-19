package de.willigering.workingtime.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import de.willigering.workingtime.ui.theme.AppColors
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AnimatedClock(
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(50L)
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(w, h) * 0.38f

        drawCircle(
            color = AppColors.Accent.copy(alpha = 0.03f),
            radius = radius * 1.10f,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = AppColors.Surface.copy(alpha = 0.55f),
            radius = radius,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.4f),
        )

        for (tick in 0 until 60) {
            val angle = (tick * 6.0 - 90.0) * PI / 180.0
            val isHour = tick % 5 == 0
            val isCardinal = tick % 15 == 0
            val inner = radius - when {
                isCardinal -> 11f
                isHour -> 9f
                else -> 6f
            }
            val outer = radius - 4f
            val x1 = cx + inner * cos(angle).toFloat()
            val y1 = cy + inner * sin(angle).toFloat()
            val x2 = cx + outer * cos(angle).toFloat()
            val y2 = cy + outer * sin(angle).toFloat()
            drawLine(
                color = when {
                    isCardinal -> Color.White.copy(alpha = 0.32f)
                    isHour -> Color.White.copy(alpha = 0.18f)
                    else -> Color.White.copy(alpha = 0.07f)
                },
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = when {
                    isCardinal -> 2.2f
                    isHour -> 1.6f
                    else -> 1f
                },
                cap = StrokeCap.Round,
            )
        }

        val numberPaint = Paint().apply {
            isAntiAlias = true
            color = Color.White.copy(alpha = 0.22f).toArgb()
            textAlign = Paint.Align.CENTER
            textSize = radius * 0.12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val native = drawContext.canvas.nativeCanvas
        val numberRadius = radius * 0.68f
        val numbers = listOf(
            12 to 270.0,
            3 to 0.0,
            6 to 90.0,
            9 to 180.0,
        )
        for ((label, deg) in numbers) {
            val rad = deg * PI / 180.0
            val nx = cx + numberRadius * cos(rad).toFloat()
            val ny = cy + numberRadius * sin(rad).toFloat()
            val fm = numberPaint.fontMetrics
            val baseline = ny - (fm.ascent + fm.descent) / 2f
            native.drawText(label.toString(), nx, baseline, numberPaint)
        }

        val hour = now.hour % 12 + now.minute / 60.0 + now.second / 3600.0 + now.nano / 3_600_000_000_000.0
        val minute = now.minute + now.second / 60.0 + now.nano / 60_000_000_000.0
        val second = now.second + now.nano / 1_000_000_000.0

        val hourAngle = (hour * 30.0 - 90.0) * PI / 180.0
        val minuteAngle = (minute * 6.0 - 90.0) * PI / 180.0
        val secondAngle = (second * 6.0 - 90.0) * PI / 180.0

        drawLine(
            color = Color(0xFFFFD08A).copy(alpha = 0.85f),
            start = Offset(cx, cy),
            end = Offset(
                cx + radius * 0.48f * cos(hourAngle).toFloat(),
                cy + radius * 0.48f * sin(hourAngle).toFloat(),
            ),
            strokeWidth = 4.2f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.72f),
            start = Offset(cx, cy),
            end = Offset(
                cx + radius * 0.66f * cos(minuteAngle).toFloat(),
                cy + radius * 0.66f * sin(minuteAngle).toFloat(),
            ),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = AppColors.Accent.copy(alpha = 0.80f),
            start = Offset(cx, cy),
            end = Offset(
                cx + radius * 0.76f * cos(secondAngle).toFloat(),
                cy + radius * 0.76f * sin(secondAngle).toFloat(),
            ),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round,
        )

        drawCircle(
            color = AppColors.Accent.copy(alpha = 0.28f),
            radius = 6f,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = AppColors.Accent,
            radius = 3.2f,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = 1.3f,
            center = Offset(cx, cy),
        )
    }
}
