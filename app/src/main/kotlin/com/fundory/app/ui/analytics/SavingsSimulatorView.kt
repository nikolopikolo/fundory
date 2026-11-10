package com.fundory.app.ui.analytics

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Goal
import com.fundory.app.ui.theme.FundoryGreen
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun SavingsSimulatorView(
    goals: List<Goal>,
    totalSavedGel: BigDecimal,
    totalTargetGel: BigDecimal,
    modifier: Modifier = Modifier
) {
    var simulatedMonthlySavings by remember { mutableFloatStateOf(100f) }
    var simulatedBonusDeposit by remember { mutableFloatStateOf(0f) }
    var simulatedPriceShiftPercent by remember { mutableFloatStateOf(0f) }

    val remainingTarget = remember(totalTargetGel, totalSavedGel, simulatedBonusDeposit, simulatedPriceShiftPercent) {
        val adjustedTarget = if (simulatedPriceShiftPercent != 0f) {
            totalTargetGel.multiply(BigDecimal(1.0 + simulatedPriceShiftPercent / 100.0))
        } else totalTargetGel

        val effectiveSaved = totalSavedGel.add(BigDecimal(simulatedBonusDeposit.toDouble()))
        val rem = adjustedTarget.subtract(effectiveSaved)
        (if (rem < BigDecimal.ZERO) BigDecimal.ZERO else rem).setScale(2, RoundingMode.HALF_UP)
    }

    val simulatedMonthsNeeded = remember(remainingTarget, simulatedMonthlySavings) {
        if (remainingTarget <= BigDecimal.ZERO) 0
        else if (simulatedMonthlySavings <= 0f) null
        else {
            Math.ceil(remainingTarget.toDouble() / simulatedMonthlySavings.toDouble()).toInt()
        }
    }

    val simulatedCompletionDateText = remember(simulatedMonthsNeeded) {
        if (simulatedMonthsNeeded == null) {
            "Indefinite (Monthly savings is 0)"
        } else if (simulatedMonthsNeeded == 0) {
            "Goal Achieved!"
        } else {
            val date = LocalDate.now().plusMonths(simulatedMonthsNeeded.toLong())
            date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(FundoryGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = FundoryGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "What-If Savings Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Simulate financial scenarios safely",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Projection Result Card
            Card(
                colors = CardDefaults.cardColors(containerColor = FundoryGreen.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Simulated Completion Date",
                        style = MaterialTheme.typography.labelSmall,
                        color = FundoryGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = simulatedCompletionDateText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (simulatedMonthsNeeded != null && simulatedMonthsNeeded > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "~$simulatedMonthsNeeded months remaining at ${simulatedMonthlySavings.toInt()} GEL/month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider 1: Monthly Contribution
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Monthly Savings",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${simulatedMonthlySavings.toInt()} GEL",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = FundoryGreen
                )
            }
            Slider(
                value = simulatedMonthlySavings,
                onValueChange = { simulatedMonthlySavings = it },
                valueRange = 0f..1000f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = FundoryGreen,
                    activeTrackColor = FundoryGreen
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Slider 2: One-time Bonus / Lump Sum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "One-Time Bonus / Deposit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "+${simulatedBonusDeposit.toInt()} GEL",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = FundoryGreen
                )
            }
            Slider(
                value = simulatedBonusDeposit,
                onValueChange = { simulatedBonusDeposit = it },
                valueRange = 0f..2000f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = FundoryGreen,
                    activeTrackColor = FundoryGreen
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Slider 3: Product Price Shift
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Product Price Shift",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${if (simulatedPriceShiftPercent > 0) "+" else ""}${simulatedPriceShiftPercent.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (simulatedPriceShiftPercent > 0) Color(0xFFEF5350) else FundoryGreen
                )
            }
            Slider(
                value = simulatedPriceShiftPercent,
                onValueChange = { simulatedPriceShiftPercent = it },
                valueRange = -30f..30f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = FundoryGreen,
                    activeTrackColor = FundoryGreen
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Simulated values are not saved to your real database.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

