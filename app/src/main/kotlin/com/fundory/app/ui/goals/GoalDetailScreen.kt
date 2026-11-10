package com.fundory.app.ui.goals

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.Product
import com.fundory.app.domain.model.Transaction
import com.fundory.app.domain.model.TransactionType
import com.fundory.app.ui.components.AnimatedProgressBar
import com.fundory.app.ui.components.ConfirmationDialog
import com.fundory.app.ui.components.MoneyActionBottomSheet
import com.fundory.app.ui.theme.FundoryGreen
import com.fundory.app.ui.theme.NegativeRed
import com.fundory.app.ui.theme.WarningAmber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
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
    // Stably remember the flow to prevent recreating StateFlows on recomposition
    val detailFlow = remember(goalId) { viewModel.getGoalDetailFlow(goalId) }
    val state by detailFlow.collectAsStateWithLifecycle()

    val txFlow = remember(goalId) { viewModel.getTransactionsForGoal(goalId) }
    val transactions by txFlow.collectAsStateWithLifecycle(initialValue = emptyList())

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
                        overflow = TextOverflow.Ellipsis,
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
        if (state.isLoading && goal == null) {
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
                // Header & Goal Overview Card
                item {
                    GoalProgressDetailCard(
                        goal = goal,
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
                                text = "Wishlist Products",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val totalProductCostGel = goal.products.fold(BigDecimal.ZERO) { acc, p ->
                                acc.add(p.calculateTotalGel(rates))
                            }
                            Text(
                                text = "${goal.products.size} items • Combined: ${Currencies.format(totalProductCostGel)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { onNavigateToShippingCalculator(goalId) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp), tint = FundoryGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Calculator", fontSize = 12.sp, color = FundoryGreen)
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
                                    text = "Add items you want to purchase in CNY, USD, EUR, or GEL.",
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

                // Recent Goal Transactions Section
                if (transactions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(transactions.take(5), key = { it.id }) { tx ->
                        GoalTransactionRow(transaction = tx)
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
            message = "Remove \"${prod.name}\" from this savings goal?",
            confirmButtonText = "Remove",
            onConfirm = {
                viewModel.deleteProduct(prod)
                productToDelete = null
            },
            onDismiss = { productToDelete = null }
        )
    }

    // Money Action Sheet (properly linked to execute ViewModel calls!)
    if (isMoneySheetOpen && goal != null) {
        MoneyActionBottomSheet(
            isDeposit = isDepositAction,
            goals = listOf(goal),
            selectedGoalId = goal.id,
            onDismiss = { isMoneySheetOpen = false },
            onSubmit = { gId, amount, note ->
                val targetId = gId ?: goal.id
                if (isDepositAction) {
                    viewModel.recordDeposit(targetId, amount, note)
                } else {
                    viewModel.recordWithdrawal(targetId, amount, note)
                }
            }
        )
    }
}

@Composable
private fun GoalProgressDetailCard(
    goal: Goal,
    onAddMoney: () -> Unit,
    onWithdrawMoney: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with Icon, Name, and optional description
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val iconItem = BuiltInGoalIcons.find { it.id == goal.iconName }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(FundoryGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconItem?.icon ?: Icons.Default.Savings,
                        contentDescription = null,
                        tint = FundoryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (goal.description.isNotBlank()) {
                        Text(
                            text = goal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Saved vs Target
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
                        style = MaterialTheme.typography.headlineMedium,
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
                    if (!goal.targetCurrency.equals("GEL", ignoreCase = true)) {
                        Text(
                            text = "Original: ${Currencies.format(goal.originalTargetAmount, goal.targetCurrency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp), tint = NegativeRed)
                    Spacer(modifier = Modifier.width(6.dp))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (product.description.isNotBlank()) {
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${product.quantity}x ${Currencies.format(product.originalPrice, product.currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Currencies.format(convertedTotalGel),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FundoryGreen
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", contributionPercentage)}% of target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!product.productUrl.isNullOrBlank()) {
                    IconButton(
                        onClick = { onOpenUrl(product.productUrl) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Link",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Product",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Product",
                        modifier = Modifier.size(18.dp),
                        tint = NegativeRed
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalTransactionRow(transaction: Transaction) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = transaction.note?.ifBlank { null } ?: when (transaction.type) {
                        TransactionType.DEPOSIT -> "Deposit"
                        TransactionType.WITHDRAWAL -> "Withdrawal"
                        TransactionType.ADJUSTMENT -> "Adjustment"
                        TransactionType.TRANSFER -> "Transfer"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateFormat.format(Date(transaction.timestampEpochMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${if (transaction.isPositive) "+" else "-"}${Currencies.format(transaction.amountGel)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isPositive) FundoryGreen else NegativeRed
            )
        }
    }
}
