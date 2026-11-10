package com.fundory.app.ui.wishlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundory.app.R
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.Product
import com.fundory.app.ui.components.PriceHistoryDialog
import com.fundory.app.ui.theme.FundoryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProduct: (String) -> Unit,
    viewModel: WishlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var movingProduct by remember { mutableStateOf<Product?>(null) }
    var priceHistoryProduct by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var isSortMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Wishlist", fontWeight = FontWeight.Bold)
                        if (uiState.items.isNotEmpty()) {
                            Text(
                                text = "${uiState.items.size} items • Total ${Currencies.format(uiState.totalWishlistGel, "GEL")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { isSortMenuOpen = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest First") },
                                onClick = {
                                    viewModel.onSortOrderChanged(WishlistSort.DATE_DESC)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Price: Low to High") },
                                onClick = {
                                    viewModel.onSortOrderChanged(WishlistSort.PRICE_ASC)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Price: High to Low") },
                                onClick = {
                                    viewModel.onSortOrderChanged(WishlistSort.PRICE_DESC)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name A-Z") },
                                onClick = {
                                    viewModel.onSortOrderChanged(WishlistSort.NAME_ASC)
                                    isSortMenuOpen = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search wishlist items...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Marketplace Filters Row
            if (uiState.availableMarketplaces.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedMarketplaceFilter == null,
                            onClick = { viewModel.onMarketplaceFilterSelected(null) },
                            label = { Text("All Stores") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FundoryGreen.copy(alpha = 0.2f))
                        )
                    }
                    items(uiState.availableMarketplaces) { mp ->
                        FilterChip(
                            selected = uiState.selectedMarketplaceFilter == mp,
                            onClick = { viewModel.onMarketplaceFilterSelected(if (uiState.selectedMarketplaceFilter == mp) null else mp) },
                            label = { Text(mp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FundoryGreen.copy(alpha = 0.2f))
                        )
                    }
                }
            }

            // Product List or Empty State
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_fundory_logo),
                            contentDescription = "Fundory Emblem",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Your Wishlist is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Share links from Pinduoduo, Taobao, AliExpress or browsers directly into Fundory to plan purchases.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.items, key = { it.product.id }) { itemUi ->
                        val product = itemUi.product
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Store badge
                                    Box(
                                        modifier = Modifier
                                            .background(FundoryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = product.marketplace ?: "Store Item",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FundoryGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Action buttons
                                    Row {
                                        IconButton(onClick = { priceHistoryProduct = product }) {
                                            Icon(Icons.Default.History, contentDescription = "Price History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { movingProduct = product }) {
                                            Icon(Icons.Default.DriveFileMove, contentDescription = "Move to Goal", tint = FundoryGreen)
                                        }
                                        IconButton(onClick = { productToDelete = product }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Product Title
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Prices
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = itemUi.formattedGelPrice,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = FundoryGreen
                                        )
                                        if (product.currency != "GEL") {
                                            Text(
                                                text = "Original: ${itemUi.formattedPrice}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (product.quantity > 1) {
                                        Text(
                                            text = "Qty: ${product.quantity}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Move to Goal Dialog
    movingProduct?.let { prod ->
        AlertDialog(
            onDismissRequest = { movingProduct = null },
            title = { Text("Move to Goal") },
            text = {
                if (uiState.availableGoals.isEmpty()) {
                    Text("No active goals found. Create a goal first!")
                } else {
                    Column {
                        Text("Select a destination goal for \"${prod.name}\":")
                        Spacer(modifier = Modifier.height(12.dp))
                        uiState.availableGoals.forEach { goal ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.moveProductToGoal(prod.id, goal.id)
                                        movingProduct = null
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = goal.name,
                                    modifier = Modifier.padding(12.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = { movingProduct = null }) { Text("Cancel") }
            }
        )
    }

    // Price History Dialog
    priceHistoryProduct?.let { prod ->
        PriceHistoryDialog(
            product = prod,
            history = emptyList(), // repository updates reactively
            onDismiss = { priceHistoryProduct = null },
            onRecordNewPrice = { newPrice, note ->
                // will save through repository
            }
        )
    }

    // Delete Confirmation Dialog
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to remove \"${prod.name}\" from your wishlist?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { productToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

