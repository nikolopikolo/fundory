package com.fundory.app.ui.goals

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Product
import com.fundory.app.ui.theme.FundoryGreen
import com.fundory.app.ui.theme.FundoryGreenLight
import com.fundory.app.ui.theme.NegativeRed
import com.fundory.app.ui.theme.WarningAmber
import java.math.BigDecimal
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    goalId: String,
    productId: String?,
    onNavigateBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rates = state.ratesToGel

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var originalPriceText by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("USD") }
    var quantity by remember { mutableIntStateOf(1) }
    var productUrl by remember { mutableStateOf("") }
    var marketplace by remember { mutableStateOf("") }
    var shippingCostText by remember { mutableStateOf("0.00") }
    var shippingCurrency by remember { mutableStateOf("USD") }
    var taxesAndFeesText by remember { mutableStateOf("0.00") }
    var taxesCurrency by remember { mutableStateOf("USD") }
    var isManualOverride by remember { mutableStateOf(false) }
    var manualOverrideText by remember { mutableStateOf("") }
    var purchaseStatus by remember { mutableStateOf(com.fundory.app.domain.model.PurchaseStatus.NOT_PURCHASED) }
    var shippingCarrier by remember { mutableStateOf("") }
    var trackingNumber by remember { mutableStateOf("") }
    var trackingNotes by remember { mutableStateOf("") }
    var alternativeGroupId by remember { mutableStateOf("") }
    var isSelectedAlternative by remember { mutableStateOf(true) }
    var isStatusMenuOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    val isEditMode = productId != null

    LaunchedEffect(productId) {
        if (productId != null && !isLoaded) {
            val prod = viewModel.getProductById(productId)
            if (prod != null) {
                name = prod.name
                description = prod.description
                originalPriceText = prod.originalPrice.toPlainString()
                selectedCurrency = prod.currency
                quantity = prod.quantity
                productUrl = prod.productUrl ?: ""
                marketplace = prod.marketplace ?: ""
                shippingCostText = prod.shippingCost.toPlainString()
                shippingCurrency = prod.shippingCurrency
                taxesAndFeesText = prod.taxesAndFees.toPlainString()
                taxesCurrency = prod.taxesCurrency
                purchaseStatus = prod.purchaseStatus
                shippingCarrier = prod.shippingCarrier ?: ""
                trackingNumber = prod.trackingNumber ?: ""
                trackingNotes = prod.trackingNotes ?: ""
                alternativeGroupId = prod.alternativeGroupId ?: ""
                isSelectedAlternative = prod.isSelectedAlternative
                if (prod.manualOverrideGel != null) {
                    isManualOverride = true
                    manualOverrideText = prod.manualOverrideGel.toPlainString()
                }
            }
            isLoaded = true
        }
    }

    // Dynamic Converted GEL Price Preview
    val parsedOriginal = Currencies.parseAmount(originalPriceText) ?: BigDecimal.ZERO
    val parsedShipping = Currencies.parseAmount(shippingCostText) ?: BigDecimal.ZERO
    val parsedTaxes = Currencies.parseAmount(taxesAndFeesText) ?: BigDecimal.ZERO
    val parsedOverride = if (isManualOverride) Currencies.parseAmount(manualOverrideText) else null

    val previewProduct = remember(
        name, parsedOriginal, selectedCurrency, quantity,
        parsedShipping, shippingCurrency, parsedTaxes, taxesCurrency, parsedOverride
    ) {
        Product(
            id = "preview",
            goalId = goalId,
            name = name,
            originalPrice = parsedOriginal,
            currency = selectedCurrency,
            quantity = quantity,
            shippingCost = parsedShipping,
            shippingCurrency = shippingCurrency,
            taxesAndFees = parsedTaxes,
            taxesCurrency = taxesCurrency,
            manualOverrideGel = parsedOverride
        )
    }

    val previewTotalGel = previewProduct.calculateTotalGel(rates)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Product" else "Add Product",
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
            // Live Converted Total Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CONVERTED TOTAL",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Currencies.format(previewTotalGel),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = FundoryGreen
                        )
                    }

                    if (isManualOverride) {
                        Box(
                            modifier = Modifier
                                .background(WarningAmber.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Manual Override",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                        }
                    } else {
                        val rate = rates[selectedCurrency.uppercase()] ?: BigDecimal.ONE
                        Text(
                            text = "1 $selectedCurrency ≈ ${rate.setScale(3, java.math.RoundingMode.HALF_UP)} GEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Product Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Product Name") },
                placeholder = { Text("e.g. CPU + Motherboard, RAM 32GB, Guitar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Currency Selector Chips
            Text(
                text = "Currency",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Currencies.ALL) { curr ->
                    val isSelected = curr.code == selectedCurrency
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCurrency = curr.code
                            shippingCurrency = curr.code
                            taxesCurrency = curr.code
                        },
                        label = { Text("${curr.flagEmoji} ${curr.code}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FundoryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = FundoryGreenLight
                        )
                    )
                }
            }

            // Original Price & Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = originalPriceText,
                    onValueChange = {
                        originalPriceText = it
                        errorMessage = null
                    },
                    label = { Text("Price ($selectedCurrency)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = quantity.toString(),
                    onValueChange = {
                        val q = it.toIntOrNull()
                        if (q != null && q in 1..999) quantity = q
                    },
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.7f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Shipping Cost
            OutlinedTextField(
                value = shippingCostText,
                onValueChange = { shippingCostText = it },
                label = { Text("Shipping Cost ($shippingCurrency)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Taxes & Fees
            OutlinedTextField(
                value = taxesAndFeesText,
                onValueChange = { taxesAndFeesText = it },
                label = { Text("Taxes & Fees ($taxesCurrency)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Product URL
            OutlinedTextField(
                value = productUrl,
                onValueChange = { productUrl = it },
                label = { Text("Product URL / Store Link (Optional)") },
                placeholder = { Text("https://...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Manual Override Toggle & Field
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isManualOverride = !isManualOverride }
            ) {
                Checkbox(
                    checked = isManualOverride,
                    onCheckedChange = { isManualOverride = it },
                    colors = CheckboxDefaults.colors(checkedColor = FundoryGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Manually Override Converted GEL Value",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Fix a specific total cost in GEL regardless of live rates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isManualOverride) {
                OutlinedTextField(
                    value = manualOverrideText,
                    onValueChange = { manualOverrideText = it },
                    label = { Text("Overridden Total in GEL") },
                    placeholder = { Text("e.g. 250.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ORDER TRACKING SECTION
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Order & Delivery Tracking",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Purchase Status Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = purchaseStatus.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Purchase Status") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { isStatusMenuOpen = true })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isStatusMenuOpen = true },
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = isStatusMenuOpen,
                    onDismissRequest = { isStatusMenuOpen = false }
                ) {
                    com.fundory.app.domain.model.PurchaseStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.displayName) },
                            onClick = {
                                purchaseStatus = status
                                isStatusMenuOpen = false
                            }
                        )
                    }
                }
            }

            if (purchaseStatus != com.fundory.app.domain.model.PurchaseStatus.NOT_PURCHASED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = shippingCarrier,
                        onValueChange = { shippingCarrier = it },
                        label = { Text("Carrier (e.g. Boxette, Inex)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = trackingNumber,
                        onValueChange = { trackingNumber = it },
                        label = { Text("Tracking #") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = trackingNotes,
                    onValueChange = { trackingNotes = it },
                    label = { Text("Delivery Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ALTERNATIVE PRODUCT COMPARISON SECTION
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Product Alternatives",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = alternativeGroupId,
                onValueChange = { alternativeGroupId = it },
                label = { Text("Alternative Group ID (e.g. 'CPU_Choice')") },
                placeholder = { Text("Leave empty if not an alternative") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (alternativeGroupId.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set as active alternative",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Only the active choice contributes to goal cost",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isSelectedAlternative,
                        onCheckedChange = { isSelectedAlternative = it }
                    )
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

            // Save Product Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter a product name."
                        return@Button
                    }
                    val original = Currencies.parseAmount(originalPriceText)
                    if (original == null || original <= BigDecimal.ZERO) {
                        errorMessage = "Please enter a valid original price."
                        return@Button
                    }

                    val shipCost = Currencies.parseAmount(shippingCostText) ?: BigDecimal.ZERO
                    val taxCost = Currencies.parseAmount(taxesAndFeesText) ?: BigDecimal.ZERO
                    val overrideGel = if (isManualOverride) Currencies.parseAmount(manualOverrideText) else null

                    val product = Product(
                        id = productId ?: UUID.randomUUID().toString(),
                        goalId = goalId,
                        name = name.trim(),
                        description = description.trim(),
                        originalPrice = original,
                        currency = selectedCurrency.uppercase(),
                        quantity = quantity,
                        productUrl = productUrl.trim().ifBlank { null },
                        marketplace = marketplace.trim().ifBlank { null },
                        shippingCost = shipCost,
                        shippingCurrency = shippingCurrency.uppercase(),
                        taxesAndFees = taxCost,
                        taxesCurrency = taxesCurrency.uppercase(),
                        manualOverrideGel = overrideGel,
                        purchaseStatus = purchaseStatus,
                        shippingCarrier = shippingCarrier.trim().ifBlank { null },
                        trackingNumber = trackingNumber.trim().ifBlank { null },
                        trackingNotes = trackingNotes.trim().ifBlank { null },
                        alternativeGroupId = alternativeGroupId.trim().ifBlank { null },
                        isSelectedAlternative = isSelectedAlternative
                    )

                    viewModel.saveProduct(product)
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FundoryGreen)
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "Add to Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

