package com.fundory.app.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fundory.app.domain.model.Currencies
import com.fundory.app.ui.theme.FundoryGreen
import com.fundory.app.ui.theme.FundoryGreenLight
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TimeRange(val label: String, val days: Int) {
    SEVEN_DAYS("7D", 7),
    THIRTY_DAYS("30D", 30),
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180),
    TWELVE_MONTHS("12M", 365),
    ALL_TIME("All", -1)
}

data class BalanceHistoryPoint(
    val timestampEpochMillis: Long,
    val balanceGel: BigDecimal
)

@Composable
fun SavingsGrowthLineChart(
    points: List<BalanceHistoryPoint>,
    modifier: Modifier = Modifier,
    selectedTimeRange: TimeRange = TimeRange.ALL_TIME,
    onTimeRangeSelected: (TimeRange) -> Unit = {}
) {
    var touchedIndex by remember { mutableStateOf<Int?>(null) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(points, selectedTimeRange) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 700))
        touchedIndex = null
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Savings Growth",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Historical balance over time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Currently inspected point or latest
            val activePoint = if (touchedIndex != null && touchedIndex in points.indices) {
                points[touchedIndex!!]
            } else {
                points.lastOrNull()
            }

            if (activePoint != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Currencies.format(activePoint.balanceGel),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FundoryGreen
                    )
                    Text(
                        text = dateFormat.format(Date(activePoint.timestampEpochMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time Range Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TimeRange.entries.forEach { range ->
                val isSelected = range == selectedTimeRange
                FilterChip(
                    selected = isSelected,
                    onClick = { onTimeRangeSelected(range) },
                    label = {
                        Text(
                            text = range.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundoryGreen.copy(alpha = 0.2f),
                        selectedLabelColor = FundoryGreenLight,
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) FundoryGreen else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transaction data for this time period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Interactive Canvas Line Chart
            val primaryColor = FundoryGreen
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            val tooltipColor = MaterialTheme.colorScheme.surfaceVariant

            val normalizedData = remember(points) {
                if (points.isEmpty()) emptyList()
                else {
                    val maxBalance = points.maxOfOrNull { it.balanceGel } ?: BigDecimal.ONE
                    val minBalance = points.minOfOrNull { it.balanceGel } ?: BigDecimal.ZERO
                    val range = if (maxBalance > minBalance) {
                        (maxBalance.subtract(minBalance)).toFloat().coerceAtLeast(10f)
                    } else 10f

                    points.mapIndexed { index, point ->
                        val xFraction = if (points.size > 1) index.toFloat() / (points.size - 1) else 0.5f
                        val yNorm = (point.balanceGel.subtract(minBalance)).toFloat() / range
                        xFraction to yNorm
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .pointerInput(points) {
                        detectTapGestures(
                            onPress = { offset ->
                                val xFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                val idx = (xFraction * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                                touchedIndex = idx
                            }
                        )
                    }
                    .pointerInput(points) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                val xFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                val idx = (xFraction * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                                touchedIndex = idx
                            },
                            onDragEnd = {
                                // keep latest touched or reset
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingBottom = 24.dp.toPx()
                val paddingTop = 16.dp.toPx()
                val usableHeight = height - paddingTop - paddingBottom

                // Draw horizontal grid lines (3 levels)
                val gridLevels = 3
                for (i in 0..gridLevels) {
                    val y = paddingTop + usableHeight * (i.toFloat() / gridLevels)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }

                // Build line path from precomputed normalized fractions
                val coordinates = normalizedData.map { (xFraction, yNorm) ->
                    val x = xFraction * width
                    val y = paddingTop + usableHeight * (1f - (yNorm * animProgress.value))
                    Offset(x, y)
                }

                if (coordinates.size >= 2) {
                    // Smooth cubic bezier curve
                    val strokePath = Path().apply {
                        moveTo(coordinates.first().x, coordinates.first().y)
                        for (i in 0 until coordinates.size - 1) {
                            val p0 = coordinates[i]
                            val p1 = coordinates[i + 1]
                            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                        }
                    }

                    // Gradient fill area
                    val fillPath = Path().apply {
                        addPath(strokePath)
                        lineTo(coordinates.last().x, height - paddingBottom)
                        lineTo(coordinates.first().x, height - paddingBottom)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.35f * animProgress.value),
                                primaryColor.copy(alpha = 0.0f)
                            ),
                            startY = paddingTop,
                            endY = height - paddingBottom
                        )
                    )

                    drawPath(
                        path = strokePath,
                        color = primaryColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                } else if (coordinates.size == 1) {
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = coordinates.first()
                    )
                }

                // Draw Scrubber / Crosshair when touched
                touchedIndex?.let { idx ->
                    if (idx in coordinates.indices) {
                        val touchCoord = coordinates[idx]

                        // Vertical guide line
                        drawLine(
                            color = primaryColor.copy(alpha = 0.8f),
                            start = Offset(touchCoord.x, paddingTop),
                            end = Offset(touchCoord.x, height - paddingBottom),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        // Outer ring & center dot
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.3f),
                            radius = 10.dp.toPx(),
                            center = touchCoord
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 5.dp.toPx(),
                            center = touchCoord
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "💡 Tip: Tap and drag along the line to inspect balance at any date.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

