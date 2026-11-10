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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Product
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.atan2

val DonutColors = listOf(
    Color(0xFF38B273), // Green
    Color(0xFF3B82F6), // Blue
    Color(0xFF8B5CF6), // Purple
    Color(0xFFF59E0B), // Amber
    Color(0xFFEC4899), // Pink
    Color(0xFF06B6D4), // Cyan
    Color(0xFFF97316), // Orange
    Color(0xFF14B8A6)  // Teal
)

data class ProductSlice(
    val product: Product,
    val amountGel: BigDecimal,
    val percentage: Float,
    val color: Color
)

@Composable
fun SpendingBreakdownDonutChart(
    products: List<Product>,
    ratesToGel: Map<String, BigDecimal>,
    modifier: Modifier = Modifier
) {
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(products, ratesToGel) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
        selectedSliceIndex = null
    }

    // Compute slice percentages
    val totalGel = remember(products, ratesToGel) {
        products.fold(BigDecimal.ZERO) { acc, p ->
            acc.add(p.calculateTotalGel(ratesToGel))
        }
    }

    val slices = remember(products, ratesToGel, totalGel) {
        if (totalGel <= BigDecimal.ZERO) emptyList()
        else {
            products.mapIndexed { index, product ->
                val gel = product.calculateTotalGel(ratesToGel)
                val pct = if (totalGel > BigDecimal.ZERO) {
                    gel.divide(totalGel, 4, RoundingMode.HALF_UP).toFloat() * 100f
                } else 0f
                ProductSlice(
                    product = product,
                    amountGel = gel,
                    percentage = pct,
                    color = DonutColors[index % DonutColors.size]
                )
            }
        }
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
        Text(
            text = "Spending Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Product contribution to total goal cost",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (slices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add products to view spending distribution",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Donut Chart with central total readout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .pointerInput(slices) {
                            detectTapGestures { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                val radius = size.width / 2f

                                if (distance in (radius * 0.45f)..(radius * 1.05f)) {
                                    var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (touchAngle < 0) touchAngle += 360f

                                    // Rotate so top is 0 degrees (matching drawing start angle -90)
                                    touchAngle = (touchAngle + 90f) % 360f

                                    var cumulative = 0f
                                    for ((idx, slice) in slices.withIndex()) {
                                        val sweep = (slice.percentage / 100f) * 360f
                                        if (touchAngle in cumulative..(cumulative + sweep)) {
                                            selectedSliceIndex = if (selectedSliceIndex == idx) null else idx
                                            break
                                        }
                                        cumulative += sweep
                                    }
                                }
                            }
                        }
                ) {
                    val strokeWidth = 32.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                    val arcSize = Size(diameter, diameter)

                    var startAngle = -90f

                    slices.forEachIndexed { i, slice ->
                        val sweep = (slice.percentage / 100f) * 360f * animProgress.value
                        val isSelected = selectedSliceIndex == i
                        val effectiveStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweep.coerceAtLeast(1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = effectiveStroke, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }

                // Central Readout
                val activeSlice = selectedSliceIndex?.let { slices.getOrNull(it) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (activeSlice != null) {
                        Text(
                            text = String.format(Locale.US, "%.1f%%", activeSlice.percentage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = activeSlice.color
                        )
                        Text(
                            text = Currencies.format(activeSlice.amountGel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Currencies.format(totalGel),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Item Callout or List of contributions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slices.forEachIndexed { idx, slice ->
                    val isSelected = selectedSliceIndex == idx
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) slice.color.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(slice.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = slice.product.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${Currencies.format(slice.amountGel)} (${String.format(Locale.US, "%.1f%%", slice.percentage)})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) slice.color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
