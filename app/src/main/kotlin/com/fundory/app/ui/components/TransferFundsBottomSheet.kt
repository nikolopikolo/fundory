package com.fundory.app.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Goal
import com.fundory.app.ui.theme.FundoryGreen
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFundsBottomSheet(
    goals: List<Goal>,
    generalSavingsBalance: BigDecimal,
    initialSourceGoalId: String? = null,
    onDismiss: () -> Unit,
    onTransfer: (fromGoalId: String?, toGoalId: String?, amount: BigDecimal, note: String?) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var sourceGoalId by remember { mutableStateOf(initialSourceGoalId) }
    var destGoalId by remember {
        val defaultDest = goals.find { it.id != initialSourceGoalId }?.id
        mutableStateOf(if (initialSourceGoalId == null) defaultDest else null)
    }

    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var isSourceDropdownOpen by remember { mutableStateOf(false) }
    var isDestDropdownOpen by remember { mutableStateOf(false) }

    val availableSourceBalance = if (sourceGoalId == null) {
        generalSavingsBalance
    } else {
        goals.find { it.id == sourceGoalId }?.savedAmountGel ?: BigDecimal.ZERO
    }

    val parsedAmount = Currencies.parseAmount(amountText) ?: BigDecimal.ZERO
    val isOverdraft = parsedAmount > availableSourceBalance
    val isValid = parsedAmount > BigDecimal.ZERO && !isOverdraft && sourceGoalId != destGoalId

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(FundoryGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = FundoryGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Transfer Savings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Source Selector
            Text(text = "From (Source)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                val sourceName = if (sourceGoalId == null) "General Savings" else (goals.find { it.id == sourceGoalId }?.name ?: "Unknown")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSourceDropdownOpen = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (sourceGoalId == null) Icons.Default.AccountBalanceWallet else Icons.Default.Flag,
                                contentDescription = null,
                                tint = FundoryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sourceName, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Avail: ${Currencies.format(availableSourceBalance, "GEL")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = isSourceDropdownOpen,
                    onDismissRequest = { isSourceDropdownOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("General Savings (${Currencies.format(generalSavingsBalance, "GEL")})") },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                        onClick = {
                            sourceGoalId = null
                            isSourceDropdownOpen = false
                        }
                    )
                    goals.forEach { g ->
                        DropdownMenuItem(
                            text = { Text("${g.name} (${Currencies.format(g.savedAmountGel, "GEL")})") },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                            onClick = {
                                sourceGoalId = g.id
                                isSourceDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = FundoryGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Destination Selector
            Text(text = "To (Destination)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                val destName = if (destGoalId == null) "General Savings" else (goals.find { it.id == destGoalId }?.name ?: "Select Destination")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDestDropdownOpen = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (destGoalId == null) Icons.Default.AccountBalanceWallet else Icons.Default.Flag,
                                contentDescription = null,
                                tint = FundoryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(destName, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                DropdownMenu(
                    expanded = isDestDropdownOpen,
                    onDismissRequest = { isDestDropdownOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("General Savings") },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                        onClick = {
                            destGoalId = null
                            isDestDropdownOpen = false
                        }
                    )
                    goals.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g.name) },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                            onClick = {
                                destGoalId = g.id
                                isDestDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount to Transfer (GEL)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = isOverdraft,
                supportingText = if (isOverdraft) {
                    { Text("Exceeds available balance (${Currencies.format(availableSourceBalance, "GEL")})", color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Percentage Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(25, 50, 100).forEach { pct ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            if (availableSourceBalance > BigDecimal.ZERO) {
                                val calculated = availableSourceBalance.multiply(BigDecimal(pct))
                                    .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                                amountText = calculated.toPlainString()
                            }
                        },
                        label = { Text("$pct%") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm Button
            Button(
                onClick = {
                    if (isValid) {
                        onTransfer(sourceGoalId, destGoalId, parsedAmount, noteText.ifBlank { null })
                        onDismiss()
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = FundoryGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Transfer", fontWeight = FontWeight.Bold)
            }
        }
    }
}

