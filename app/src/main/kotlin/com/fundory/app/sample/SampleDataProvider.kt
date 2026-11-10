package com.fundory.app.sample

import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.domain.model.Product
import com.fundory.app.domain.model.Transaction
import com.fundory.app.domain.model.TransactionType
import java.math.BigDecimal
import java.util.Calendar
import java.util.UUID

object SampleDataProvider {

    fun generateSampleGoals(ratesToGel: Map<String, BigDecimal>): Pair<List<Goal>, List<Transaction>> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // 1. Goal: PC Upgrade (Direct example from project prompt)
        val pcGoalId = "sample_pc_upgrade"
        val pcProducts = listOf(
            Product(
                id = UUID.randomUUID().toString(),
                goalId = pcGoalId,
                name = "CPU + Motherboard Bundle",
                description = "AMD Ryzen 5 7600 + B650M AM5 board",
                originalPrice = BigDecimal("1100.00"),
                currency = "CNY",
                quantity = 1,
                productUrl = "https://item.taobao.com",
                shippingCost = BigDecimal.ZERO,
                shippingCurrency = "CNY",
                taxesAndFees = BigDecimal.ZERO,
                sortOrder = 0,
                dateAddedEpochMillis = now - 30 * 86400000L,
                lastEditedEpochMillis = now - 30 * 86400000L
            ),
            Product(
                id = UUID.randomUUID().toString(),
                goalId = pcGoalId,
                name = "CPU Tower Cooler",
                description = "Thermalright Peerless Assassin 120 SE",
                originalPrice = BigDecimal("66.90"),
                currency = "CNY",
                quantity = 1,
                productUrl = "https://item.taobao.com",
                shippingCost = BigDecimal.ZERO,
                shippingCurrency = "CNY",
                taxesAndFees = BigDecimal.ZERO,
                sortOrder = 1,
                dateAddedEpochMillis = now - 28 * 86400000L,
                lastEditedEpochMillis = now - 28 * 86400000L
            ),
            Product(
                id = UUID.randomUUID().toString(),
                goalId = pcGoalId,
                name = "DDR5 32GB RAM Kit",
                description = "KingBank DDR5 6000MHz CL30 2x16GB",
                originalPrice = BigDecimal("543.00"),
                currency = "CNY",
                quantity = 1,
                productUrl = "https://item.jd.com",
                shippingCost = BigDecimal.ZERO,
                shippingCurrency = "CNY",
                taxesAndFees = BigDecimal.ZERO,
                sortOrder = 2,
                dateAddedEpochMillis = now - 25 * 86400000L,
                lastEditedEpochMillis = now - 25 * 86400000L
            ),
            Product(
                id = UUID.randomUUID().toString(),
                goalId = pcGoalId,
                name = "International Combined Shipping",
                description = "Air cargo freight via Georgian freight forwarder",
                originalPrice = BigDecimal("15.00"),
                currency = "USD",
                quantity = 1,
                shippingCost = BigDecimal.ZERO,
                shippingCurrency = "USD",
                taxesAndFees = BigDecimal.ZERO,
                sortOrder = 3,
                dateAddedEpochMillis = now - 20 * 86400000L,
                lastEditedEpochMillis = now - 20 * 86400000L
            )
        )

        // Calculate target from products
        var pcTargetGel = BigDecimal.ZERO
        for (p in pcProducts) {
            pcTargetGel = pcTargetGel.add(p.calculateTotalGel(ratesToGel))
        }
        if (pcTargetGel <= BigDecimal.ZERO) {
            pcTargetGel = BigDecimal("702.00")
        }

        val pcGoal = Goal(
            id = pcGoalId,
            name = "PC Upgrade",
            description = "High efficiency AM5 gaming & workstation components imported from Taobao & USA",
            iconName = "computer",
            colorHex = "#38B273", // Fundory Green
            targetAmountGel = pcTargetGel,
            isTargetCalculatedFromProducts = true,
            savedAmountGel = BigDecimal("40.00"),
            monthlyPlanGel = BigDecimal("100.00"),
            targetDateEpochMillis = now + 200 * 86400000L,
            status = GoalStatus.ACTIVE,
            createdDateEpochMillis = now - 45 * 86400000L,
            lastUpdatedEpochMillis = now,
            products = pcProducts
        )

        // 2. Goal: Sony Mirrorless Camera
        val cameraGoalId = "sample_camera"
        val cameraProducts = listOf(
            Product(
                id = UUID.randomUUID().toString(),
                goalId = cameraGoalId,
                name = "Sony A6700 Body",
                description = "Compact APS-C 4K60p video & photo camera",
                originalPrice = BigDecimal("1199.00"),
                currency = "USD",
                quantity = 1,
                productUrl = "https://bhphotovideo.com",
                shippingCost = BigDecimal("25.00"),
                shippingCurrency = "USD",
                sortOrder = 0,
                dateAddedEpochMillis = now - 60 * 86400000L
            ),
            Product(
                id = UUID.randomUUID().toString(),
                goalId = cameraGoalId,
                name = "Sigma 18-50mm f/2.8 Lens",
                description = "Standard fast zoom lens for Sony E-mount",
                originalPrice = BigDecimal("499.00"),
                currency = "USD",
                quantity = 1,
                productUrl = "https://amazon.com",
                shippingCost = BigDecimal("10.00"),
                shippingCurrency = "USD",
                sortOrder = 1,
                dateAddedEpochMillis = now - 50 * 86400000L
            )
        )

        var cameraTargetGel = BigDecimal.ZERO
        for (p in cameraProducts) {
            cameraTargetGel = cameraTargetGel.add(p.calculateTotalGel(ratesToGel))
        }

