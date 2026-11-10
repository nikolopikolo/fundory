package com.fundory.app

import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class CurrencyConversionTest {

    private val ratesToGel = mapOf(
        "GEL" to BigDecimal("1.00"),
        "USD" to BigDecimal("2.594"),
        "EUR" to BigDecimal("2.90"),
        "CNY" to BigDecimal("0.388"),
        "GBP" to BigDecimal("3.48")
    )

    @Test
    fun testCurrencyFormatting() {
        val gelAmount = BigDecimal("1234.50")
        val formattedGel = Currencies.format(gelAmount, "GEL", includeCode = true)
        assertEquals("1,234.50 GEL", formattedGel)

        val formattedWithSymbol = Currencies.format(gelAmount, "GEL", includeCode = false)
        assertEquals("1,234.50 ₾", formattedWithSymbol)

        val usdAmount = BigDecimal("50.00")
        val formattedUsd = Currencies.format(usdAmount, "USD", includeCode = true)
        assertEquals("50.00 USD", formattedUsd)
    }

    @Test
    fun testPrecisionDecimalCalculations() {
        // Floating point 0.1 + 0.2 != 0.3, but BigDecimal must be exact
        val val1 = BigDecimal("0.10")
        val val2 = BigDecimal("0.20")
        val sum = val1.add(val2)
        assertEquals(BigDecimal("0.30"), sum)

        val parsed = Currencies.parseAmount("₾ 1,234.56")
        assertNotNull(parsed)
        assertEquals(BigDecimal("1234.56"), parsed)
    }

    @Test
    fun testProductConversionToGel() {
        // 1,100 CNY * 0.388 = 426.80 GEL
        val cpuBundle = Product(
            id = "prod_cpu",
            goalId = "goal_pc",
            name = "CPU + Motherboard",
            originalPrice = BigDecimal("1100.00"),
            currency = "CNY",
            quantity = 1
        )

        val gel = cpuBundle.calculateTotalGel(ratesToGel)
        assertEquals(BigDecimal("426.80"), gel)

        // 15 USD * 2.594 = 38.91 GEL
        val shipping = Product(
            id = "prod_ship",
            goalId = "goal_pc",
            name = "Shipping",
            originalPrice = BigDecimal("15.00"),
            currency = "USD",
            quantity = 1
        )
        val shipGel = shipping.calculateTotalGel(ratesToGel)
        assertEquals(BigDecimal("38.91"), shipGel)
    }

    @Test
    fun testManualOverridePrecedence() {
        // Auto converted would be 100 USD * 2.594 = 259.40 GEL
        // But user manually overrides to 240.00 GEL
        val productWithOverride = Product(
            id = "prod_override",
            goalId = "goal_1",
            name = "Custom Import",
            originalPrice = BigDecimal("100.00"),
            currency = "USD",
            manualOverrideGel = BigDecimal("240.00")
        )

        val totalGel = productWithOverride.calculateTotalGel(ratesToGel)
        assertEquals(BigDecimal("240.00"), totalGel)
        assertTrue(productWithOverride.isManuallyOverridden)
    }
}

