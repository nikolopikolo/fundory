package com.fundory.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

data class ShippingCarrierOption(
    val carrier: String, // "Boxette", "Inex"
    val method: String, // "Air", "Ground", "Sea"
    val rateUsdPerKg: BigDecimal,
    val estimatedDays: String,
    val notes: String = ""
) {
    fun calculateCostUsd(chargeableWeightKg: Double): BigDecimal {
        return rateUsdPerKg.multiply(BigDecimal(chargeableWeightKg.coerceAtLeast(0.0)))
            .setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateCostGel(chargeableWeightKg: Double, usdToGelRate: BigDecimal): BigDecimal {
        val costUsd = calculateCostUsd(chargeableWeightKg)
        return costUsd.multiply(usdToGelRate).setScale(2, RoundingMode.HALF_UP)
    }

    companion object {
        val DEFAULT_OPTIONS = listOf(
            ShippingCarrierOption("Boxette", "Air", BigDecimal("12.90"), "7-12 days"),
            ShippingCarrierOption("Boxette", "Ground", BigDecimal("5.45"), "25-35 days"),
            ShippingCarrierOption("Inex", "Air", BigDecimal("10.50"), "8-14 days"),
            ShippingCarrierOption("Inex", "Sea", BigDecimal("4.50"), "30-45 days")
        )

        /**
         * Calculate volumetric weight in kg: (L * W * H in cm) / divisor.
         * Default air courier divisor is 5000 or 6000. We default to 6000.
         */
        fun calculateVolumetricWeightKg(
            lengthCm: Double?,
            widthCm: Double?,
            heightCm: Double?,
            divisor: Double = 6000.0
        ): Double? {
            if (lengthCm == null || widthCm == null || heightCm == null) return null
            if (lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) return null
            return (lengthCm * widthCm * heightCm) / divisor
        }
    }
}

