package com.fundory.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fundory.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.fundory.app.domain.model.Currencies
import com.fundory.app.ui.theme.FundoryGreen
import java.math.BigDecimal

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var mode by remember { mutableStateOf<String?>("explore") } // "goal", "savings", "explore"
    var goalName by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var goalDepositText by remember { mutableStateOf("") }
    var generalSavingsText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo & Welcome
            Image(
                painter = painterResource(id = R.drawable.ic_fundory_logo),
                contentDescription = "Fundory Emblem",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to Fundory",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Native savings, purchase planning and foreign currency tracking in Georgian Lari.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Initial Balance Status Card (showing clean 0.00 GEL)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Savings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "0.00 ₾",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FundoryGreen
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Active Goals",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Choice selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        mode = "explore"
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (mode == "explore") FundoryGreen.copy(alpha = 0.15f) else Color.Transparent
                    )
                ) {
                    Text(
                        text = "Explore",
                        fontSize = 12.sp,
                        fontWeight = if (mode == "explore") FontWeight.Bold else FontWeight.Medium,
                        color = if (mode == "explore") FundoryGreen else MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = {
                        mode = "goal"
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (mode == "goal") FundoryGreen.copy(alpha = 0.15f) else Color.Transparent
                    )
                ) {
                    Text(
                        text = "Create Goal",
                        fontSize = 12.sp,
                        fontWeight = if (mode == "goal") FontWeight.Bold else FontWeight.Medium,
                        color = if (mode == "goal") FundoryGreen else MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = {
                        mode = "savings"
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (mode == "savings") FundoryGreen.copy(alpha = 0.15f) else Color.Transparent
                    )
                ) {
                    Text(
                        text = "Add Savings",
                        fontSize = 12.sp,
                        fontWeight = if (mode == "savings") FontWeight.Bold else FontWeight.Medium,
                        color = if (mode == "savings") FundoryGreen else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Details
            AnimatedVisibility(visible = mode == "goal") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Set Up a Savings Goal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = goalName,
                            onValueChange = {
                                goalName = it
                                errorMessage = null
                            },
                            label = { Text("Goal Name") },
                            placeholder = { Text("e.g. PC Upgrade, Camera") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = targetAmountText,
                            onValueChange = {
                                targetAmountText = it
                                errorMessage = null
                            },
                            label = { Text("Target Amount (GEL)") },
                            placeholder = { Text("e.g. 500.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = goalDepositText,
                            onValueChange = { goalDepositText = it },
                            label = { Text("Initial Deposit (Optional)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = mode == "savings") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Start General Unallocated Savings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Deposit money directly into your general balance without needing a specific goal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = generalSavingsText,
                            onValueChange = {
                                generalSavingsText = it
                                errorMessage = null
                            },
                            label = { Text("Initial Savings (GEL)") },
                            placeholder = { Text("e.g. 100.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = mode == "explore") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Start with a Clean Slate",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Jump straight into Fundory. You can create goals or record savings transactions whenever you are ready.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Action Button
            Button(
                onClick = {
                    when (mode) {
                        "goal" -> {
                            val target = Currencies.parseAmount(targetAmountText)
                            if (goalName.isBlank()) {
                                errorMessage = "Please enter a goal name."
                                return@Button
                            }
                            if (target == null || target <= BigDecimal.ZERO) {
                                errorMessage = "Please enter a target amount greater than zero."
                                return@Button
                            }
                            val initial = Currencies.parseAmount(goalDepositText) ?: BigDecimal.ZERO
                            viewModel.completeOnboardingWithGoal(
                                goalName = goalName,
                                targetAmountGel = target,
                                initialDepositGel = initial,
                                onComplete = onFinish
                            )
                        }
                        "savings" -> {
                            val deposit = Currencies.parseAmount(generalSavingsText) ?: BigDecimal.ZERO
                            viewModel.completeOnboardingWithGeneralSavings(
                                initialDepositGel = deposit,
                                onComplete = onFinish
                            )
                        }
                        else -> {
                            viewModel.skipOnboarding(onFinish)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FundoryGreen)
            ) {
                Text(
                    text = when (mode) {
                        "goal" -> "Create Goal & Start"
                        "savings" -> "Deposit & Start"
                        else -> "Enter Fundory"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { viewModel.skipOnboarding(onFinish) }
            ) {
                Text(
                    text = "Skip to Dashboard",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
