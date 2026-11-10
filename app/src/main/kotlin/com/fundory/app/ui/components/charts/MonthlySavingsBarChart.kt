package com.fundory.app.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fundory.app.domain.model.Currencies
import com.fundory.app.ui.theme.NegativeRed
import com.fundory.app.ui.theme.PositiveGreen
import java.math.BigDecimal

data class MonthSavingsData(
    val monthLabel: String, // e.g. "Nov 2026"
    val depositsGel: BigDecimal,
    val withdrawalsGel: BigDecimal,
    val netSavingsGel: BigDecimal
)

enum class BarChartMode {
    NET_SAVINGS,
    TOTAL_DEPOSITS
}

@Composable
fun MonthlySavingsBarChart(
    data: List<MonthSavingsData>,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(BarChartMode.NET_SAVINGS) }
    var selectedMonthIndex by remember { mutableStateOf<Int?>(null) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(data, mode) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 600))
        selectedMonthIndex = null
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
                    text = "Monthly Savings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (mode == BarChartMode.NET_SAVINGS) "Net (Deposits − Withdrawals)" else "Gross Deposits per Month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mode Selector
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = mode == BarChartMode.NET_SAVINGS,
                    onClick = { mode = BarChartMode.NET_SAVINGS },
                    label = { Text("Net", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PositiveGreen.copy(alpha = 0.2f),
                        selectedLabelColor = PositiveGreen
                    )
                )
                FilterChip(
                    selected = mode == BarChartMode.TOTAL_DEPOSITS,
                    onClick = { mode = BarChartMode.TOTAL_DEPOSITS },
                    label = { Text("Deposits", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PositiveGreen.copy(alpha = 0.2f),
                        selectedLabelColor = PositiveGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected month details callout
        val activeItem = if (selectedMonthIndex != null && selectedMonthIndex in data.indices) {
            data[selectedMonthIndex!!]
        } else {
            data.lastOrNull()
        }

        if (activeItem != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeItem.monthLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "+${Currencies.format(activeItem.depositsGel)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PositiveGreen
                    )
                    if (activeItem.withdrawalsGel > BigDecimal.ZERO) {
                        Text(
                            text = "-${Currencies.format(activeItem.withdrawalsGel)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NegativeRed
                        )
                    }
                    Text(
                        text = "Net: ${if (activeItem.netSavingsGel >= BigDecimal.ZERO) "+" else ""}${Currencies.format(activeItem.netSavingsGel)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (activeItem.netSavingsGel >= BigDecimal.ZERO) PositiveGreen else NegativeRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (data.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No monthly savings recorded yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val axisColor = MaterialTheme.colorScheme.outlineVariant
            val textPaintColor = MaterialTheme.colorScheme.onSurfaceVariant.hashCode()

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / data.size
                            val idx = (offset.x / slotWidth).toInt().coerceIn(0, data.size - 1)
                            selectedMonthIndex = idx
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val labelHeight = 24.dp.toPx()
                val chartHeight = height - labelHeight

                // Find max absolute value
                val maxVal = data.maxOfOrNull { item ->
                    val v = if (mode == BarChartMode.NET_SAVINGS) item.netSavingsGel.abs() else item.depositsGel
                    v.toFloat()
                }?.coerceAtLeast(50f) ?: 100f

                val hasNegative = mode == BarChartMode.NET_SAVINGS && data.any { it.netSavingsGel < BigDecimal.ZERO }
                val zeroY = if (hasNegative) chartHeight / 2f else chartHeight - 4.dp.toPx()

                // Baseline
                drawLine(
                    color = axisColor,
                    start = Offset(0f, zeroY),
                    end = Offset(width, zeroY),
                    strokeWidth = 1.5.dp.toPx()
                )

                val slotWidth = width / data.size
                val barWidth = (slotWidth * 0.55f).coerceAtMost(36.dp.toPx())

                data.forEachIndexed { i, item ->
                    val x = i * slotWidth + (slotWidth - barWidth) / 2f
                    val amount = if (mode == BarChartMode.NET_SAVINGS) item.netSavingsGel else item.depositsGel
                    val amountFloat = amount.toFloat()
                    val isPos = amountFloat >= 0f

                    val barHeight = (kotlin.math.abs(amountFloat) / maxVal) * (if (hasNegative) (chartHeight / 2f - 10f) else (chartHeight - 16f)) * animProgress.value
                    val topY = if (isPos) zeroY - barHeight else zeroY
                    val barColor = if (isPos) PositiveGreen else NegativeRed

                    val isSelected = selectedMonthIndex == i
                    val alpha = if (selectedMonthIndex == null || isSelected) 1f else 0.4f

                    // Draw bar with rounded top/bottom
                    drawRoundRect(
                        color = barColor.copy(alpha = alpha),
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, barHeight.coerceAtLeast(2.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Month label text under chart
                    val textPaint = android.graphics.Paint().apply {
                        color = textPaintColor
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val shortLabel = item.monthLabel.take(3)
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            shortLabel,
                            x + barWidth / 2f,
                            height - 4.dp.toPx(),
                            textPaint
                        )
                    }
                }
            }
        }
    }
}
