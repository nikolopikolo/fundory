package com.fundory.app

import com.fundory.app.domain.engine.SavingsForecastEngine
import com.fundory.app.domain.model.FutureBonus
import com.fundory.app.domain.model.ProjectionScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale

class SavingsForecastEngineTest {

    private lateinit var engine: SavingsForecastEngine

    @Before
    fun setUp() {
        engine = SavingsForecastEngine()
    }

    @Test
    fun testForecastWhenTargetAlreadyReached() {
        val startCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.NOVEMBER, 1, 12, 0, 0)
        }

        val result = engine.calculateProjection(
            goalId = "g1",
            targetAmountGel = BigDecimal("500.00"),
            currentSavedGel = BigDecimal("500.00"),
            monthlyPlanGel = BigDecimal("100.00"),
            scenario = ProjectionScenario.EXPECTED,
            startCalendar = startCal
        )

        assertEquals(0, result.remainingMonths)
        assertEquals(BigDecimal.ZERO, result.remainingAmountGel)
        assertEquals("Target Reached!", result.completionDateFormatted)
        assertEquals(1, result.timeline.size)
        assertTrue(result.timeline.first().isTargetReached)
    }

    @Test
    fun testForecastWithZeroMonthlyPlanAndNoBonuses() {
        val startCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.NOVEMBER, 1, 12, 0, 0)
        }

        val result = engine.calculateProjection(
            goalId = "g1",
            targetAmountGel = BigDecimal("1000.00"),
            currentSavedGel = BigDecimal("200.00"),
            monthlyPlanGel = BigDecimal.ZERO,
            scenario = ProjectionScenario.EXPECTED,
            startCalendar = startCal
        )

        assertEquals(-1, result.remainingMonths)
        assertEquals(BigDecimal("800.00"), result.remainingAmountGel)
        assertEquals("Not enough information to estimate", result.completionDateFormatted)
    }

    @Test
    fun testDeterministicMonthlyProjectionExpected() {
        val startCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.JANUARY, 1, 12, 0, 0)
        }

        // Target: 700 GEL, current: 100 GEL -> remaining 600 GEL
        // Monthly: 100 GEL -> 6 months to reach
        val result = engine.calculateProjection(
            goalId = "g_pc",
            targetAmountGel = BigDecimal("700.00"),
            currentSavedGel = BigDecimal("100.00"),
            monthlyPlanGel = BigDecimal("100.00"),
            scenario = ProjectionScenario.EXPECTED,
            startCalendar = startCal
        )

        assertEquals(6, result.remainingMonths)
        assertEquals(BigDecimal("600.00"), result.remainingAmountGel)
        assertEquals(BigDecimal("100.00"), result.monthlyContributionGel)
        assertNotNull(result.estimatedCompletionDateEpochMillis)

        // Starting month + 6 projected months = 7 points
        assertEquals(7, result.timeline.size)
        assertEquals(BigDecimal("100.00"), result.timeline[0].balanceGel) // Jan
        assertEquals(BigDecimal("200.00"), result.timeline[1].balanceGel) // Feb
        assertEquals(BigDecimal("300.00"), result.timeline[2].balanceGel) // Mar
        assertEquals(BigDecimal("400.00"), result.timeline[3].balanceGel) // Apr
        assertEquals(BigDecimal("500.00"), result.timeline[4].balanceGel) // May
        assertEquals(BigDecimal("600.00"), result.timeline[5].balanceGel) // Jun
        assertEquals(BigDecimal("700.00"), result.timeline[6].balanceGel) // Jul (Reached)

        assertTrue(result.timeline[6].isTargetReached)
        assertEquals("Jul 2026", result.completionDateFormatted)
    }

    @Test
    fun testConservativeAndOptimisticScenarios() {
        val startCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.JANUARY, 1, 12, 0, 0)
        }

        val target = BigDecimal("1000.00")
        val current = BigDecimal("0.00")
        val plannedMonthly = BigDecimal("100.00")

        // Conservative multiplier is 0.6 -> 60.00 GEL/mo -> ceil(1000 / 60) = 17 months
        val conservativeResult = engine.calculateProjection(
            goalId = "g1",
            targetAmountGel = target,
            currentSavedGel = current,
            monthlyPlanGel = plannedMonthly,
            scenario = ProjectionScenario.CONSERVATIVE,
            startCalendar = startCal.clone() as Calendar
        )
        assertEquals(BigDecimal("60.00"), conservativeResult.monthlyContributionGel)
        assertEquals(17, conservativeResult.remainingMonths)

        // Optimistic multiplier is 1.4 -> 140.00 GEL/mo -> ceil(1000 / 140) = 8 months
        val optimisticResult = engine.calculateProjection(
            goalId = "g1",
            targetAmountGel = target,
            currentSavedGel = current,
            monthlyPlanGel = plannedMonthly,
            scenario = ProjectionScenario.OPTIMISTIC,
            startCalendar = startCal.clone() as Calendar
        )
        assertEquals(BigDecimal("140.00"), optimisticResult.monthlyContributionGel)
        assertEquals(8, optimisticResult.remainingMonths)
    }

    @Test
    fun testFutureBonusInclusionSpeedsUpGoal() {
        val startCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.JANUARY, 1, 12, 0, 0)
        }

        // Bonus of 200 GEL scheduled in March 2026 (Month 2 from Jan)
        val bonusCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.MARCH, 15, 12, 0, 0)
        }
        val bonus = FutureBonus(
            id = "bonus_1",
            name = "Quarterly Work Bonus",
            amountGel = BigDecimal("200.00"),
            scheduledDateEpochMillis = bonusCal.timeInMillis
        )

        // Target: 500 GEL, Current: 100 GEL, Monthly: 50 GEL
        // Without bonus: remaining 400 GEL / 50 = 8 months
        // With 200 GEL bonus in Month 2:
        // Jan: 100
        // Feb (+50): 150
        // Mar (+50 + 200): 400
        // Apr (+50): 450
        // May (+50): 500 (Reached in month 4!)
        val result = engine.calculateProjection(
            goalId = "g1",
            targetAmountGel = BigDecimal("500.00"),
            currentSavedGel = BigDecimal("100.00"),
            monthlyPlanGel = BigDecimal("50.00"),
            scenario = ProjectionScenario.EXPECTED,
            futureBonuses = listOf(bonus),
            startCalendar = startCal
        )

        assertEquals(4, result.remainingMonths)
        assertEquals("May 2026", result.completionDateFormatted)
        assertEquals(BigDecimal("400.00"), result.timeline[2].balanceGel) // March has the 200 GEL bonus
    }

    @Test
    fun testCalculateRequiredMonthlyContribution() {
        val currentCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.JANUARY, 1, 12, 0, 0)
        }
        val deadlineCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.JULY, 1, 12, 0, 0) // 6 months difference
        }

        val target = BigDecimal("1000.00")
        val current = BigDecimal("400.00") // Remaining = 600.00 GEL

        val requiredMonthly = engine.calculateRequiredMonthlyContribution(
            targetAmountGel = target,
            currentSavedGel = current,
            deadlineEpochMillis = deadlineCal.timeInMillis,
            currentCalendar = currentCal
        )

        // 600.00 GEL / 6 months = 100.00 GEL/month
        assertEquals(BigDecimal("100.00"), requiredMonthly)
    }
}

