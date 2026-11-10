package com.fundory.app

import com.fundory.app.domain.model.Transaction
import com.fundory.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransactionBalanceCalculationTest {

    @Test
    fun testSignedAmountForTransactionTypes() {
        val deposit = Transaction(
            id = "tx1",
            goalId = "g1",
            type = TransactionType.DEPOSIT,
            amountGel = BigDecimal("100.00")
        )
        assertEquals(BigDecimal("100.00"), deposit.signedAmountGel)
        assertTrue(deposit.isPositive)

        val withdrawal = Transaction(
            id = "tx2",
            goalId = "g1",
            type = TransactionType.WITHDRAWAL,
            amountGel = BigDecimal("45.50")
        )
        assertEquals(BigDecimal("-45.50"), withdrawal.signedAmountGel)
        assertFalse(withdrawal.isPositive)

        val posAdjustment = Transaction(
            id = "tx3",
            goalId = "g1",
            type = TransactionType.ADJUSTMENT,
            amountGel = BigDecimal("10.00")
        )
        assertEquals(BigDecimal("10.00"), posAdjustment.signedAmountGel)
        assertTrue(posAdjustment.isPositive)

        val negAdjustment = Transaction(
            id = "tx4",
            goalId = "g1",
            type = TransactionType.ADJUSTMENT,
            amountGel = BigDecimal("-15.00")
        )
        assertEquals(BigDecimal("-15.00"), negAdjustment.signedAmountGel)
        assertFalse(negAdjustment.isPositive)

        val transfer = Transaction(
            id = "tx5",
            goalId = "g1",
            targetGoalId = "g2",
            type = TransactionType.TRANSFER,
            amountGel = BigDecimal("50.00")
        )
        assertEquals(BigDecimal("-50.00"), transfer.signedAmountGel)
        assertFalse(transfer.isPositive)
    }

    @Test
    fun testSequentialTransactionBalanceComputation() {
        val transactions = listOf(
            Transaction("1", "g1", TransactionType.DEPOSIT, BigDecimal("100.00")),
            Transaction("2", "g1", TransactionType.DEPOSIT, BigDecimal("40.00")),
            Transaction("3", "g1", TransactionType.WITHDRAWAL, BigDecimal("20.00")),
            Transaction("4", "g1", TransactionType.ADJUSTMENT, BigDecimal("15.50")),
            Transaction("5", "g1", TransactionType.ADJUSTMENT, BigDecimal("-5.50"))
        )

        var balance = BigDecimal.ZERO
        for (tx in transactions) {
            balance = balance.add(tx.signedAmountGel)
        }

        // 100 + 40 - 20 + 15.50 - 5.50 = 130.00
        assertEquals(BigDecimal("130.00"), balance)
    }

    @Test
    fun testMultiGoalTransferConservationOfFunds() {
        val goal1Tx = mutableListOf(
            Transaction("g1_1", "goal_pc", TransactionType.DEPOSIT, BigDecimal("400.00")),
            Transaction("g1_2", "goal_pc", TransactionType.WITHDRAWAL, BigDecimal("50.00"))
        )
        val goal2Tx = mutableListOf(
            Transaction("g2_1", "goal_cam", TransactionType.DEPOSIT, BigDecimal("200.00"))
        )

        val balanceGoal1Before = goal1Tx.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        val balanceGoal2Before = goal2Tx.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        val totalCombinedBefore = balanceGoal1Before.add(balanceGoal2Before)

        assertEquals(BigDecimal("350.00"), balanceGoal1Before)
        assertEquals(BigDecimal("200.00"), balanceGoal2Before)
        assertEquals(BigDecimal("550.00"), totalCombinedBefore)

        // Perform transfer of 100 GEL from goal_pc to goal_cam
        val transferAmount = BigDecimal("100.00")
        goal1Tx.add(
            Transaction("tr_out", "goal_pc", TransactionType.TRANSFER, transferAmount, targetGoalId = "goal_cam")
        )
        goal2Tx.add(
            Transaction("tr_in", "goal_cam", TransactionType.DEPOSIT, transferAmount, note = "Transfer from goal_pc")
        )

        val balanceGoal1After = goal1Tx.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        val balanceGoal2After = goal2Tx.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        val totalCombinedAfter = balanceGoal1After.add(balanceGoal2After)

        assertEquals(BigDecimal("250.00"), balanceGoal1After)
        assertEquals(BigDecimal("300.00"), balanceGoal2After)
        assertEquals(BigDecimal("550.00"), totalCombinedAfter)
    }

    @Test
    fun testOverdraftPreventionLogic() {
        val currentSaved = BigDecimal("150.00")
        val withdrawalAttempt = BigDecimal("200.00")

        val canWithdraw = currentSaved >= withdrawalAttempt
        assertFalse(canWithdraw)

        val validWithdrawal = BigDecimal("120.00")
        val canWithdrawValid = currentSaved >= validWithdrawal
        assertTrue(canWithdrawValid)

        val remainingBalance = currentSaved.subtract(validWithdrawal)
        assertEquals(BigDecimal("30.00"), remainingBalance)
        assertTrue(remainingBalance >= BigDecimal.ZERO)
    }

    @Test
    fun testHistoricalTransactionEditRecalculation() {
        val initialTransactions = mutableListOf(
            Transaction("1", "g1", TransactionType.DEPOSIT, BigDecimal("100.00")),
            Transaction("2", "g1", TransactionType.DEPOSIT, BigDecimal("50.00")),
            Transaction("3", "g1", TransactionType.WITHDRAWAL, BigDecimal("30.00"))
        )

        var balance = initialTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        assertEquals(BigDecimal("120.00"), balance)

        // User edits transaction 2 from 50.00 to 80.00
        val updatedTransactions = initialTransactions.map { tx ->
            if (tx.id == "2") tx.copy(amountGel = BigDecimal("80.00")) else tx
        }

        balance = updatedTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        assertEquals(BigDecimal("150.00"), balance)

        // User deletes transaction 3 (withdrawal of 30.00)
        val afterDeletion = updatedTransactions.filter { it.id != "3" }
        balance = afterDeletion.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        assertEquals(BigDecimal("180.00"), balance)
    }

    @Test
    fun testMonthlyGroupingAndNetSavings() {
        val cal = Calendar.getInstance(Locale.US)
        cal.set(2026, Calendar.NOVEMBER, 5)
        val nov5 = cal.timeInMillis
        cal.set(2026, Calendar.NOVEMBER, 20)
        val nov20 = cal.timeInMillis
        cal.set(2026, Calendar.DECEMBER, 1)
        val dec1 = cal.timeInMillis

        val txList = listOf(
            Transaction("1", "g1", TransactionType.DEPOSIT, BigDecimal("100.00"), timestampEpochMillis = nov5),
            Transaction("2", "g1", TransactionType.WITHDRAWAL, BigDecimal("20.00"), timestampEpochMillis = nov20),
            Transaction("3", "g1", TransactionType.DEPOSIT, BigDecimal("200.00"), timestampEpochMillis = dec1)
        )

        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val grouped = txList.groupBy { monthFormat.format(it.timestampEpochMillis) }

        val novTx = grouped["2026-11"] ?: emptyList()
        val novNet = novTx.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        assertEquals(BigDecimal("80.00"), novNet)

        val decTx = grouped["2026-12"] ?: emptyList()
        val decNet = decTx.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        assertEquals(BigDecimal("200.00"), decNet)
    }

    @Test
    fun testGeneralSavingsAndTotalSavingsInvariant() {
        // User deposits 40 GEL without a goal (General Savings)
        val generalDeposit = Transaction(
            id = "gen_1",
            goalId = null,
            type = TransactionType.DEPOSIT,
            amountGel = BigDecimal("40.00")
        )
        assertTrue(generalDeposit.isGeneralSavings)

        // User creates a goal and deposits 100 GEL into it
        val goalDeposit = Transaction(
            id = "goal_1",
            goalId = "pc_goal",
            type = TransactionType.DEPOSIT,
            amountGel = BigDecimal("100.00")
        )
        assertFalse(goalDeposit.isGeneralSavings)

        // User withdraws 10 GEL from General Savings
        val generalWithdrawal = Transaction(
            id = "gen_2",
            goalId = null,
            type = TransactionType.WITHDRAWAL,
            amountGel = BigDecimal("10.00")
        )

        val allTransactions = listOf(generalDeposit, goalDeposit, generalWithdrawal)

        val generalSavingsBalance = allTransactions
            .filter { it.isGeneralSavings }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        assertEquals(BigDecimal("30.00"), generalSavingsBalance)

        val goalSavingsBalance = allTransactions
            .filter { it.goalId == "pc_goal" }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        assertEquals(BigDecimal("100.00"), goalSavingsBalance)

        val totalSavingsAcrossAll = allTransactions
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.signedAmountGel) }
        assertEquals(BigDecimal("130.00"), totalSavingsAcrossAll)

        // Invariant check: Total = General + Sum(Goals)
        assertEquals(totalSavingsAcrossAll, generalSavingsBalance.add(goalSavingsBalance))
    }
}

