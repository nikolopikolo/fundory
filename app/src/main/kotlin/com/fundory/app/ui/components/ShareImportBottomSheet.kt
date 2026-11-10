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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingBag
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
import com.fundory.app.domain.engine.SharedProductInfo
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.Product
import com.fundory.app.ui.theme.FundoryGreen
import java.math.BigDecimal
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareImportBottomSheet(
    sharedInfo: SharedProductInfo,
    activeGoals: List<Goal>,
    onDismiss: () -> Unit,
    onSaveProduct: (Product) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var title by remember { mutableStateOf(sharedInfo.title) }
    var priceText by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(sharedInfo.suggestedCurrency) }
    var quantityText by remember { mutableStateOf("1") }
    var selectedGoalId by remember { mutableStateOf<String?>(null) } // null = Wishlist
    var isCurrencyDropdownOpen by remember { mutableStateOf(false) }
    var isGoalDropdownOpen by remember { mutableStateOf(false) }

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
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = FundoryGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Import Product",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "From ${sharedInfo.marketplace}",
                            style = MaterialTheme.typography.labelSmall,
                            color = FundoryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Marketplace & URL preview card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Store: ${sharedInfo.marketplace}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sharedInfo.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Product Title") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Price & Currency Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // Currency selector
                Box(modifier = Modifier.width(110.dp)) {
                    val curr = Currencies.get(selectedCurrency)
                    OutlinedTextField(
                        value = "${curr.flagEmoji} ${curr.code}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { isCurrencyDropdownOpen = true })
                        },
                        modifier = Modifier.clickable { isCurrencyDropdownOpen = true }
                    )
                    DropdownMenu(
                        expanded = isCurrencyDropdownOpen,
                        onDismissRequest = { isCurrencyDropdownOpen = false }
                    ) {
                        Currencies.ALL.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.flagEmoji} ${c.code} (${c.symbol})") },
                                onClick = {
                                    selectedCurrency = c.code
                                    isCurrencyDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quantity
            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Destination Selector (Wishlist or Goal)
            Text(
                text = "Save To Destination",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                val selectedGoal = activeGoals.find { it.id == selectedGoalId }
                val destinationName = selectedGoal?.name ?: "Wishlist (Unallocated)"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isGoalDropdownOpen = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                if (selectedGoalId == null) Icons.Default.FavoriteBorder else Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = FundoryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = destinationName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                DropdownMenu(
                    expanded = isGoalDropdownOpen,
                    onDismissRequest = { isGoalDropdownOpen = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    DropdownMenuItem(
                        text = { Text("Wishlist (No goal yet)") },
                        leadingIcon = { Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = FundoryGreen) },
                        onClick = {
                            selectedGoalId = null
                            isGoalDropdownOpen = false
                        }
                    )
                    activeGoals.forEach { goal ->
                        DropdownMenuItem(
                            text = { Text(goal.name) },
                            leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                            onClick = {
                                selectedGoalId = goal.id
                                isGoalDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    val price = Currencies.parseAmount(priceText) ?: BigDecimal.ZERO
                    val qty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val newProduct = Product(
                        id = UUID.randomUUID().toString(),
                        goalId = selectedGoalId,
                        name = title.ifBlank { "Imported Product" },
                        description = "",
                        originalPrice = price,
                        currency = selectedCurrency,
                        quantity = qty,
                        productUrl = sharedInfo.url,
                        marketplace = sharedInfo.marketplace,
                        dateAddedEpochMillis = System.currentTimeMillis()
                    )
                    onSaveProduct(newProduct)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FundoryGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to ${if (selectedGoalId == null) "Wishlist" else "Goal"}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

