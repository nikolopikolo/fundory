package com.fundory.app.domain.model

import java.math.BigDecimal

enum class ProjectionScenario(val label: String, val multiplier: Double) {
    CONSERVATIVE("Conservative", 0.6),
    EXPECTED("Expected", 1.0),
    OPTIMISTIC("Optimistic", 1.4)
}

data class ProjectedMonthPoint(
    val monthLabel: String, // e.g. "Dec 2026"
    val timestampEpochMillis: Long,
    val balanceGel: BigDecimal,
    val isTargetReached: Boolean
)

data class FutureBonus(
    val id: String,
    val name: String, // e.g. "Birthday Gift", "Year-End Bonus"
    val amountGel: BigDecimal,
    val scheduledDateEpochMillis: Long
)

data class GoalProjectionResult(
    val goalId: String,
    val scenario: ProjectionScenario,
    val monthlyContributionGel: BigDecimal,
    val targetAmountGel: BigDecimal,
    val currentSavedGel: BigDecimal,
    val remainingAmountGel: BigDecimal,
    val remainingMonths: Int,
    val estimatedCompletionDateEpochMillis: Long?,
    val completionDateFormatted: String,
    val timeline: List<ProjectedMonthPoint>,
    val explanationText: String
)