        val cameraGoal = Goal(
            id = cameraGoalId,
            name = "Sony A6700 Camera Kit",
            description = "Travel photography and video gear setup",
            iconName = "photo_camera",
            colorHex = "#3B82F6", // Blue
            targetAmountGel = cameraTargetGel,
            isTargetCalculatedFromProducts = true,
            savedAmountGel = BigDecimal("1650.00"),
            monthlyPlanGel = BigDecimal("350.00"),
            targetDateEpochMillis = now + 150 * 86400000L,
            status = GoalStatus.ACTIVE,
            createdDateEpochMillis = now - 90 * 86400000L,
            lastUpdatedEpochMillis = now,
            products = cameraProducts
        )

        // 3. Goal: Fender Acoustic Guitar
        val guitarGoalId = "sample_guitar"
        val guitarGoal = Goal(
            id = guitarGoalId,
            name = "Fender Paramount Guitar",
            description = "All-mahogany acoustic-electric dreadnought guitar",
            iconName = "music_note",
            colorHex = "#F59E0B", // Amber
            targetAmountGel = BigDecimal("1250.00"),
            isTargetCalculatedFromProducts = false,
            savedAmountGel = BigDecimal("520.00"),
            monthlyPlanGel = BigDecimal("150.00"),
            targetDateEpochMillis = now + 120 * 86400000L,
            status = GoalStatus.ACTIVE,
            createdDateEpochMillis = now - 60 * 86400000L,
            lastUpdatedEpochMillis = now
        )

        // 4. Completed Goal: Weekend Mountain Trip
        val tripGoalId = "sample_kazbegi_trip"
        val tripGoal = Goal(
            id = tripGoalId,
            name = "Kazbegi Weekend Getaway",
            description = "Cabin rental, dinner, and fuel for mountain trip",
            iconName = "terrain",
            colorHex = "#10B981",
            targetAmountGel = BigDecimal("500.00"),
            isTargetCalculatedFromProducts = false,
            savedAmountGel = BigDecimal("500.00"),
            monthlyPlanGel = BigDecimal("250.00"),
            targetDateEpochMillis = now - 5 * 86400000L,
            status = GoalStatus.COMPLETED,
            createdDateEpochMillis = now - 65 * 86400000L,
            lastUpdatedEpochMillis = now - 5 * 86400000L
        )

        val goals = listOf(pcGoal, cameraGoal, guitarGoal, tripGoal)

        // Historical sample transactions covering multiple months to populate Graphs A, B, and C
        val transactions = mutableListOf<Transaction>()

        // Months past
        val m3 = now - 90 * 86400000L
        val m2 = now - 60 * 86400000L
        val m1 = now - 30 * 86400000L
        val current = now - 2 * 86400000L

        // PC Upgrade transactions (Initial 40 GEL saved)
        transactions.add(
            Transaction(
                id = UUID.randomUUID().toString(),
                goalId = pcGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = BigDecimal("40.00"),
                timestampEpochMillis = now - 15 * 86400000L,
                note = "Initial savings deposit"
            )
        )

        // Camera transactions
        transactions.add(
            Transaction(
                id = UUID.randomUUID().toString(),
                goalId = cameraGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = BigDecimal("500.00"),
                timestampEpochMillis = m3,
                note = "Quarterly bonus allocation"
            )
        )
        transactions.add(
            Transaction(
                id = UUID.randomUUID().toString(),
                goalId = cameraGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = BigDecimal("400.00"),
                timestampEpochMillis = m2,
                note = "Monthly savings"
            )
        )
        transactions.add(
            Transaction(
                id = UUID.randomUUID().toString(),
                goalId = cameraGoalId,
                type = TransactionType.WITHDRAWAL,
                amountGel = BigDecimal("50.00"),
                timestampEpochMillis = m2 + 5 * 86400000L,
                note = "Lens cap & protective filter pre-order"
            )
        )
        transactions.add(
            Transaction(
                id = UUID.randomUUID().toString(),
                goalId = cameraGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = BigDecimal("400.00"),
                timestampEpochMillis = m1,
                note = "Monthly savings"
            )
        )
        transactions.add(
            Transaction(
                id = UUID.randomUUID().toString(),
                goalId = cameraGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = BigDecimal("400.00"),
                timestampEpochMillis = current,
                note = "November savings"
            )
        )

        // Guitar transactions
        transactions.add(
            Transaction(
                id = UUID.randomUUID().toString(),
                goalId = guitarGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = BigDecimal("200.00"),
                timestampEpochMillis = m2,
                note = "Starting fund"
            )
        )
        transactions.add(
            Transaction(
                id = UUID.randomUUID().toString(),
                goalId = guitarGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = BigDecimal("200.00"),
                timestampEpochMillis = m1,
                note = "Monthly savings"
            )
        )
        transactions.add(
            Transaction(
                id = UUID.randomUUID().toString(),
                goalId = guitarGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = BigDecimal("120.00"),
                timestampEpochMillis = current,
                note = "Gig earnings"
            )
        )

        // Trip transactions (reached 500 GEL)
        transactions.add(
            Transaction(
                id = UUID.randomUUID().toString(),
                goalId = tripGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = BigDecimal("250.00"),
                timestampEpochMillis = m2,
                note = "Trip deposit 1"
            )
        )
        transactions.add(
            Transaction(
                id = UUID.randomUUID().toString(),
                goalId = tripGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = BigDecimal("250.00"),
                timestampEpochMillis = m1,
                note = "Trip deposit 2 (Goal Completed!)"
            )
        )

        return Pair(goals, transactions)
    }
}

