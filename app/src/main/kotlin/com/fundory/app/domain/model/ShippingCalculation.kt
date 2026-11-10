package com.fundory.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

data class ImportCalculationConfig(
    val shippingCompanyName: String = "USA2GEORGIA / Camex",
    val shippingMethod: String = "Air Freight",
    val weightKg: Double = 1.0,
    val ratePerKg: BigDecimal = BigDecimal("8.00"), // 8 USD / kg standard
    val shippingCurrency: String = "USD",
    val fixedShippingFee: BigDecimal = BigDecimal.ZERO,
    val transactionFeePercent: Double = 2.0, // 2% card/bank transaction fee
    val currencyConversionFeePercent: Double = 1.5, // 1.5% FX margin
    val isGeorgianImportTaxMode: Boolean = true,
    val customsThresholdGel: BigDecimal = BigDecimal("300.00"),
    val vatRatePercent: Double = 18.0, // 18% VAT
    val customsDeclarationFeeGel: BigDecimal = BigDecimal("20.00"),
    val isCustomsExempt: Boolean = false
)

data class ImportCalculationResult(
    val productsSubtotalGel: BigDecimal,
    val shippingTotalGel: BigDecimal,
    val transactionFeesGel: BigDecimal,
    val currencyConversionFeesGel: BigDecimal,
    val isTaxableUnderCustoms: Boolean,
    val estimatedVatGel: BigDecimal,
    val customsDeclarationFeeGel: BigDecimal,
    val totalTaxesAndFeesGel: BigDecimal,
    val grandTotalGel: BigDecimal,
    val breakdownExplanation: String
)
