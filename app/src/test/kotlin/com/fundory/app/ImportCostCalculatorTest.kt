package com.fundory.app

import com.fundory.app.domain.engine.ImportCostCalculator
import com.fundory.app.domain.model.ImportCalculationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ImportCostCalculatorTest {

    private lateinit var calculator: ImportCostCalculator
    private val ratesToGel = mapOf(
        "GEL" to BigDecimal("1.00"),
        "USD" to BigDecimal("2.50"),
        "EUR" to BigDecimal("2.80")
    )

    @Before
    fun setUp() {
        calculator = ImportCostCalculator()
    }

    @Test
    fun testSubThresholdImportExemptFromVat() {
        // Items: 200 GEL, Shipping: 1 kg @ 8 USD ($8 * 2.50 = 20 GEL)
        // CIF total = 220 GEL <= 300 GEL threshold
        val config = ImportCalculationConfig(
            weightKg = 1.0,
            ratePerKg = BigDecimal("8.00"),
            fixedShippingFee = BigDecimal.ZERO,
            shippingCurrency = "USD",
            customsThresholdGel = BigDecimal("300.00"),
            vatRatePercent = 18.0,
            customsDeclarationFeeGel = BigDecimal("20.00"),
            transactionFeePercent = 0.0,
            currencyConversionFeePercent = 0.0,
            isGeorgianImportTaxMode = true,
            isCustomsExempt = false
        )

        val result = calculator.calculate(
            productsSubtotalGel = BigDecimal("200.00"),
            config = config,
            ratesToGel = ratesToGel
        )

        assertEquals(BigDecimal("200.00"), result.productsSubtotalGel)
        assertEquals(BigDecimal("20.00"), result.shippingTotalGel)
        assertFalse(result.isTaxableUnderCustoms)
        assertEquals(BigDecimal("0.00"), result.estimatedVatGel)
        assertEquals(BigDecimal("0.00"), result.customsDeclarationFeeGel)
        assertEquals(BigDecimal("0.00"), result.totalTaxesAndFeesGel)
        assertEquals(BigDecimal("220.00"), result.grandTotalGel)
    }

    @Test
    fun testOverThresholdImportAppliesVatAndDeclarationFee() {
        // Items: 350 GEL, Shipping: 2 kg @ 10 USD ($20 * 2.50 = 50 GEL)
        // CIF = 350 + 50 = 400 GEL > 300 GEL threshold
        // VAT = 18% of 400 = 72.00 GEL
        // Customs declaration fee = 20.00 GEL
        val config = ImportCalculationConfig(
            weightKg = 2.0,
            ratePerKg = BigDecimal("10.00"),
            fixedShippingFee = BigDecimal.ZERO,
            shippingCurrency = "USD",
            customsThresholdGel = BigDecimal("300.00"),
            vatRatePercent = 18.0,
            customsDeclarationFeeGel = BigDecimal("20.00"),
            transactionFeePercent = 0.0,
            currencyConversionFeePercent = 0.0,
            isGeorgianImportTaxMode = true,
            isCustomsExempt = false
        )

        val result = calculator.calculate(
            productsSubtotalGel = BigDecimal("350.00"),
            config = config,
            ratesToGel = ratesToGel
        )

        assertEquals(BigDecimal("350.00"), result.productsSubtotalGel)
        assertEquals(BigDecimal("50.00"), result.shippingTotalGel)
        assertTrue(result.isTaxableUnderCustoms)
        assertEquals(BigDecimal("72.00"), result.estimatedVatGel)
        assertEquals(BigDecimal("20.00"), result.customsDeclarationFeeGel)
        assertEquals(BigDecimal("92.00"), result.totalTaxesAndFeesGel)
        assertEquals(BigDecimal("492.00"), result.grandTotalGel)
    }

    @Test
    fun testExemptionOverridesThreshold() {
        val config = ImportCalculationConfig(
            weightKg = 1.0,
            ratePerKg = BigDecimal("10.00"),
            shippingCurrency = "USD",
            customsThresholdGel = BigDecimal("300.00"),
            vatRatePercent = 18.0,
            customsDeclarationFeeGel = BigDecimal("20.00"),
            isGeorgianImportTaxMode = true,
            isCustomsExempt = true // Explicitly marked exempt
        )

        val result = calculator.calculate(
            productsSubtotalGel = BigDecimal("600.00"),
            config = config,
            ratesToGel = ratesToGel
        )

        assertFalse(result.isTaxableUnderCustoms)
        assertEquals(BigDecimal("0.00"), result.estimatedVatGel)
        assertEquals(BigDecimal("0.00"), result.customsDeclarationFeeGel)
    }

    @Test
    fun testTransactionAndCurrencyConversionFees() {
        // Products: 1000.00 GEL, Shipping: 0.00 GEL
        // Transaction fee: 2% of 1000 = 20.00 GEL
        // FX fee: 1.5% of 1000 = 15.00 GEL
        // VAT: 18% of 1000 = 180.00 GEL
        // Customs declaration fee: 20.00 GEL
        // Total taxes & fees: 20 + 15 + 180 + 20 = 235.00 GEL
        // Grand total: 1000 + 235 = 1235.00 GEL
        val config = ImportCalculationConfig(
            weightKg = 0.0,
            ratePerKg = BigDecimal.ZERO,
            fixedShippingFee = BigDecimal.ZERO,
            customsThresholdGel = BigDecimal("300.00"),
            vatRatePercent = 18.0,
            customsDeclarationFeeGel = BigDecimal("20.00"),
            transactionFeePercent = 2.0,
            currencyConversionFeePercent = 1.5,
            isGeorgianImportTaxMode = true,
            isCustomsExempt = false
        )

        val result = calculator.calculate(
            productsSubtotalGel = BigDecimal("1000.00"),
            config = config,
            ratesToGel = ratesToGel
        )

        assertEquals(BigDecimal("20.00"), result.transactionFeesGel)
        assertEquals(BigDecimal("15.00"), result.currencyConversionFeesGel)
        assertEquals(BigDecimal("180.00"), result.estimatedVatGel)
        assertEquals(BigDecimal("20.00"), result.customsDeclarationFeeGel)
        assertEquals(BigDecimal("235.00"), result.totalTaxesAndFeesGel)
        assertEquals(BigDecimal("1235.00"), result.grandTotalGel)
    }
}
