package com.fundory.app

import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class GoalTotalCalculationTest {

    private val ratesToGel = mapOf(
        "GEL" to BigDecimal("1.00"),
        "USD" to BigDecimal("2.594"),
        "EUR" to BigDecimal("2.90"),
        "CNY" to BigDecimal("0.388")
    )

    @Test
    fun testGoalCalculatedTargetFromProducts() {
        val products = listOf(
            Product(
                id = "p1",
                goalId = "g1",
                name = "CPU + Motherboard",
                originalPrice = BigDecimal("1100.00"),
                currency = "CNY",
                quantity = 1
            ), // 1100 * 0.388 = 426.80 GEL
            Product(
                id = "p2",
                goalId = "g1",
                name = "Cooler",
                originalPrice = BigDecimal("66.90"),
                currency = "CNY",
                quantity = 1
            ), // 66.9 * 0.388 = 25.96 GEL
            Product(
                id = "p3",
                goalId = "g1",
                name = "RAM",
                originalPrice = BigDecimal("543.00"),
                currency = "CNY",
                quantity = 1
            ), // 543 * 0.388 = 210.68 GEL
            Product(
                id = "p4",
                goalId = "g1",
                name = "Shipping",
                originalPrice = BigDecimal("15.00"),
                currency = "USD",
                quantity = 1
            ) // 15 * 2.594 = 38.91 GEL
        )

        var totalTargetGel = BigDecimal.ZERO
        for (p in products) {
            totalTargetGel = totalTargetGel.add(p.calculateTotalGel(ratesToGel))
        }

        // 426.80 + 25.96 + 210.68 + 38.91 = 702.35 GEL
        assertEquals(BigDecimal("702.35"), totalTargetGel)

        val goal = Goal(
            id = "g1",
            name = "PC Upgrade",
            targetAmountGel = totalTargetGel,
            savedAmountGel = BigDecimal("40.00"),
            products = products
        )

        // Remaining: 702.35 - 40.00 = 662.35 GEL
        assertEquals(BigDecimal("662.35"), goal.remainingAmountGel)

        // Progress percentage: 40.00 / 702.35 ≈ 5.7%
        assertEquals(5.69f, goal.progressPercentage, 0.05f)
        assertFalse(goal.isCompleted)
    }

    @Test
    fun testCompletedGoalCalculations() {
        val goal = Goal(
            id = "g2",
            name = "Weekend Trip",
            targetAmountGel = BigDecimal("500.00"),
            savedAmountGel = BigDecimal("500.00"),
            status = GoalStatus.COMPLETED
        )

        assertEquals(BigDecimal.ZERO, goal.remainingAmountGel)
        assertEquals(100f, goal.progressPercentage, 0.01f)
        assertTrue(goal.isCompleted)
    }

    @Test
    fun testZeroTargetSafety() {
        val goal = Goal(
            id = "g3",
            name = "Empty Goal",
            targetAmountGel = BigDecimal.ZERO,
            savedAmountGel = BigDecimal.ZERO
        )

        assertEquals(BigDecimal.ZERO, goal.remainingAmountGel)
        assertEquals(0f, goal.progressPercentage, 0.01f)
        assertFalse(goal.isCompleted)
    }
}
