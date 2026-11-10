package com.fundory.app.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.GoalProjectionResult
import com.fundory.app.domain.model.ProjectionScenario
import com.fundory.app.ui.theme.FundoryGreen
import com.fundory.app.ui.theme.ProjectionDashedGold
import com.fundory.app.ui.theme.TargetLineCyan
import java.math.BigDecimal

@Composable
fun SavingsProjectionChart(
    projection: GoalProjectionResult,
    historicalPoints: List<BalanceHistoryPoint>,
    selectedScenario: ProjectionScenario,
    onScenarioSelected: (ProjectionScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(projection, selectedScenario) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }

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
                    text = "Savings Projection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Forecasted path to target",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = projection.completionDateFormatted,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ProjectionDashedGold
                )
                Text(
                    text = if (projection.remainingMonths > 0) "${projection.remainingMonths} months left" else "Achieved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scenario selector chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProjectionScenario.entries.forEach { scenario ->
                val isSelected = scenario == selectedScenario
                FilterChip(
                    selected = isSelected,
                    onClick = { onScenarioSelected(scenario) },
                    label = {
                        Text(
                            text = scenario.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.height(28.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ProjectionDashedGold.copy(alpha = 0.2f),
                        selectedLabelColor = ProjectionDashedGold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Canvas Forecast Chart
        val targetAmount = projection.targetAmountGel
        val histColor = FundoryGreen
        val projColor = ProjectionDashedGold
        val targetLineColor = TargetLineCyan

        val maxVal = remember(targetAmount, projection.timeline, historicalPoints) {
            maxOf(
                targetAmount.multiply(BigDecimal("1.15")).toFloat(),
                projection.timeline.maxOfOrNull { it.balanceGel }?.toFloat() ?: 100f,
                historicalPoints.maxOfOrNull { it.balanceGel }?.toFloat() ?: 100f
            ).coerceAtLeast(50f)
        }

        val totalSteps = remember(historicalPoints, projection.timeline) {
            (historicalPoints.size + projection.timeline.size).coerceAtLeast(2)
        }

        val histNormalized = remember(historicalPoints, maxVal, totalSteps) {
            historicalPoints.mapIndexed { i, p ->
                val xNorm = i.toFloat() / (totalSteps - 1)
                val yNorm = (p.balanceGel.toFloat() / maxVal).coerceIn(0f, 1f)
                xNorm to yNorm
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingTop = 20.dp.toPx()
            val paddingBottom = 20.dp.toPx()
            val usableHeight = height - paddingTop - paddingBottom

            // 1. Draw Target Reference Line (Horizontal dashed cyan)
            val targetNorm = (targetAmount.toFloat() / maxVal).coerceIn(0f, 1f)
            val targetY = paddingTop + usableHeight * (1f - targetNorm)

            drawLine(
                color = targetLineColor,
                start = Offset(0f, targetY),
                end = Offset(width, targetY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )

            val histCount = historicalPoints.size

            // Historical Points (Solid line)
            if (histNormalized.isNotEmpty()) {
                val histCoords = histNormalized.map { (xNorm, yNorm) ->
                    val x = xNorm * width
                    val y = paddingTop + usableHeight * (1f - yNorm)
                    Offset(x, y)
                }

                if (histCoords.size >= 2) {
                    val histPath = Path().apply {
                        moveTo(histCoords.first().x, histCoords.first().y)
                        for (i in 1 until histCoords.size) {
                            lineTo(histCoords[i].x, histCoords[i].y)
                        }
                    }
                    drawPath(
                        path = histPath,
                        color = histColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Projected Points (Dashed Line continuing from current saved to target)
            val currentSaved = projection.currentSavedGel
            val startX = if (histCount > 0) ((histCount - 1).toFloat() / (totalSteps - 1)) * width else 0f
            val startY = paddingTop + usableHeight * (1f - (currentSaved.toFloat() / maxVal).coerceIn(0f, 1f))
            val startOffset = Offset(startX, startY)

            val projCoords = mutableListOf(startOffset)
            var reachedTargetOffset: Offset? = null

            projection.timeline.forEachIndexed { i, pt ->
                val x = ((histCount + i).toFloat() / (totalSteps - 1)) * width
                val yNorm = (pt.balanceGel.toFloat() / maxVal).coerceIn(0f, 1f)
                val y = paddingTop + usableHeight * (1f - (yNorm * animProgress.value))
                val coord = Offset(x, y)
                projCoords.add(coord)

                if (pt.isTargetReached && reachedTargetOffset == null) {
                    reachedTargetOffset = coord
                }
            }

            if (projCoords.size >= 2) {
                val projPath = Path().apply {
                    moveTo(projCoords.first().x, projCoords.first().y)
                    for (i in 1 until projCoords.size) {
                        lineTo(projCoords[i].x, projCoords[i].y)
                    }
                }

                drawPath(
                    path = projPath,
                    color = projColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                )
            }

            // Target Intersection Callout
            reachedTargetOffset?.let { offset ->
                drawCircle(
                    color = ProjectionDashedGold.copy(alpha = 0.35f),
                    radius = 12.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = ProjectionDashedGold,
                    radius = 6.dp.toPx(),
                    center = offset
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = FundoryGreen, label = "Historical Savings (Solid)")
            LegendItem(color = ProjectionDashedGold, label = "Projected (Dashed)")
            LegendItem(color = TargetLineCyan, label = "Target: ${Currencies.format(targetAmount)}")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Explanation text box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = projection.explanationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

