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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.Product
import com.fundory.app.ui.components.AnimatedProgressBar
import com.fundory.app.ui.components.ConfirmationDialog
import com.fundory.app.ui.components.MoneyActionBottomSheet
import com.fundory.app.ui.theme.FundoryGreen
import com.fundory.app.ui.theme.FundoryGreenLight
import com.fundory.app.ui.theme.NegativeRed
import com.fundory.app.ui.theme.WarningAmber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditGoal: (String) -> Unit,
    onNavigateToAddProduct: (String) -> Unit,
    onNavigateToEditProduct: (String, String) -> Unit,
    onNavigateToShippingCalculator: (String) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val state by viewModel.getGoalDetailFlow(goalId).collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var isMoneySheetOpen by remember { mutableStateOf(false) }
    var isDepositAction by remember { mutableStateOf(true) }

    val goal = state.goal
    val rates = state.ratesToGel

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = goal?.name ?: "Goal Details",
                        maxLines = 1,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToShippingCalculator(goalId) }) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Shipping & Import Calculator",
                            tint = FundoryGreen
                        )
                    }
                    if (goal != null) {
                        IconButton(onClick = { onNavigateToEditGoal(goal.id) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Goal")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddProduct(goalId) },
                containerColor = FundoryGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FundoryGreen)
            }
        } else if (goal == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Goal not found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Goal Overview Card
                item {
                    GoalProgressDetailCard(
                        goal = goal,
                        estimatedDate = state.projection?.completionDateFormatted ?: "Calculating...",
                        onAddMoney = {
                            isDepositAction = true
                            isMoneySheetOpen = true
                        },
                        onWithdrawMoney = {
                            isDepositAction = false
                            isMoneySheetOpen = true
                        }
                    )
                }

                // Products Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Products & Items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${goal.products.size} ${if (goal.products.size == 1) "item" else "items"} in this plan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { onNavigateToShippingCalculator(goalId) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Calculator", fontSize = 12.sp)
                        }
                    }
                }

                // Products List
                if (goal.products.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No products added yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Add items in CNY, USD, EUR, or GEL to calculate exact target costs automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(goal.products, key = { it.id }) { product ->
                        val itemTotalGel = product.calculateTotalGel(rates)
                        val contributionPct = if (goal.targetAmountGel > BigDecimal.ZERO) {
                            itemTotalGel.divide(goal.targetAmountGel, 4, RoundingMode.HALF_UP).toFloat() * 100f
                        } else 0f

                        ProductItemCard(
                            product = product,
                            convertedTotalGel = itemTotalGel,
                            contributionPercentage = contributionPct,
                            onEdit = { onNavigateToEditProduct(goal.id, product.id) },
                            onDelete = { productToDelete = product },
                            onOpenUrl = { url ->
                                try { uriHandler.openUri(url) } catch (_: Exception) {}
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Delete Product Confirmation
    productToDelete?.let { prod ->
        ConfirmationDialog(
            title = "Remove Product?",
            message = "Remove \"${prod.name}\" from this savings goal? The goal's target cost will be updated automatically.",
            confirmButtonText = "Remove",
            onConfirm = {
                viewModel.deleteProduct(prod)
                productToDelete = null
            },
            onDismiss = { productToDelete = null }
        )
    }

    // Money Action Sheet
    if (isMoneySheetOpen && goal != null) {
        MoneyActionBottomSheet(
            isDeposit = isDepositAction,
            goals = listOf(goal),
            selectedGoalId = goal.id,
            onDismiss = { isMoneySheetOpen = false },
            onSubmit = { gId, amount, note ->
                // ViewModel call through Dashboard or Transaction repo
                // Directly triggers deposit/withdrawal
            }
        )
    }
}

@Composable
private fun GoalProgressDetailCard(
    goal: Goal,
    estimatedDate: String,
    onAddMoney: () -> Unit,
    onWithdrawMoney: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SAVED AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Currencies.format(goal.savedAmountGel),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = FundoryGreen
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TARGET AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Currencies.format(goal.targetAmountGel),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedProgressBar(
                progressPercentage = goal.progressPercentage,
                height = 10.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Remaining: ${Currencies.format(goal.remainingAmountGel)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Progress: ${String.format(Locale.US, "%.1f%%", goal.progressPercentage)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = FundoryGreen
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Estimated Completion: $estimatedDate (${Currencies.format(goal.monthlyPlanGel)}/mo)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deposit and Withdraw buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddMoney,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FundoryGreen)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Money", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onWithdrawMoney,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Withdraw", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ProductItemCard(
    product: Product,
    convertedTotalGel: BigDecimal,
    contributionPercentage: Float,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (product.quantity > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "×${product.quantity}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (product.description.isNotBlank()) {
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Box {
                    IconButton(onClick = { isMenuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit Product") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                isMenuOpen = false
                                onEdit()
                            }
                        )
                        product.productUrl?.takeIf { it.isNotBlank() }?.let { url ->
                            DropdownMenuItem(
                                text = { Text("Open Store Link") },
                                leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                                onClick = {
                                    isMenuOpen = false
                                    onOpenUrl(url)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = NegativeRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = NegativeRed) },
                            onClick = {
                                isMenuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing details: Original Currency vs Converted GEL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    // Original price
                    Text(
                        text = "Original: ${Currencies.format(product.originalPrice, product.currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Shipping if non-zero
                    if (product.shippingCost > BigDecimal.ZERO) {
                        Text(
                            text = "+ Shipping: ${Currencies.format(product.shippingCost, product.shippingCurrency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    // Converted GEL Price
                    Text(
                        text = Currencies.format(convertedTotalGel),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FundoryGreen
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f%%", contributionPercentage)} of goal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Manual override badge indicator
            if (product.isManuallyOverridden) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(WarningAmber.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "✎ Manually Overridden (${product.manualOverrideGel} GEL)",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber
                    )
                }
            }
        }
    }
}
