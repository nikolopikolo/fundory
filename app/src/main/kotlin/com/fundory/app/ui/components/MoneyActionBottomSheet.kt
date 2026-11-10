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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.fundory.app.ui.theme.NegativeRed
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyActionBottomSheet(
    isDeposit: Boolean, // true = Add Money, false = Withdraw Money
    goals: List<Goal>,
    generalSavingsBalance: BigDecimal = BigDecimal.ZERO,
    selectedGoalId: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onSubmit: (goalId: String?, amount: BigDecimal, note: String?) -> Unit
) {
    // Determine default destination:
    // If selectedGoalId is passed, use it.
    // If exactly 1 goal exists and no explicit goalId passed, default to that goal.
    // If 0 goals exist, default to null (General Savings).
    var chosenGoalId by remember(selectedGoalId, goals) {
        mutableStateOf(
            if (selectedGoalId != null) {
                selectedGoalId
            } else if (goals.size == 1) {
                goals.first().id
            } else {
                null // General Savings
            }
        )
    }

    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isGoalDropdownExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val activeGoal = if (chosenGoalId != null) goals.find { it.id == chosenGoalId } else null
    val destinationName = if (chosenGoalId == null) "General Savings" else (activeGoal?.name ?: "Unknown Goal")
    val availableBalance = if (chosenGoalId == null) generalSavingsBalance else (activeGoal?.savedAmountGel ?: BigDecimal.ZERO)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isDeposit) FundoryGreen.copy(alpha = 0.15f) else NegativeRed.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDeposit) Icons.Default.Add else Icons.Default.Remove,
                        contentDescription = null,
                        tint = if (isDeposit) FundoryGreen else NegativeRed,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isDeposit) "Add Money" else "Withdraw Money",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isDeposit) "Record deposit to $destinationName" else "Withdraw funds from $destinationName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Destination Selector (Only when goals exist, allows choosing General Savings or Goal)
            if (goals.isNotEmpty()) {
                Text(
                    text = if (isDeposit) "Deposit Destination" else "Withdraw Source",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { isGoalDropdownExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (chosenGoalId == null) Icons.Default.AccountBalanceWallet else Icons.Default.Flag,
                                contentDescription = null,
                                tint = FundoryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = destinationName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (chosenGoalId == null) {
                                        "Available: ${Currencies.format(generalSavingsBalance)}"
                                    } else {
                                        "Saved: ${Currencies.format(activeGoal?.savedAmountGel ?: BigDecimal.ZERO)} / Target: ${Currencies.format(activeGoal?.targetAmountGel ?: BigDecimal.ZERO)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = isGoalDropdownExpanded,
                        onDismissRequest = { isGoalDropdownExpanded = false }
                    ) {
                        // General Savings option
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = FundoryGreen, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("General Savings", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Available: ${Currencies.format(generalSavingsBalance)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                chosenGoalId = null
                                isGoalDropdownExpanded = false
                            }
                        )

                        // Goal options
                        goals.forEach { g ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Flag, contentDescription = null, tint = FundoryGreen, modifier = Modifier.size(18.dp))
                                        Column {
                                            Text(g.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "Saved: ${Currencies.format(g.savedAmountGel)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    chosenGoalId = g.id
                                    isGoalDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // When 0 goals exist, clearly inform user it's going to General Savings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = FundoryGreen, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = "General Savings Balance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Current balance: ${Currencies.format(generalSavingsBalance)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("Amount (GEL)") },
                placeholder = { Text("0.00") },
                leadingIcon = {
                    Text(
                        text = "₾",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDeposit) FundoryGreen else NegativeRed
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick preset chips (+20, +50, +100, +200)
            if (isDeposit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("20", "50", "100", "200").forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    amountText = preset
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$preset ₾",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FundoryGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Optional Note Input
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note (Optional)") },
                placeholder = { Text(if (isDeposit) "e.g. Salary, Gift" else "e.g. Purchase, Withdrawal") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Error display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = NegativeRed
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    val amount = Currencies.parseAmount(amountText)
                    if (amount == null || amount <= BigDecimal.ZERO) {
                        errorMessage = "Please enter a valid amount greater than zero."
                        return@Button
                    }

                    if (!isDeposit && availableBalance < amount) {
                        errorMessage = "Cannot withdraw ${Currencies.format(amount)}: current balance in $destinationName is only ${Currencies.format(availableBalance)}."
                        return@Button
                    }

                    isSubmitting = true
                    onSubmit(chosenGoalId, amount, noteText.ifBlank { null })
                    onDismiss()
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDeposit) FundoryGreen else NegativeRed
                )
            ) {
                Text(
                    text = if (isDeposit) "Confirm Deposit" else "Confirm Withdrawal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
