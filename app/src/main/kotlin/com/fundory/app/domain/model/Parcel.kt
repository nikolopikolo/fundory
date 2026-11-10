package com.fundory.app.domain.model

import java.math.BigDecimal
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
    val isConfirmed: Boolean = false,
    val trackingNumber: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

