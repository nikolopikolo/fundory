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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fundory.app.data.local.entity.GoalSnapshotEntity
import com.fundory.app.ui.theme.FundoryGreen
import java.util.Locale

@Composable
fun GoalProgressHistoryChart(
    snapshots: List<GoalSnapshotEntity>,
    goalAccentColor: Color,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(snapshots) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Goal Progress History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Percentage completion over time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val currentPct = snapshots.lastOrNull()?.completionPercentage ?: 0f
            Text(
                text = String.format(Locale.US, "%.1f%%", currentPct),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = goalAccentColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (snapshots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Record transactions to view progress history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val width = size.width
                val height = size.height
                val paddingTop = 16.dp.toPx()
                val paddingBottom = 20.dp.toPx()
                val usableHeight = height - paddingTop - paddingBottom

                // Draw Milestone reference lines at 25%, 50%, 75%, 100%
                val milestones = listOf(25, 50, 75, 100)
                for (m in milestones) {
                    val y = paddingTop + usableHeight * (1f - (m / 100f))
                    drawLine(
                        color = if (m == 100) FundoryGreen.copy(alpha = 0.5f) else gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }

                // Map coordinates
                val coords = snapshots.mapIndexed { idx, snap ->
                    val x = if (snapshots.size > 1) {
                        (idx.toFloat() / (snapshots.size - 1)) * width
                    } else {
                        width / 2f
                    }
                    val pctNorm = (snap.completionPercentage.coerceIn(0f, 100f) / 100f) * animProgress.value
                    val y = paddingTop + usableHeight * (1f - pctNorm)
                    Offset(x, y)
                }

                if (coords.size >= 2) {
                    val path = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) {
                            lineTo(coords[i].x, coords[i].y)
                        }
                    }

                    // Gradient under progress line
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(coords.last().x, height - paddingBottom)
                        lineTo(coords.first().x, height - paddingBottom)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                goalAccentColor.copy(alpha = 0.35f),
                                goalAccentColor.copy(alpha = 0.0f)
                            ),
                            startY = paddingTop,
                            endY = height - paddingBottom
                        )
                    )

                    drawPath(
                        path = path,
                        color = goalAccentColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw point markers
                coords.forEach { pt ->
                    drawCircle(
                        color = goalAccentColor,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

