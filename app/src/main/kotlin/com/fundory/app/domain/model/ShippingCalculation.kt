package com.fundory.app.domain.model

import java.math.BigDecimal

data class ImportCalculationConfig(
    val isLocalPurchase: Boolean = false,
    val shippingCompanyName: String = "Carrier / Courier",
    val shippingMethod: String = "Standard",
    val weightKg: Double = 0.0,
    val ratePerKg: BigDecimal = BigDecimal.ZERO,
    val shippingCurrency: String = "USD",
    val fixedShippingFee: BigDecimal = BigDecimal.ZERO,
    val applyCardTransactionFee: Boolean = false,
    val transactionFeePercent: Double = 0.0,
    val applyCurrencyConversionFee: Boolean = false,
    val currencyConversionFeePercent: Double = 0.0,
    val isGeorgianImportTaxMode: Boolean = true,
    val applyImportVatAndCustoms: Boolean = true,
    val customsThresholdGel: BigDecimal = BigDecimal("300.00"),
    val vatRatePercent: Double = 18.0,
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
