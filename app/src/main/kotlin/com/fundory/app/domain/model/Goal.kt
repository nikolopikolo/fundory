package com.fundory.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}

data class Goal(
    val id: String,
    val name: String,
    val description: String = "",
    val iconName: String = "savings",
    val colorHex: String = "#38B273",
    val targetAmountGel: BigDecimal,
    val originalTargetAmount: BigDecimal = targetAmountGel,
    val targetCurrency: String = "GEL",
    val isTargetCalculatedFromProducts: Boolean = false,
    val savedAmountGel: BigDecimal = BigDecimal.ZERO,
    val monthlyPlanGel: BigDecimal = BigDecimal.ZERO,
    val targetDateEpochMillis: Long? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdDateEpochMillis: Long = System.currentTimeMillis(),
    val lastUpdatedEpochMillis: Long = System.currentTimeMillis(),
    val products: List<Product> = emptyList(),
    val parcels: List<Parcel> = emptyList()
) {
    val totalShippingGel: BigDecimal
        get() = parcels.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.shippingCostGel) }.setScale(2, RoundingMode.HALF_UP)

    val totalVatGel: BigDecimal
        get() = parcels.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.vatGel) }.setScale(2, RoundingMode.HALF_UP)

    val totalCustomsFeeGel: BigDecimal
        get() = parcels.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.customsFeeGel) }.setScale(2, RoundingMode.HALF_UP)

    val totalOtherFeesGel: BigDecimal
        get() = parcels.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.otherFeesGel) }.setScale(2, RoundingMode.HALF_UP)

    val totalAdditionalCostsGel: BigDecimal
        get() = parcels.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalCostGel) }.setScale(2, RoundingMode.HALF_UP)

    val totalRequiredFundingGel: BigDecimal
        get() = targetAmountGel.add(totalAdditionalCostsGel).setScale(2, RoundingMode.HALF_UP)

    val remainingAmountGel: BigDecimal
        get() {
            val rem = totalRequiredFundingGel.subtract(savedAmountGel)
            return (if (rem < BigDecimal.ZERO) BigDecimal.ZERO else rem).setScale(2, RoundingMode.HALF_UP)
        }

    val progressPercentage: Float
        get() {
            if (totalRequiredFundingGel <= BigDecimal.ZERO) return 0f
            val pct = savedAmountGel.divide(totalRequiredFundingGel, 4, RoundingMode.HALF_UP).toFloat() * 100f
            return pct.coerceIn(0f, 100f)
        }

    val isCompleted: Boolean
        get() = savedAmountGel >= totalRequiredFundingGel && totalRequiredFundingGel > BigDecimal.ZERO
}
