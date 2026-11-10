package com.fundory.app.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.PriceHistoryRecord
import com.fundory.app.domain.model.Product
import com.fundory.app.ui.theme.FundoryGreen
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PriceHistoryDialog(
    product: Product,
    history: List<PriceHistoryRecord>,
    onDismiss: () -> Unit,
    onRecordNewPrice: (newPrice: BigDecimal, note: String?) -> Unit
) {
    var isAddingPrice by remember { mutableStateOf(false) }
    var inputPriceText by remember { mutableStateOf("") }
    var inputNoteText by remember { mutableStateOf("") }

    val allPrices = if (history.isNotEmpty()) history else listOf(
        PriceHistoryRecord(
            productId = product.id,
            price = product.originalPrice,
            currency = product.currency,
            recordedAtEpochMillis = product.dateAddedEpochMillis,
            note = "Initial Price"
        )
    )

    val initialPrice = allPrices.first().price
    val currentPrice = allPrices.last().price
    val lowestPrice = allPrices.minOf { it.price }
    val highestPrice = allPrices.maxOf { it.price }

    val percentChange = if (initialPrice > BigDecimal.ZERO) {
        currentPrice.subtract(initialPrice)
            .multiply(BigDecimal(100))
            .divide(initialPrice, 1, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Price History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriceStatBox(
                        title = "Current",
                        value = Currencies.format(currentPrice, product.currency),
                        modifier = Modifier.weight(1f)
                    )
                    PriceStatBox(
                        title = "Lowest",
                        value = Currencies.format(lowestPrice, product.currency),
                        modifier = Modifier.weight(1f),
                        accentColor = FundoryGreen
                    )
                    PriceStatBox(
                        title = "Highest",
                        value = Currencies.format(highestPrice, product.currency),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Percentage difference banner
                if (allPrices.size > 1) {
                    val isCheaper = percentChange < BigDecimal.ZERO
                    val color = if (isCheaper) FundoryGreen else Color(0xFFEF5350)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isCheaper) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${percentChange.abs().toPlainString()}% ${if (isCheaper) "drop from initial price" else "increase from initial price"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = color
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Mini Chart
                if (allPrices.size >= 2) {
                    Text(
                        text = "Price Trend",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PriceTrendMiniChart(
                        prices = allPrices.map { it.price.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Add New Price Section
                if (!isAddingPrice) {
                    Button(
                        onClick = { isAddingPrice = true },
                        colors = ButtonDefaults.buttonColors(containerColor = FundoryGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record New Price")
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Record Price Change",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = inputPriceText,
                                onValueChange = { inputPriceText = it },
                                label = { Text("New Price (${product.currency})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = inputNoteText,
                                onValueChange = { inputNoteText = it },
                                label = { Text("Note (e.g. Sale, Coupon)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { isAddingPrice = false },
                                    colors = ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val parsed = Currencies.parseAmount(inputPriceText)
                                        if (parsed != null && parsed > BigDecimal.ZERO) {
                                            onRecordNewPrice(parsed, inputNoteText.ifBlank { null })
                                            isAddingPrice = false
                                            inputPriceText = ""
                                            inputNoteText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FundoryGreen)
                                ) {
                                    Text("Save Price")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline List
                Text(
                    text = "Recorded History",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                allPrices.reversed().forEach { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Currencies.format(record.price, record.currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            record.note?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = formatter.format(Date(record.recordedAtEpochMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceStatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PriceTrendMiniChart(
    prices: List<Float>,
    modifier: Modifier = Modifier
) {
    val min = prices.minOrNull() ?: 0f
    val max = prices.maxOrNull() ?: 1f
    val range = if (max > min) max - min else 1f

    Canvas(modifier = modifier) {
        if (prices.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val stepX = w / (prices.size - 1)

        val path = Path()
        prices.forEachIndexed { i, p ->
            val x = i * stepX
            val normalizedY = (p - min) / range
            val y = h - (normalizedY * (h - 20f)) - 10f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = FundoryGreen,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw points
        prices.forEachIndexed { i, p ->
            val x = i * stepX
            val normalizedY = (p - min) / range
            val y = h - (normalizedY * (h - 20f)) - 10f
            drawCircle(
                color = FundoryGreen,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

