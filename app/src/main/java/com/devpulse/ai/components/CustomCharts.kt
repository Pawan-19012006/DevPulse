package com.devpulse.ai.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.ui.theme.BorderDark
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.Secondary
import com.devpulse.ai.utils.ActivityPoint
import com.devpulse.ai.utils.LanguageShare

@Composable
fun PremiumDonutChart(
    shares: List<LanguageShare>,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animateFraction by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2.5f
            val strokeWidth = 32.dp.toPx()

            var startAngle = -90f

            if (shares.isEmpty()) {
                // Empty state drawing
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                shares.forEach { share ->
                    val sweepAngle = (share.percentage / 100f) * 360f * animateFraction
                    val parsedColor = try {
                        Color(android.graphics.Color.parseColor(share.color))
                    } catch (e: Exception) {
                        Primary
                    }

                    drawArc(
                        color = parsedColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += (share.percentage / 100f) * 360f
                }
            }
        }

        // Central text label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Languages",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${shares.size} Total",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PremiumLineChart(
    points: List<ActivityPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val maxVal = points.maxOfOrNull { it.value } ?: 1
    val yMax = if (maxVal > 0) maxVal else 1

    var animationPlayed by remember { mutableStateOf(false) }
    val animateFraction by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1200)
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacingX = width / (points.size - 1).coerceAtLeast(1)

            val linePath = Path()
            val fillPath = Path()

            val coordinates = points.mapIndexed { idx, pt ->
                val x = idx * spacingX
                // Invert Y coordinate because 0 is at the top
                val y = height - (height * (pt.value.toFloat() / yMax) * animateFraction)
                Offset(x, y)
            }

            coordinates.forEachIndexed { idx, offset ->
                if (idx == 0) {
                    linePath.moveTo(offset.x, offset.y)
                    fillPath.moveTo(offset.x, height)
                    fillPath.lineTo(offset.x, offset.y)
                } else {
                    // Draw smooth curves using cubic interpolation points
                    val prev = coordinates[idx - 1]
                    val controlX1 = prev.x + (offset.x - prev.x) / 2
                    val controlY1 = prev.y
                    val controlX2 = prev.x + (offset.x - prev.x) / 2
                    val controlY2 = offset.y

                    linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, offset.x, offset.y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, offset.x, offset.y)
                }
            }

            if (coordinates.isNotEmpty()) {
                fillPath.lineTo(coordinates.last().x, height)
                fillPath.close()

                // Draw glowing background fill gradient
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )

                // Draw solid line
                drawPath(
                    path = linePath,
                    color = Primary,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw points and values
                coordinates.forEachIndexed { idx, offset ->
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = Primary,
                        radius = 6.dp.toPx(),
                        center = offset,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PremiumBarChart(
    points: List<ActivityPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val maxVal = points.maxOfOrNull { it.value } ?: 1
    val yMax = if (maxVal > 0) maxVal else 1

    var animationPlayed by remember { mutableStateOf(false) }
    val animateFraction by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val width = size.width
            val height = size.height
            val barCount = points.size
            val barWidth = 24.dp.toPx()
            val totalSpacing = width - (barWidth * barCount)
            val spacingX = totalSpacing / (barCount - 1).coerceAtLeast(1)

            points.forEachIndexed { idx, pt ->
                val x = idx * (barWidth + spacingX)
                val barHeight = height * (pt.value.toFloat() / yMax) * animateFraction
                val y = height - barHeight

                // Draw Bar with rounded top corners
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Secondary, Secondary.copy(alpha = 0.4f))
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Optional: Draw text values inside Canvas or above bars
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(38.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
