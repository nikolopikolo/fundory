package com.fundory.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.ui.theme.FundoryGreen
import com.fundory.app.ui.theme.FundoryGreenLight
import com.fundory.app.ui.theme.GoalColors
import com.fundory.app.ui.theme.NegativeRed
import java.math.BigDecimal
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoalScreen(
    goalId: String?,
    onNavigateBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isTargetFromProducts by remember { mutableStateOf(true) }
    var manualTargetText by remember { mutableStateOf("") }
    var initialSavedText by remember { mutableStateOf("0.00") }
    var monthlyPlanText by remember { mutableStateOf("100.00") }
    var selectedColorHex by remember { mutableStateOf(GoalColors.first()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    val isEditMode = goalId != null

    LaunchedEffect(goalId) {
        if (goalId != null && !isLoaded) {
            // Load existing goal
            val existing = viewModel.uiState.value.goals.find { it.id == goalId }
            if (existing != null) {
                name = existing.name
                description = existing.description
                isTargetFromProducts = existing.isTargetCalculatedFromProducts
                manualTargetText = existing.targetAmountGel.toPlainString()
                initialSavedText = existing.savedAmountGel.toPlainString()
                monthlyPlanText = existing.monthlyPlanGel.toPlainString()
                selectedColorHex = existing.colorHex
            }
            isLoaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Goal" else "New Savings Goal",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Goal Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Goal Name") },
                placeholder = { Text("e.g. PC Upgrade, New Guitar, Vacation") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("What is this goal for?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            // Target calculation mode toggle
            Text(
                text = "Target Amount Method",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isTargetFromProducts,
                    onClick = { isTargetFromProducts = true },
                    label = { Text("Calculate from products") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundoryGreen.copy(alpha = 0.2f),
                        selectedLabelColor = FundoryGreenLight
                    )
                )
                FilterChip(
                    selected = !isTargetFromProducts,
                    onClick = { isTargetFromProducts = false },
                    label = { Text("Manual Target") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundoryGreen.copy(alpha = 0.2f),
                        selectedLabelColor = FundoryGreenLight
                    )
                )
            }

            if (!isTargetFromProducts) {
                OutlinedTextField(
                    value = manualTargetText,
                    onValueChange = { manualTargetText = it },
                    label = { Text("Target Amount (GEL)") },
                    placeholder = { Text("702.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                Text(
                    text = "Target will update automatically whenever products in foreign currencies (CNY, USD, etc.) or GEL are added or edited.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // If creating new, allow entering initial saved balance
            if (!isEditMode) {
                OutlinedTextField(
                    value = initialSavedText,
                    onValueChange = { initialSavedText = it },
                    label = { Text("Current Saved Balance (GEL)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Monthly Savings Plan
            OutlinedTextField(
                value = monthlyPlanText,
                onValueChange = { monthlyPlanText = it },
                label = { Text("Planned Monthly Contribution (GEL)") },
                placeholder = { Text("100.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Accent Color Picker
            Text(
                text = "Accent Color",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(GoalColors) { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = hex == selectedColorHex
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(color, CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = NegativeRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Goal Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter a name for your savings goal."
                        return@Button
                    }

                    val target = if (!isTargetFromProducts) {
                        val parsed = Currencies.parseAmount(manualTargetText)
                        if (parsed == null || parsed <= BigDecimal.ZERO) {
                            errorMessage = "Please enter a valid target amount greater than zero."
                            return@Button
                        }
                        parsed
                    } else {
                        BigDecimal.ZERO // Will be calculated from products
                    }

                    val saved = Currencies.parseAmount(initialSavedText) ?: BigDecimal.ZERO
                    val monthlyPlan = Currencies.parseAmount(monthlyPlanText) ?: BigDecimal("100.00")

                    val newGoal = Goal(
                        id = goalId ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        description = description.trim(),
                        colorHex = selectedColorHex,
                        targetAmountGel = target,
                        isTargetCalculatedFromProducts = isTargetFromProducts,
                        savedAmountGel = saved,
                        monthlyPlanGel = monthlyPlan,
                        status = GoalStatus.ACTIVE
                    )

                    viewModel.saveGoal(newGoal)
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FundoryGreen)
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "Create Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

