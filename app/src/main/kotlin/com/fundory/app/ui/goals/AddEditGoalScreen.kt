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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.ui.theme.FundoryGreen
import com.fundory.app.ui.theme.FundoryGreenLight
import com.fundory.app.ui.theme.GoalColors
import com.fundory.app.ui.theme.NegativeRed
import com.fundory.app.ui.theme.WarningAmber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class GoalIconItem(val id: String, val label: String, val icon: ImageVector)

val BuiltInGoalIcons = listOf(
    GoalIconItem("savings", "Savings", Icons.Default.Savings),
    GoalIconItem("computer", "PC / Tech", Icons.Default.Computer),
    GoalIconItem("smartphone", "Phone", Icons.Default.Smartphone),
    GoalIconItem("camera", "Camera", Icons.Default.PhotoCamera),
    GoalIconItem("car", "Car", Icons.Default.DirectionsCar),
    GoalIconItem("travel", "Travel", Icons.Default.Flight),
    GoalIconItem("gaming", "Gaming", Icons.Default.SportsEsports),
    GoalIconItem("education", "Education", Icons.Default.School),
    GoalIconItem("home", "Home", Icons.Default.Home),
    GoalIconItem("shopping", "Shopping", Icons.Default.ShoppingBag),
    GoalIconItem("music", "Music", Icons.Default.MusicNote),
    GoalIconItem("target", "Target", Icons.Default.TrackChanges)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoalScreen(
    goalId: String?,
    onNavigateBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rates = state.ratesToGel

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIconId by remember { mutableStateOf("savings") }
    var targetAmountText by remember { mutableStateOf("") }
    var targetCurrency by remember { mutableStateOf("GEL") }
    var initialDepositText by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(GoalColors.first()) }
    var existingGoal by remember { mutableStateOf<Goal?>(null) }
    var isCurrencyMenuOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isDataLoaded by remember { mutableStateOf(false) }

    val isEditMode = goalId != null

    // Load existing goal data directly from repository
    LaunchedEffect(goalId) {
        if (goalId != null && !isDataLoaded) {
            val goal = viewModel.getGoalById(goalId)
            if (goal != null) {
                existingGoal = goal
                name = goal.name
                description = goal.description
                selectedIconId = goal.iconName
                targetAmountText = goal.originalTargetAmount.toPlainString()
                targetCurrency = goal.targetCurrency
                selectedColorHex = goal.colorHex
            }
            isDataLoaded = true
        }
    }

    // Live converted target in GEL
    val parsedTarget = Currencies.parseAmount(targetAmountText) ?: BigDecimal.ZERO
    val conversionRate = rates[targetCurrency.uppercase()] ?: BigDecimal.ONE
    val convertedTargetGel = remember(parsedTarget, targetCurrency, rates) {
        if (targetCurrency.equals("GEL", ignoreCase = true)) {
            parsedTarget
        } else {
            parsedTarget.multiply(conversionRate).setScale(2, RoundingMode.HALF_UP)
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
            // 1. Goal Name (Required)
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Goal Name *") },
                placeholder = { Text("e.g. PC Upgrade, New Guitar, Vacation") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 2. Description (Optional)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("What is this goal for?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            // 3. Goal Icon Picker
            Text(
                text = "Goal Icon",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(BuiltInGoalIcons) { item ->
                    val isSelected = selectedIconId == item.id
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (isSelected) FundoryGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer,
                                shape = CircleShape
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) FundoryGreen else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .clickable { selectedIconId = item.id },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) FundoryGreenLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 4. Target Amount & 5. Target Currency
            Text(
                text = "Target Amount & Currency *",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = targetAmountText,
                    onValueChange = {
                        targetAmountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                // Currency Dropdown
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                        .clickable { isCurrencyMenuOpen = true }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = targetCurrency,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FundoryGreen
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FundoryGreen)
                    }

                    DropdownMenu(
                        expanded = isCurrencyMenuOpen,
                        onDismissRequest = { isCurrencyMenuOpen = false }
                    ) {
                        Currencies.supportedCodes.forEach { code ->
                            DropdownMenuItem(
                                text = {
                                    val curr = Currencies.get(code)
                                    Text("$code (${curr?.symbol ?: ""}) - ${curr?.name ?: ""}")
                                },
                                onClick = {
                                    targetCurrency = code
                                    isCurrencyMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            // Converted amount indicator if foreign currency
            if (!targetCurrency.equals("GEL", ignoreCase = true) && parsedTarget > BigDecimal.ZERO) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Converted Target in GEL:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "≈ ${Currencies.format(convertedTargetGel)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FundoryGreen
                        )
                    }
                }
            }

            // 6. Current Savings / Initial Balance
            if (!isEditMode) {
                OutlinedTextField(
                    value = initialDepositText,
                    onValueChange = { initialDepositText = it },
                    label = { Text("Initial Savings Balance (Optional)") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = "₾",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                // In edit mode: show current saved amount as readonly reassurance
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Saved Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Currencies.format(existingGoal?.savedAmountGel ?: BigDecimal.ZERO),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FundoryGreen
                        )
                    }
                }
            }

            // Color Accent Picker
            Text(
                text = "Accent Color",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(GoalColors) { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(color, CircleShape)
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

            // Error Message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = NegativeRed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Save / Create Goal Button
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    val trimmedName = name.trim()
                    if (trimmedName.isBlank()) {
                        errorMessage = "Please enter a goal name."
                        return@Button
                    }

                    if (parsedTarget <= BigDecimal.ZERO) {
                        errorMessage = "Please enter a target amount greater than zero."
                        return@Button
                    }

                    isSubmitting = true
                    val now = System.currentTimeMillis()

                    if (isEditMode && existingGoal != null) {
                        val updated = existingGoal!!.copy(
                            name = trimmedName,
                            description = description.trim(),
                            iconName = selectedIconId,
                            colorHex = selectedColorHex,
                            targetAmountGel = convertedTargetGel,
                            originalTargetAmount = parsedTarget,
                            targetCurrency = targetCurrency,
                            lastUpdatedEpochMillis = now
                        )
                        viewModel.saveGoal(updated)
                    } else {
                        val initialDeposit = Currencies.parseAmount(initialDepositText) ?: BigDecimal.ZERO
                        val newGoal = Goal(
                            id = UUID.randomUUID().toString(),
                            name = trimmedName,
                            description = description.trim(),
                            iconName = selectedIconId,
                            colorHex = selectedColorHex,
                            targetAmountGel = convertedTargetGel,
                            originalTargetAmount = parsedTarget,
                            targetCurrency = targetCurrency,
                            isTargetCalculatedFromProducts = false,
                            savedAmountGel = initialDeposit,
                            monthlyPlanGel = BigDecimal.ZERO,
                            status = GoalStatus.ACTIVE,
                            createdDateEpochMillis = now,
                            lastUpdatedEpochMillis = now
                        )
                        viewModel.createGoalWithInitialDeposit(newGoal, initialDeposit)
                    }

                    onNavigateBack()
                },
                enabled = !isSubmitting,
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
