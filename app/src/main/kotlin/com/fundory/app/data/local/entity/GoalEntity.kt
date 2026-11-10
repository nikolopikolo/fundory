package com.fundory.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalStatus
import java.math.BigDecimal

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val colorHex: String,
    val targetAmountGel: BigDecimal,
    val originalTargetAmount: BigDecimal = targetAmountGel,
    val targetCurrency: String = "GEL",
    val isTargetCalculatedFromProducts: Boolean = false,
    val savedAmountGel: BigDecimal,
    val monthlyPlanGel: BigDecimal = BigDecimal.ZERO,
    val targetDateEpochMillis: Long?,
    val status: GoalStatus,
    val createdDateEpochMillis: Long,
    val lastUpdatedEpochMillis: Long
) {
    fun toDomain(): Goal = Goal(
        id = id,
        name = name,
        description = description,
        iconName = iconName,
        colorHex = colorHex,
        targetAmountGel = targetAmountGel,
        originalTargetAmount = originalTargetAmount,
        targetCurrency = targetCurrency,
        isTargetCalculatedFromProducts = isTargetCalculatedFromProducts,
        savedAmountGel = savedAmountGel,
        monthlyPlanGel = monthlyPlanGel,
        targetDateEpochMillis = targetDateEpochMillis,
        status = status,
        createdDateEpochMillis = createdDateEpochMillis,
        lastUpdatedEpochMillis = lastUpdatedEpochMillis
    )

    companion object {
        fun fromDomain(goal: Goal): GoalEntity = GoalEntity(
            id = goal.id,
            name = goal.name,
            description = goal.description,
            iconName = goal.iconName,
            colorHex = goal.colorHex,
            targetAmountGel = goal.targetAmountGel,
            originalTargetAmount = goal.originalTargetAmount,
            targetCurrency = goal.targetCurrency,
            isTargetCalculatedFromProducts = goal.isTargetCalculatedFromProducts,
            savedAmountGel = goal.savedAmountGel,
            monthlyPlanGel = goal.monthlyPlanGel,
            targetDateEpochMillis = goal.targetDateEpochMillis,
            status = goal.status,
            createdDateEpochMillis = goal.createdDateEpochMillis,
            lastUpdatedEpochMillis = goal.lastUpdatedEpochMillis
        )
    }
}
