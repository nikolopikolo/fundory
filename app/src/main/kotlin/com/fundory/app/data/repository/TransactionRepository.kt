package com.fundory.app.data.repository

import androidx.room.withTransaction
import com.fundory.app.data.local.FundoryDatabase
import com.fundory.app.data.local.dao.GoalDao
import com.fundory.app.data.local.dao.GoalSnapshotDao
import com.fundory.app.data.local.dao.TransactionDao
import com.fundory.app.data.local.entity.GoalSnapshotEntity
import com.fundory.app.data.local.entity.TransactionEntity
import com.fundory.app.domain.model.Transaction
import com.fundory.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class TransactionResult {
    data object Success : TransactionResult()
    data class Failure(val reason: String) : TransactionResult()
}

@Singleton
class TransactionRepository @Inject constructor(
    private val database: FundoryDatabase,
    private val transactionDao: TransactionDao,
    private val goalDao: GoalDao,
    private val goalSnapshotDao: GoalSnapshotDao
) {

    val allTransactionsWithGoalsFlow: Flow<List<Transaction>> = combine(
        transactionDao.getAllTransactionsFlow(),
        goalDao.getAllGoalsFlow()
    ) { transactions, goals ->
        val goalMap = goals.associate { it.id to it.name }
        transactions.map { entity ->
            entity.toDomain(
                goalName = goalMap[entity.goalId] ?: "Unknown Goal",
                targetGoalName = entity.targetGoalId?.let { goalMap[it] }
            )
        }
    }

    fun getTransactionsByGoalFlow(goalId: String): Flow<List<Transaction>> = combine(
        transactionDao.getTransactionsByGoalFlow(goalId),
        goalDao.getAllGoalsFlow()
    ) { transactions, goals ->
        val goalMap = goals.associate { it.id to it.name }
        transactions.map { entity ->
            entity.toDomain(
                goalName = goalMap[entity.goalId] ?: "Unknown Goal",
                targetGoalName = entity.targetGoalId?.let { goalMap[it] }
            )
        }
    }

    suspend fun recordDeposit(
        goalId: String,
        amountGel: BigDecimal,
        note: String? = null,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ): TransactionResult {
        if (amountGel <= BigDecimal.ZERO) {
            return TransactionResult.Failure("Deposit amount must be greater than zero.")
        }

        val tx = Transaction(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            type = TransactionType.DEPOSIT,
            amountGel = amountGel,
            timestampEpochMillis = timestampEpochMillis,
            note = note
        )

        return executeAtomicTransaction(tx)
    }

    suspend fun recordWithdrawal(
        goalId: String,
        amountGel: BigDecimal,
        note: String? = null,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ): TransactionResult {
        if (amountGel <= BigDecimal.ZERO) {
            return TransactionResult.Failure("Withdrawal amount must be greater than zero.")
        }

        val currentGoal = goalDao.getGoalById(goalId)
            ?: return TransactionResult.Failure("Selected goal not found.")

        if (currentGoal.savedAmountGel < amountGel) {
            return TransactionResult.Failure(
                "Insufficient saved balance: goal has ${currentGoal.savedAmountGel} GEL saved, cannot withdraw ${amountGel} GEL."
            )
        }

        val tx = Transaction(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            type = TransactionType.WITHDRAWAL,
            amountGel = amountGel,
            timestampEpochMillis = timestampEpochMillis,
            note = note
        )

        return executeAtomicTransaction(tx)
    }

    suspend fun recordAdjustment(
        goalId: String,
        adjustedAmountGel: BigDecimal, // can be positive or negative
        note: String? = null,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ): TransactionResult {
        if (adjustedAmountGel == BigDecimal.ZERO) {
            return TransactionResult.Failure("Adjustment amount cannot be zero.")
        }

        val currentGoal = goalDao.getGoalById(goalId)
            ?: return TransactionResult.Failure("Selected goal not found.")

        val newBalance = currentGoal.savedAmountGel.add(adjustedAmountGel)
        if (newBalance < BigDecimal.ZERO) {
            return TransactionResult.Failure("Adjustment would result in negative balance (${newBalance} GEL).")
        }

        val tx = Transaction(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            type = TransactionType.ADJUSTMENT,
            amountGel = adjustedAmountGel,
            timestampEpochMillis = timestampEpochMillis,
            note = note
        )

        return executeAtomicTransaction(tx)
    }

    suspend fun recordTransfer(
        sourceGoalId: String,
        destinationGoalId: String,
        amountGel: BigDecimal,
        note: String? = null,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ): TransactionResult {
        if (amountGel <= BigDecimal.ZERO) {
            return TransactionResult.Failure("Transfer amount must be greater than zero.")
        }
        if (sourceGoalId == destinationGoalId) {
            return TransactionResult.Failure("Cannot transfer to the same goal.")
        }

        return database.withTransaction {
            val sourceGoal = goalDao.getGoalById(sourceGoalId)
                ?: return@withTransaction TransactionResult.Failure("Source goal not found.")
            val destGoal = goalDao.getGoalById(destinationGoalId)
                ?: return@withTransaction TransactionResult.Failure("Destination goal not found.")

            if (sourceGoal.savedAmountGel < amountGel) {
                return@withTransaction TransactionResult.Failure(
                    "Insufficient balance in source goal (${sourceGoal.savedAmountGel} GEL)."
                )
            }

            // 1. Outgoing transfer transaction
            val outgoingTx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                goalId = sourceGoalId,
                type = TransactionType.TRANSFER,
                amountGel = amountGel,
                targetGoalId = destinationGoalId,
                timestampEpochMillis = timestampEpochMillis,
                note = note ?: "Transfer to ${destGoal.name}"
            )
            transactionDao.insertTransaction(outgoingTx)

            // 2. Incoming deposit on destination goal
            val incomingTx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                goalId = destinationGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = amountGel,
                targetGoalId = sourceGoalId,
                timestampEpochMillis = timestampEpochMillis,
                note = note ?: "Transfer from ${sourceGoal.name}"
            )
            transactionDao.insertTransaction(incomingTx)

            // 3. Recalculate both goal balances
            recalculateGoalBalanceInternal(sourceGoalId)
            recalculateGoalBalanceInternal(destinationGoalId)

            TransactionResult.Success
        }
    }

    suspend fun deleteTransaction(transactionId: String): TransactionResult {
        return database.withTransaction {
            val tx = transactionDao.getTransactionById(transactionId)
                ?: return@withTransaction TransactionResult.Failure("Transaction not found.")

            transactionDao.deleteTransactionById(transactionId)
            recalculateGoalBalanceInternal(tx.goalId)

            // If it was a transfer, also check target
            tx.targetGoalId?.let { recalculateGoalBalanceInternal(it) }

            TransactionResult.Success
        }
    }

    suspend fun updateTransaction(transaction: Transaction): TransactionResult {
        return database.withTransaction {
            transactionDao.updateTransaction(TransactionEntity.fromDomain(transaction))
            recalculateGoalBalanceInternal(transaction.goalId)
            transaction.targetGoalId?.let { recalculateGoalBalanceInternal(it) }
            TransactionResult.Success
        }
    }

    private suspend fun executeAtomicTransaction(transaction: Transaction): TransactionResult {
        return database.withTransaction {
            transactionDao.insertTransaction(TransactionEntity.fromDomain(transaction))
            val recalculated = recalculateGoalBalanceInternal(transaction.goalId)
            if (recalculated < BigDecimal.ZERO) {
                throw IllegalStateException("Transaction caused negative balance: $recalculated")
            }
            TransactionResult.Success
        }
    }

    /**
     * Atomically sums all historical transactions for a goal to ensure 100% data integrity.
     */
    private suspend fun recalculateGoalBalanceInternal(goalId: String): BigDecimal {
        val allTx = transactionDao.getTransactionsByGoalAscending(goalId)
        var sum = BigDecimal.ZERO

        for (tx in allTx) {
            sum = when (tx.type) {
                TransactionType.DEPOSIT -> sum.add(tx.amountGel)
                TransactionType.WITHDRAWAL -> sum.subtract(tx.amountGel)
                TransactionType.ADJUSTMENT -> sum.add(tx.amountGel)
                TransactionType.TRANSFER -> sum.subtract(tx.amountGel)
            }
        }

        if (sum < BigDecimal.ZERO) {
            sum = BigDecimal.ZERO
        }

        val goal = goalDao.getGoalById(goalId)
        if (goal != null) {
            val updatedGoal = goal.copy(
                savedAmountGel = sum,
                lastUpdatedEpochMillis = System.currentTimeMillis()
            )
            goalDao.updateGoal(updatedGoal)

            // Record snapshot for historical graphs
            goalSnapshotDao.insertSnapshot(
                GoalSnapshotEntity(
                    goalId = goalId,
                    timestampEpochMillis = System.currentTimeMillis(),
                    savedAmountGel = sum,
                    targetAmountGel = goal.targetAmountGel,
                    completionPercentage = updatedGoal.toDomain().progressPercentage
                )
            )
        }

        return sum
    }
}
