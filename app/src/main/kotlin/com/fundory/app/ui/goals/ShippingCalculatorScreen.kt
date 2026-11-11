package com.fundory.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.ImportCalculationConfig
import com.fundory.app.ui.theme.FundoryGreen
import com.fundory.app.ui.theme.FundoryGreenLight
import com.fundory.app.ui.theme.WarningAmber
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShippingCalculatorScreen(
    goalId: String?,
    onNavigateBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rates = state.ratesToGel

    // Purchase Source: true = Local Purchase (Georgia), false = International Online Purchase
    var isLocalPurchase by remember { mutableStateOf(false) }

    // Product Cost inputs
    var productPriceText by remember { mutableStateOf("") }
    var productCurrency by remember { mutableStateOf("USD") }
    var isCurrencyMenuOpen by remember { mutableStateOf(false) }

    // Shipping inputs (all start empty or zero, no fabricated 1.5kg or 31 GEL!)
    var manualShippingCostText by remember { mutableStateOf("") }
    var shippingCurrency by remember { mutableStateOf("USD") }
    var weightKgText by remember { mutableStateOf("") }
    var ratePerKgText by remember { mutableStateOf("") }
    var carrierName by remember { mutableStateOf("Boxette") }
    var carrierMethod by remember { mutableStateOf("Air") }

    // Apply Costs to Goal state
    var isApplySheetOpen by remember { mutableStateOf(false) }
    var selectedTargetGoalId by remember(goalId) { mutableStateOf(goalId) }
    var parcelNameText by remember { mutableStateOf("") }
    val allGoals by viewModel.allGoalsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Import VAT & Customs Toggle
    var applyImportVatAndCustoms by remember { mutableStateOf(true) }

    // Optional Fees (both OFF by default!)
    var applyCardFee by remember { mutableStateOf(false) }
    var cardFeePercentText by remember { mutableStateOf("2.0") }

    var applyFxFee by remember { mutableStateOf(false) }
    var fxFeePercentText by remember { mutableStateOf("1.5") }

    // Live currency calculations
    val parsedProductPrice = Currencies.parseAmount(productPriceText) ?: BigDecimal.ZERO
    val productRate = rates[productCurrency.uppercase()] ?: BigDecimal.ONE
    val productsSubtotalGel = remember(parsedProductPrice, productCurrency, productRate, isLocalPurchase) {
        if (isLocalPurchase) parsedProductPrice else parsedProductPrice.multiply(productRate).setScale(2, RoundingMode.HALF_UP)
    }

    val parsedManualShipping = Currencies.parseAmount(manualShippingCostText) ?: BigDecimal.ZERO
    val parsedWeightKg = weightKgText.toDoubleOrNull() ?: 0.0
    val parsedRatePerKg = Currencies.parseAmount(ratePerKgText) ?: BigDecimal.ZERO
    val parsedCardFeePercent = if (applyCardFee) (cardFeePercentText.toDoubleOrNull() ?: 0.0) else 0.0
    val parsedFxFeePercent = if (applyFxFee) (fxFeePercentText.toDoubleOrNull() ?: 0.0) else 0.0

    val config = remember(
        isLocalPurchase,
        parsedWeightKg,
        parsedRatePerKg,
        parsedManualShipping,
        shippingCurrency,
        applyImportVatAndCustoms,
        applyCardFee,
        parsedCardFeePercent,
        applyFxFee,
        parsedFxFeePercent
    ) {
        ImportCalculationConfig(
            isLocalPurchase = isLocalPurchase,
            weightKg = parsedWeightKg,
            ratePerKg = parsedRatePerKg,
            fixedShippingFee = parsedManualShipping,
            shippingCurrency = shippingCurrency,
            applyCardTransactionFee = applyCardFee,
            transactionFeePercent = parsedCardFeePercent,
            applyCurrencyConversionFee = applyFxFee,
            currencyConversionFeePercent = parsedFxFeePercent,
            applyImportVatAndCustoms = applyImportVatAndCustoms,
            isGeorgianImportTaxMode = true,
            isCustomsExempt = !applyImportVatAndCustoms
        )
    }

    val result = remember(productsSubtotalGel, config, rates) {
        viewModel.calculateImportCost(productsSubtotalGel, config, rates)
    }

    val additionalVatGel = if (result.isTaxableUnderCustoms) result.estimatedVatGel else BigDecimal.ZERO
    val additionalCustomsGel = if (result.isTaxableUnderCustoms) result.customsDeclarationFeeGel else BigDecimal.ZERO
    val additionalOtherFeesGel = result.transactionFeesGel.add(result.currencyConversionFeesGel)
    val additionalCostsGel = result.shippingTotalGel
        .add(additionalVatGel)
        .add(additionalCustomsGel)
        .add(additionalOtherFeesGel)
        .setScale(2, RoundingMode.HALF_UP)

    val declaredValueGel = productsSubtotalGel.add(result.shippingTotalGel)
    val exceedsThreshold = declaredValueGel > config.customsThresholdGel

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Purchase & Import Calculator", fontWeight = FontWeight.Bold)
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
            // Purchase Source Segmented Control
            Text(
                text = "Purchase Source",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = !isLocalPurchase,
                    onClick = { isLocalPurchase = false },
                    label = { Text("International Online") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundoryGreen.copy(alpha = 0.2f),
                        selectedLabelColor = FundoryGreenLight
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = isLocalPurchase,
                    onClick = { isLocalPurchase = true },
                    label = { Text("Local Purchase (Georgia)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FundoryGreen.copy(alpha = 0.2f),
                        selectedLabelColor = FundoryGreenLight
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Product Price Card
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
                        text = if (isLocalPurchase) "Product Price (in GEL)" else "Product Price & Currency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = productPriceText,
                            onValueChange = { productPriceText = it },
                            label = { Text("Price") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (!isLocalPurchase) {
                            Box(
                                modifier = Modifier
                                    .height(56.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                                    .clickable { isCurrencyMenuOpen = true }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = productCurrency,
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
                                            text = { Text(code) },
                                            onClick = {
                                                productCurrency = code
                                                isCurrencyMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!isLocalPurchase && !productCurrency.equals("GEL", ignoreCase = true) && parsedProductPrice > BigDecimal.ZERO) {
                        Text(
                            text = "Converted: ${Currencies.format(productsSubtotalGel)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FundoryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Shipping Cost Inputs
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
                        text = if (isLocalPurchase) "Local Delivery Cost (Optional)" else "International Shipping",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = manualShippingCostText,
                        onValueChange = { manualShippingCostText = it },
                        label = { Text(if (isLocalPurchase) "Local Delivery (GEL)" else "Flat Shipping Cost (USD)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (!isLocalPurchase) {
                        Text(
                            text = "Or calculate by parcel weight & courier rate:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = weightKgText,
                                onValueChange = { weightKgText = it },
                                label = { Text("Weight (kg)") },
                                placeholder = { Text("e.g. 0.5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = ratePerKgText,
                                onValueChange = { ratePerKgText = it },
                                label = { Text("Rate ($/kg)") },
                                placeholder = { Text("e.g. 8.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // China to Georgia Carrier Comparison
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "China to Georgia Carriers:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FundoryGreen
                        )

                        val carrierRates = listOf(
                            Triple("Boxette Air", "12.90", "7-12 days"),
                            Triple("Boxette Ground", "5.45", "25-35 days"),
                            Triple("Inex Air", "10.50", "8-14 days"),
                            Triple("Inex Sea", "4.50", "30-45 days")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val usdRate = rates["USD"] ?: BigDecimal("2.70")
                            val weight = weightKgText.toDoubleOrNull() ?: 1.0

                            carrierRates.forEach { (carrier, rateStr, eta) ->
                                val rateVal = BigDecimal(rateStr)
                                val costUsd = rateVal.multiply(BigDecimal(weight)).setScale(2, RoundingMode.HALF_UP)
                                val costGel = costUsd.multiply(usdRate).setScale(2, RoundingMode.HALF_UP)
                                val isSelected = ratePerKgText == rateStr

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            ratePerKgText = rateStr
                                            manualShippingCostText = ""
                                            val parts = carrier.split(" ")
                                            carrierName = parts.firstOrNull() ?: "Courier"
                                            carrierMethod = parts.getOrNull(1) ?: "Air"
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) FundoryGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(carrier, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            Text("$$rateStr/kg • $eta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${Currencies.format(costGel, "GEL")}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = FundoryGreen)
                                            Text("$$costUsd", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Import VAT & Customs Toggle (Only for international purchases)
            if (!isLocalPurchase) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Apply Import VAT & Customs Fees",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Applies 18% VAT & 20 ₾ declaration fee if declared base > 300 ₾",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = applyImportVatAndCustoms,
                                onCheckedChange = { applyImportVatAndCustoms = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FundoryGreen)
                            )
                        }

                        if (exceedsThreshold) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Declared CIF (${Currencies.format(declaredValueGel)}) exceeds 300 ₾ threshold.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarningAmber
                                )
                            }
                        }
                    }
                }
            }

            // Optional Fees (Both OFF by default!)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Optional Fees (Disabled by Default)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Card Transaction Fee Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Card Transaction Fee", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Bank payment processing fee", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = applyCardFee,
                            onCheckedChange = { applyCardFee = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FundoryGreen)
                        )
                    }

                    if (applyCardFee) {
                        OutlinedTextField(
                            value = cardFeePercentText,
                            onValueChange = { cardFeePercentText = it },
                            label = { Text("Fee Percentage (%)") },
                            placeholder = { Text("2.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Currency Conversion Fee Toggle (Only for international purchases)
                    if (!isLocalPurchase) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Currency Conversion Fee", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Card issuer FX margin spread", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = applyFxFee,
                                onCheckedChange = { applyFxFee = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FundoryGreen)
                            )
                        }

                        if (applyFxFee) {
                            OutlinedTextField(
                                value = fxFeePercentText,
                                onValueChange = { fxFeePercentText = it },
                                label = { Text("FX Margin Percentage (%)") },
                                placeholder = { Text("1.5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Calculation Breakdown Card (Every nonzero value has a clear identifiable source!)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Calculation Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    BreakdownLine("Product Cost", Currencies.format(result.productsSubtotalGel))

                    if (result.shippingTotalGel > BigDecimal.ZERO) {
                        BreakdownLine("Shipping", Currencies.format(result.shippingTotalGel))
                    }

                    if (result.isTaxableUnderCustoms) {
                        BreakdownLine("Import VAT (18%)", Currencies.format(result.estimatedVatGel))
                        BreakdownLine("Customs Declaration Fee", Currencies.format(result.customsDeclarationFeeGel))
                    } else if (!isLocalPurchase && !applyImportVatAndCustoms) {
                        Text(
                            text = "Import taxes excluded by user preference",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (result.transactionFeesGel > BigDecimal.ZERO) {
                        BreakdownLine("Card Transaction Fee", Currencies.format(result.transactionFeesGel))
                    }

                    if (result.currencyConversionFeesGel > BigDecimal.ZERO) {
                        BreakdownLine("Currency Conversion Fee", Currencies.format(result.currencyConversionFeesGel))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Estimated Cost",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Currencies.format(result.grandTotalGel),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = FundoryGreen
                        )
                    }
                }
            }

            // Apply Costs to Goal Button
            Button(
                onClick = {
                    if (parcelNameText.isBlank()) {
                        parcelNameText = if (isLocalPurchase) "Local Delivery" else "$carrierName $carrierMethod Parcel"
                    }
                    if (selectedTargetGoalId == null) {
                        selectedTargetGoalId = allGoals.firstOrNull()?.id
                    }
                    isApplySheetOpen = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FundoryGreen)
            ) {
                Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Apply Costs to Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Apply Costs to Goal Bottom Sheet
    if (isApplySheetOpen) {
        val targetGoal = allGoals.firstOrNull { it.id == selectedTargetGoalId }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { isApplySheetOpen = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Apply Estimated Costs to Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Associate this shipping & import cost estimate with a savings goal. The estimated expenses will be added to the goal's total funding requirement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Select Destination Goal
                Text("Select Destination Goal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                if (allGoals.isEmpty()) {
                    Text("No goals found. Create a savings goal first.", color = WarningAmber, style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allGoals.forEach { g ->
                            val isSelected = g.id == selectedTargetGoalId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTargetGoalId = g.id },
                                label = { Text(g.name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FundoryGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = FundoryGreenLight
                                )
                            )
                        }
                    }
                }

                // Parcel Name Input
                OutlinedTextField(
                    value = parcelNameText,
                    onValueChange = { parcelNameText = it },
                    label = { Text("Parcel / Estimate Name") },
                    placeholder = { Text("e.g. CPU Air Parcel, Boxette Delivery") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Cost Breakdown Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Additional Expenses Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        BreakdownLine("Shipping", Currencies.format(result.shippingTotalGel))
                        if (additionalVatGel > BigDecimal.ZERO) {
                            BreakdownLine("Import VAT (18%)", Currencies.format(additionalVatGel))
                        }
                        if (additionalCustomsGel > BigDecimal.ZERO) {
                            BreakdownLine("Customs Declaration Fee", Currencies.format(additionalCustomsGel))
                        }
                        if (additionalOtherFeesGel > BigDecimal.ZERO) {
                            BreakdownLine("Card & FX Fees", Currencies.format(additionalOtherFeesGel))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        BreakdownLine("Total Additional Expenses", Currencies.format(additionalCostsGel))
                    }
                }

                // Goal Impact Summary
                if (targetGoal != null) {
                    val currentBaseTarget = targetGoal.targetAmountGel
                    val existingAdditional = targetGoal.totalAdditionalCostsGel
                    val newTotalRequired = currentBaseTarget.add(existingAdditional).add(additionalCostsGel)
                    val remaining = newTotalRequired.subtract(targetGoal.savedAmountGel).let { if (it < BigDecimal.ZERO) BigDecimal.ZERO else it }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Goal Target Impact: ${targetGoal.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            BreakdownLine("Base Product Target", Currencies.format(currentBaseTarget))
                            BreakdownLine("Applied Additional Costs", "+ ${Currencies.format(existingAdditional.add(additionalCostsGel))}")
                            BreakdownLine("New Total Required", Currencies.format(newTotalRequired))
                            BreakdownLine("Saved Amount (Unchanged)", Currencies.format(targetGoal.savedAmountGel))
                            BreakdownLine("Remaining to Save", Currencies.format(remaining))
                        }
                    }
                }

                // Confirm Action Button
                Button(
                    onClick = {
                        val destinationId = selectedTargetGoalId
                        if (destinationId != null) {
                            val pName = parcelNameText.ifBlank {
                                if (isLocalPurchase) "Local Delivery" else "$carrierName $carrierMethod Parcel"
                            }
                            viewModel.applyCalculationToGoal(
                                goalId = destinationId,
                                parcelName = pName,
                                carrier = if (isLocalPurchase) "Local" else carrierName,
                                method = if (isLocalPurchase) "Ground" else carrierMethod,
                                weightKg = parsedWeightKg,
                                shippingCostGel = result.shippingTotalGel,
                                vatGel = additionalVatGel,
                                customsFeeGel = additionalCustomsGel,
                                otherFeesGel = additionalOtherFeesGel,
                                productSubtotalGel = productsSubtotalGel,
                                onComplete = {
                                    isApplySheetOpen = false
                                    onNavigateBack()
                                }
                            )
                        }
                    },
                    enabled = selectedTargetGoalId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FundoryGreen)
                ) {
                    Text("Confirm & Apply to Goal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BreakdownLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
