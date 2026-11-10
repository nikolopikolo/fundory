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
    val isTargetCalculatedFromProducts: Boolean = true,
    val savedAmountGel: BigDecimal = BigDecimal.ZERO,
    val monthlyPlanGel: BigDecimal = BigDecimal("100.00"),
    val targetDateEpochMillis: Long? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdDateEpochMillis: Long = System.currentTimeMillis(),
    val lastUpdatedEpochMillis: Long = System.currentTimeMillis(),
    val products: List<Product> = emptyList()
) {
    val remainingAmountGel: BigDecimal
        get() {
            val rem = targetAmountGel.subtract(savedAmountGel)
            return if (rem < BigDecimal.ZERO) BigDecimal.ZERO else rem
        }

    val progressPercentage: Float
        get() {
            if (targetAmountGel <= BigDecimal.ZERO) return 0f
            val pct = savedAmountGel.divide(targetAmountGel, 4, RoundingMode.HALF_UP).toFloat() * 100f
            return pct.coerceIn(0f, 100f)
        }

    val isCompleted: Boolean
        get() = savedAmountGel >= targetAmountGel && targetAmountGel > BigDecimal.ZERO
}
