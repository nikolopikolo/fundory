package com.fundory.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShippingCalculatorScreen(
    goalId: String?,
    onNavigateBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rates = state.ratesToGel

    var productsSubtotalText by remember { mutableStateOf("350.00") }
    var weightKgText by remember { mutableStateOf("1.5") }
    var ratePerKgText by remember { mutableStateOf("8.00") }
    var shippingCurrency by remember { mutableStateOf("USD") }
    var fixedShippingFeeText by remember { mutableStateOf("0.00") }
    var txFeePercentText by remember { mutableStateOf("2.0") }
    var fxFeePercentText by remember { mutableStateOf("1.5") }
    var isGeorgianTaxMode by remember { mutableStateOf(true) }
    var isCustomsExempt by remember { mutableStateOf(false) }

    val productsSubtotalGel = Currencies.parseAmount(productsSubtotalText) ?: BigDecimal.ZERO
    val weightKg = weightKgText.toDoubleOrNull() ?: 1.0
    val ratePerKg = Currencies.parseAmount(ratePerKgText) ?: BigDecimal("8.00")
    val fixedFee = Currencies.parseAmount(fixedShippingFeeText) ?: BigDecimal.ZERO
    val txPercent = txFeePercentText.toDoubleOrNull() ?: 2.0
    val fxPercent = fxFeePercentText.toDoubleOrNull() ?: 1.5

    val config = remember(weightKg, ratePerKg, shippingCurrency, fixedFee, txPercent, fxPercent, isGeorgianTaxMode, isCustomsExempt) {
        ImportCalculationConfig(
            weightKg = weightKg,
            ratePerKg = ratePerKg,
            shippingCurrency = shippingCurrency,
            fixedShippingFee = fixedFee,
            transactionFeePercent = txPercent,
            currencyConversionFeePercent = fxPercent,
            isGeorgianImportTaxMode = isGeorgianTaxMode,
            isCustomsExempt = isCustomsExempt
        )
    }

    val result = remember(productsSubtotalGel, config, rates) {
        viewModel.calculateImportCost(productsSubtotalGel, config, rates)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Import & Customs Calculator",
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
            // Grand Total Result Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ESTIMATED GRAND TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Currencies.format(result.grandTotalGel),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = FundoryGreen
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Breakdown lines
                    BreakdownLine("Products Subtotal", Currencies.format(result.productsSubtotalGel))
                    BreakdownLine("International Shipping", Currencies.format(result.shippingTotalGel))
                    BreakdownLine("Card & Transaction Fees (${config.transactionFeePercent}%)", Currencies.format(result.transactionFeesGel))
                    BreakdownLine("Currency Conversion Fees (${config.currencyConversionFeePercent}%)", Currencies.format(result.currencyConversionFeesGel))

                    if (result.isTaxableUnderCustoms) {
                        BreakdownLine("18% Import VAT", Currencies.format(result.estimatedVatGel), isHighlight = true)
                        BreakdownLine("Customs Declaration Fee", Currencies.format(result.customsDeclarationFeeGel), isHighlight = true)
                    } else {
                        BreakdownLine("Customs Import Tax", "0.00 GEL (Under 300 GEL threshold)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = result.breakdownExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Products Subtotal Input
            OutlinedTextField(
                value = productsSubtotalText,
                onValueChange = { productsSubtotalText = it },
                label = { Text("Items Subtotal (GEL)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Weight & Shipping Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = weightKgText,
                    onValueChange = { weightKgText = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = ratePerKgText,
                    onValueChange = { ratePerKgText = it },
                    label = { Text("Rate / kg ($shippingCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Shipping Currency Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("USD", "EUR", "CNY", "GEL").forEach { curr ->
                    val isSelected = curr == shippingCurrency
                    FilterChip(
                        selected = isSelected,
                        onClick = { shippingCurrency = curr },
                        label = { Text(curr) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FundoryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = FundoryGreenLight
                        )
                    )
                }
            }

            // Georgian Customs Tax Mode Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isGeorgianTaxMode,
                    onCheckedChange = { isGeorgianTaxMode = it },
                    colors = CheckboxDefaults.colors(checkedColor = FundoryGreen)
                )
                Column {
                    Text(
                        text = "Georgian Import Customs & VAT (18%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Orders exceeding 300 GEL (Items + Freight) are subject to 18% VAT and 20 GEL customs declaration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BreakdownLine(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isHighlight) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) WarningAmber else MaterialTheme.colorScheme.onSurface
        )
    }
}

