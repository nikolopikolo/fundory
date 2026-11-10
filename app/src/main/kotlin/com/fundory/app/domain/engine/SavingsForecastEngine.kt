package com.fundory.app.domain.engine

import com.fundory.app.domain.model.FutureBonus
import com.fundory.app.domain.model.GoalProjectionResult
import com.fundory.app.domain.model.ProjectedMonthPoint
import com.fundory.app.domain.model.ProjectionScenario
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SavingsForecastEngine {

    private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.US)

    /**
     * Projects future savings based on current balance, target, monthly plan, and optional one-time future deposits.
     */
    fun calculateProjection(
        goalId: String,
        targetAmountGel: BigDecimal,
        currentSavedGel: BigDecimal,
        monthlyPlanGel: BigDecimal,
        scenario: ProjectionScenario = ProjectionScenario.EXPECTED,
        futureBonuses: List<FutureBonus> = emptyList(),
        startCalendar: Calendar = Calendar.getInstance()
    ): GoalProjectionResult {
        val remaining = if (targetAmountGel > currentSavedGel) {
            targetAmountGel.subtract(currentSavedGel)
        } else {
            BigDecimal.ZERO
        }

        // Apply scenario multiplier (e.g. 0.6 for conservative, 1.0 for expected, 1.4 for optimistic)
        val adjustedMonthly = monthlyPlanGel
            .multiply(BigDecimal.valueOf(scenario.multiplier))
            .setScale(2, RoundingMode.HALF_UP)

        val timeline = mutableListOf<ProjectedMonthPoint>()
        var runningBalance = currentSavedGel
        val simCal = startCalendar.clone() as Calendar

        // Starting point (Today / current month)
        timeline.add(
            ProjectedMonthPoint(
                monthLabel = monthFormat.format(simCal.time),
                timestampEpochMillis = simCal.timeInMillis,
                balanceGel = runningBalance.setScale(2, RoundingMode.HALF_UP),
                isTargetReached = runningBalance >= targetAmountGel && targetAmountGel > BigDecimal.ZERO
            )
        )

        if (targetAmountGel <= BigDecimal.ZERO || runningBalance >= targetAmountGel) {
            return GoalProjectionResult(
                goalId = goalId,
                scenario = scenario,
                monthlyContributionGel = adjustedMonthly,
                targetAmountGel = targetAmountGel,
                currentSavedGel = currentSavedGel,
                remainingAmountGel = BigDecimal.ZERO,
                remainingMonths = 0,
                estimatedCompletionDateEpochMillis = simCal.timeInMillis,
                completionDateFormatted = "Target Reached!",
                timeline = timeline,
                explanationText = "Goal target has already been achieved."
            )
        }

        if (adjustedMonthly <= BigDecimal.ZERO && futureBonuses.isEmpty()) {
            return GoalProjectionResult(
                goalId = goalId,
                scenario = scenario,
                monthlyContributionGel = BigDecimal.ZERO,
                targetAmountGel = targetAmountGel,
                currentSavedGel = currentSavedGel,
                remainingAmountGel = remaining,
                remainingMonths = -1,
                estimatedCompletionDateEpochMillis = null,
                completionDateFormatted = "Indefinite (No Monthly Savings)",
                timeline = timeline,
                explanationText = "With 0.00 GEL monthly savings and no future bonuses, the target cannot be projected."
            )
        }

        var monthsCount = 0
        var completionDateMillis: Long? = null
        var completionDateFormatted = "Beyond 10 Years"

        // Project month-by-month for up to 120 months (10 years maximum forecast horizon)
        val maxMonths = 120
        while (monthsCount < maxMonths && runningBalance < targetAmountGel) {
            simCal.add(Calendar.MONTH, 1)
            monthsCount++

            // Add standard monthly contribution
            runningBalance = runningBalance.add(adjustedMonthly)

            // Add any future one-time scheduled bonuses falling within this month
            val monthStartMillis = simCal.timeInMillis
            val nextMonthCal = (simCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            val monthEndMillis = nextMonthCal.timeInMillis

            val bonusesThisMonth = futureBonuses.filter {
                it.scheduledDateEpochMillis in monthStartMillis until monthEndMillis
            }
            for (bonus in bonusesThisMonth) {
                runningBalance = runningBalance.add(bonus.amountGel)
            }

            val isReached = runningBalance >= targetAmountGel
            timeline.add(
                ProjectedMonthPoint(
                    monthLabel = monthFormat.format(simCal.time),
                    timestampEpochMillis = simCal.timeInMillis,
                    balanceGel = runningBalance.setScale(2, RoundingMode.HALF_UP),
                    isTargetReached = isReached
                )
            )

            if (isReached && completionDateMillis == null) {
                completionDateMillis = simCal.timeInMillis
                completionDateFormatted = monthFormat.format(simCal.time)
            }
        }

        val explanation = when (scenario) {
            ProjectionScenario.CONSERVATIVE ->
                "Conservative projection assumes saving ${adjustedMonthly} GEL/month (60% of planned ${monthlyPlanGel} GEL) to safely anticipate unforeseen expenses."
            ProjectionScenario.EXPECTED ->
                "Expected projection assumes maintaining your exact planned monthly savings of ${monthlyPlanGel} GEL/month consistently."
            ProjectionScenario.OPTIMISTIC ->
                "Optimistic projection assumes saving ${adjustedMonthly} GEL/month (140% of planned ${monthlyPlanGel} GEL), such as during high-saving months or overtime."
        }

        return GoalProjectionResult(
            goalId = goalId,
            scenario = scenario,
            monthlyContributionGel = adjustedMonthly,
            targetAmountGel = targetAmountGel,
            currentSavedGel = currentSavedGel,
            remainingAmountGel = remaining,
            remainingMonths = if (completionDateMillis != null) monthsCount else -1,
            estimatedCompletionDateEpochMillis = completionDateMillis,
            completionDateFormatted = completionDateFormatted,
            timeline = timeline,
            explanationText = explanation
        )
    }

    /**
     * Calculates the required monthly contribution needed to reach the target amount by a specific deadline.
     */
    fun calculateRequiredMonthlyContribution(
        targetAmountGel: BigDecimal,
        currentSavedGel: BigDecimal,
        deadlineEpochMillis: Long,
        currentCalendar: Calendar = Calendar.getInstance()
    ): BigDecimal {
        if (currentSavedGel >= targetAmountGel || targetAmountGel <= BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }

        val deadlineCal = Calendar.getInstance().apply { timeInMillis = deadlineEpochMillis }
        val diffYears = deadlineCal.get(Calendar.YEAR) - currentCalendar.get(Calendar.YEAR)
        val diffMonths = diffYears * 12 + (deadlineCal.get(Calendar.MONTH) - currentCalendar.get(Calendar.MONTH))

        val months = if (diffMonths <= 0) 1 else diffMonths
        val remaining = targetAmountGel.subtract(currentSavedGel)

        return remaining.divide(BigDecimal(months), 2, RoundingMode.CEILING)
    }
}

