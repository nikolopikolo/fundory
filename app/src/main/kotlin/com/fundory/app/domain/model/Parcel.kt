package com.fundory.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class Parcel(
    val id: String = UUID.randomUUID().toString(),
    val goalId: String? = null,
    val name: String,
    val carrier: String, // "Boxette", "Inex", "Custom"
    val method: String, // "Air", "Ground", "Sea"
    val weightKg: Double,
    val lengthCm: Double? = null,
    val widthCm: Double? = null,
    val heightCm: Double? = null,
    val shippingCostGel: BigDecimal = BigDecimal.ZERO,
    val vatGel: BigDecimal = BigDecimal.ZERO,
    val customsFeeGel: BigDecimal = BigDecimal.ZERO,
    val otherFeesGel: BigDecimal = BigDecimal.ZERO,
    val productSubtotalGel: BigDecimal = BigDecimal.ZERO,
    val isConfirmed: Boolean = false,
    val trackingNumber: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    val totalCostGel: BigDecimal
        get() = shippingCostGel
            .add(vatGel)
            .add(customsFeeGel)
            .add(otherFeesGel)
            .setScale(2, RoundingMode.HALF_UP)
}

