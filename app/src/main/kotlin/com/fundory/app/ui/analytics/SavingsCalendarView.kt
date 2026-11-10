package com.fundory.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Transaction
import com.fundory.app.domain.model.TransactionType
import com.fundory.app.ui.theme.FundoryGreen
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SavingsCalendarView(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val monthTransactions = remember(transactions, currentYearMonth) {
        transactions.filter { tx ->
            val date = Instant.ofEpochMilli(tx.timestampEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            date.year == currentYearMonth.year && date.month == currentYearMonth.month
        }
    }

    val dailyTxs = remember(monthTransactions) {
        monthTransactions.groupBy { tx ->
            Instant.ofEpochMilli(tx.timestampEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    val monthDeposits = remember(monthTransactions) {
        monthTransactions
            .filter { it.type == TransactionType.DEPOSIT }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amountGel) }
    }

    val monthWithdrawals = remember(monthTransactions) {
        monthTransactions
            .filter { it.type == TransactionType.WITHDRAWAL }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amountGel) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }
                    IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
            }

            // Month Totals
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = FundoryGreen.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Deposited", style = MaterialTheme.typography.labelSmall, color = FundoryGreen)
                        Text("+${Currencies.format(monthDeposits, "GEL")}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = FundoryGreen)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF5350).copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Withdrawn", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF5350))
                        Text("-${Currencies.format(monthWithdrawals, "GEL")}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday Headers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Days Grid
            val firstDayOfMonth = currentYearMonth.atDay(1)
            val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value - 1) // 0 for Monday
            val daysInMonth = currentYearMonth.lengthOfMonth()

            val gridSlots = mutableListOf<LocalDate?>()
            for (i in 0 until dayOfWeekOffset) {
                gridSlots.add(null)
            }
            for (day in 1..daysInMonth) {
                gridSlots.add(currentYearMonth.atDay(day))
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(240.dp),
                userScrollEnabled = false
            ) {
                items(gridSlots) { date ->
                    if (date == null) {
                        Box(modifier = Modifier.size(38.dp))
                    } else {
                        val isSelected = (date == selectedDate)
                        val dayTxs = dailyTxs[date] ?: emptyList()
                        val hasDeposit = dayTxs.any { it.type == TransactionType.DEPOSIT }
                        val hasWithdrawal = dayTxs.any { it.type == TransactionType.WITHDRAWAL || it.type == TransactionType.TRANSFER }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) FundoryGreen.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { selectedDate = date },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                // Dots for activity
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (hasDeposit) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(FundoryGreen, CircleShape)
                                        )
                                    }
                                    if (hasWithdrawal) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(Color(0xFFEF5350), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Date Transactions
            val selectedTxs = dailyTxs[selectedDate] ?: emptyList()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Activity on ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()))}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (selectedTxs.isEmpty()) {
                Text(
                    text = "No transactions on this date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                selectedTxs.forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (tx.type == TransactionType.DEPOSIT) Icons.Default.Add else Icons.Default.Remove,
                                contentDescription = null,
                                tint = if (tx.type == TransactionType.DEPOSIT) FundoryGreen else Color(0xFFEF5350),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(tx.goalName ?: "General Savings", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                tx.note?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Text(
                            text = "${if (tx.type == TransactionType.DEPOSIT) "+" else "-"}${Currencies.format(tx.amountGel, "GEL")}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (tx.type == TransactionType.DEPOSIT) FundoryGreen else Color(0xFFEF5350)
                        )
                    }
                }
            }
        }
    }
}

