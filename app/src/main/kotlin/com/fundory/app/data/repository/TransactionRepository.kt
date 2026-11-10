package com.fundory.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.fundory.app.data.local.FundoryDatabase
import com.fundory.app.data.local.dao.GoalDao
import com.fundory.app.data.local.dao.GoalSnapshotDao
import com.fundory.app.data.local.dao.TransactionDao
import com.fundory.app.data.local.entity.GoalSnapshotEntity
import com.fundory.app.data.local.entity.TransactionEntity
import com.fundory.app.domain.model.Transaction
import com.fundory.app.domain.model.TransactionType
import com.fundory.app.widget.FundoryWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
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
    private val goalSnapshotDao: GoalSnapshotDao,
    @param:ApplicationContext private val context: Context
) {

    val allTransactionsWithGoalsFlow: Flow<List<Transaction>> = combine(
        transactionDao.getAllTransactionsFlow(),
        goalDao.getAllGoalsFlow()
    ) { transactions, goals ->
        val goalMap = goals.associate { it.id to it.name }
        transactions.map { entity ->
            val gName = if (entity.goalId == null) "General Savings" else (goalMap[entity.goalId] ?: "Unknown Goal")
            val targetGName = if (entity.targetGoalId == null) null else (goalMap[entity.targetGoalId] ?: "General Savings")
            entity.toDomain(
                goalName = gName,
                targetGoalName = targetGName
            )
        }
    }

    val generalSavingsBalanceFlow: Flow<BigDecimal> = transactionDao.getGeneralSavingsTransactionsFlow().map { list ->
        val sum = list.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.toDomain().signedAmountGel) }
        (if (sum < BigDecimal.ZERO) BigDecimal.ZERO else sum).setScale(2, RoundingMode.HALF_UP)
    }

    val totalSavingsFlow: Flow<BigDecimal> = transactionDao.getAllTransactionsFlow().map { list ->
        val sum = list.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.toDomain().signedAmountGel) }
        (if (sum < BigDecimal.ZERO) BigDecimal.ZERO else sum).setScale(2, RoundingMode.HALF_UP)
    }

    fun getTransactionsByGoalFlow(goalId: String?): Flow<List<Transaction>> {
        val rawFlow = if (goalId == null) {
            transactionDao.getGeneralSavingsTransactionsFlow()
        } else {
            transactionDao.getTransactionsByGoalFlow(goalId)
        }

        return combine(rawFlow, goalDao.getAllGoalsFlow()) { transactions, goals ->
            val goalMap = goals.associate { it.id to it.name }
            transactions.map { entity ->
                val gName = if (entity.goalId == null) "General Savings" else (goalMap[entity.goalId] ?: "Unknown Goal")
                val targetGName = if (entity.targetGoalId == null) null else (goalMap[entity.targetGoalId] ?: "General Savings")
                entity.toDomain(
                    goalName = gName,
                    targetGoalName = targetGName
                )
            }
        }
    }

    suspend fun recordDeposit(
        goalId: String?,
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
        goalId: String?,
        amountGel: BigDecimal,
        note: String? = null,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ): TransactionResult {
        if (amountGel <= BigDecimal.ZERO) {
            return TransactionResult.Failure("Withdrawal amount must be greater than zero.")
        }

        if (goalId != null) {
            val currentGoal = goalDao.getGoalById(goalId)
                ?: return TransactionResult.Failure("Selected goal not found.")

            if (currentGoal.savedAmountGel < amountGel) {
                return TransactionResult.Failure(
                    "Insufficient saved balance: goal has ${currentGoal.savedAmountGel} GEL saved, cannot withdraw ${amountGel} GEL."
                )
            }
        } else {
            // General Savings withdrawal
            val generalTx = transactionDao.getGeneralSavingsTransactionsAscending()
            val generalBalance = generalTx.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.toDomain().signedAmountGel) }
            if (generalBalance < amountGel) {
                return TransactionResult.Failure(
                    "Insufficient balance in General Savings: available balance is ${generalBalance} GEL, cannot withdraw ${amountGel} GEL."
                )
            }
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
        goalId: String?,
        adjustedAmountGel: BigDecimal, // can be positive or negative
        note: String? = null,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ): TransactionResult {
        if (adjustedAmountGel == BigDecimal.ZERO) {
            return TransactionResult.Failure("Adjustment amount cannot be zero.")
        }

        if (goalId != null) {
            val currentGoal = goalDao.getGoalById(goalId)
                ?: return TransactionResult.Failure("Selected goal not found.")

            val newBalance = currentGoal.savedAmountGel.add(adjustedAmountGel)
            if (newBalance < BigDecimal.ZERO) {
                return TransactionResult.Failure("Adjustment would result in negative balance (${newBalance} GEL).")
            }
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
        sourceGoalId: String?,
        destinationGoalId: String?,
        amountGel: BigDecimal,
        note: String? = null,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ): TransactionResult {
        if (amountGel <= BigDecimal.ZERO) {
            return TransactionResult.Failure("Transfer amount must be greater than zero.")
        }
        if (sourceGoalId == destinationGoalId) {
            return TransactionResult.Failure("Cannot transfer to the same location.")
        }

        return database.withTransaction {
            val sourceName = if (sourceGoalId == null) {
                "General Savings"
            } else {
                goalDao.getGoalById(sourceGoalId)?.name
                    ?: return@withTransaction TransactionResult.Failure("Source goal not found.")
            }

            val destName = if (destinationGoalId == null) {
                "General Savings"
            } else {
                goalDao.getGoalById(destinationGoalId)?.name
                    ?: return@withTransaction TransactionResult.Failure("Destination goal not found.")
            }

            // Check source balance
            if (sourceGoalId != null) {
                val sourceGoal = goalDao.getGoalById(sourceGoalId)!!
                if (sourceGoal.savedAmountGel < amountGel) {
                    return@withTransaction TransactionResult.Failure(
                        "Insufficient balance in source goal (${sourceGoal.savedAmountGel} GEL)."
                    )
                }
            } else {
                val generalTx = transactionDao.getGeneralSavingsTransactionsAscending()
                val generalBalance = generalTx.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.toDomain().signedAmountGel) }
                if (generalBalance < amountGel) {
                    return@withTransaction TransactionResult.Failure(
                        "Insufficient balance in General Savings (${generalBalance} GEL)."
                    )
                }
            }

            // 1. Outgoing transfer transaction
            val outgoingTx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                goalId = sourceGoalId,
                type = TransactionType.TRANSFER,
                amountGel = amountGel,
                targetGoalId = destinationGoalId,
                timestampEpochMillis = timestampEpochMillis,
                note = note ?: "Transfer to $destName"
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
                note = note ?: "Transfer from $sourceName"
            )
            transactionDao.insertTransaction(incomingTx)

            // 3. Recalculate goal balances
            if (sourceGoalId != null) recalculateGoalBalanceInternal(sourceGoalId)
            if (destinationGoalId != null) recalculateGoalBalanceInternal(destinationGoalId)

            TransactionResult.Success
        }
    }

    suspend fun transferFunds(
        fromGoalId: String?,
        toGoalId: String?,
        amountGel: BigDecimal,
        note: String? = null
    ): TransactionResult {
        if (amountGel <= BigDecimal.ZERO) {
            return TransactionResult.Failure("Transfer amount must be greater than zero.")
        }
        if (fromGoalId == toGoalId) {
            return TransactionResult.Failure("Source and destination must be different.")
        }

        return database.withTransaction {
            val currentSourceBalance: BigDecimal = if (fromGoalId != null) {
                goalDao.getGoalById(fromGoalId)?.savedAmountGel ?: BigDecimal.ZERO
            } else {
                val generalTxs = transactionDao.getGeneralSavingsTransactionsAscending()
                generalTxs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.toDomain().signedAmountGel) }
            }

            if (currentSourceBalance < amountGel) {
                return@withTransaction TransactionResult.Failure("Insufficient funds in source for transfer. Available: $currentSourceBalance GEL")
            }

            val withdrawalTx = Transaction(
                id = UUID.randomUUID().toString(),
                goalId = fromGoalId,
                targetGoalId = toGoalId,
                type = TransactionType.TRANSFER,
                amountGel = amountGel,
                timestampEpochMillis = System.currentTimeMillis(),
                note = note ?: "Transfer"
            )
            transactionDao.insertTransaction(TransactionEntity.fromDomain(withdrawalTx))
            if (fromGoalId != null) {
                recalculateGoalBalanceInternal(fromGoalId)
            }

            val depositTx = Transaction(
                id = UUID.randomUUID().toString(),
                goalId = toGoalId,
                targetGoalId = fromGoalId,
                type = TransactionType.DEPOSIT,
                amountGel = amountGel,
                timestampEpochMillis = System.currentTimeMillis() + 1,
                note = note ?: "Transfer from ${fromGoalId ?: "General Savings"}"
            )
            transactionDao.insertTransaction(TransactionEntity.fromDomain(depositTx))
            if (toGoalId != null) {
                recalculateGoalBalanceInternal(toGoalId)
            }

            try {
                FundoryWidgetProvider.updateAllWidgets(context)
            } catch (_: Exception) {}

            TransactionResult.Success
        }
    }

    suspend fun deleteTransaction(transactionId: String): TransactionResult {
        return database.withTransaction {
            val tx = transactionDao.getTransactionById(transactionId)
                ?: return@withTransaction TransactionResult.Failure("Transaction not found.")

            transactionDao.deleteTransactionById(transactionId)
            if (tx.goalId != null) {
                recalculateGoalBalanceInternal(tx.goalId)
            }
            if (tx.targetGoalId != null) {
                recalculateGoalBalanceInternal(tx.targetGoalId)
            }

            TransactionResult.Success
        }
    }

    suspend fun updateTransaction(transaction: Transaction): TransactionResult {
        return database.withTransaction {
            transactionDao.updateTransaction(TransactionEntity.fromDomain(transaction))
            if (transaction.goalId != null) {
                recalculateGoalBalanceInternal(transaction.goalId)
            }
            if (transaction.targetGoalId != null) {
                recalculateGoalBalanceInternal(transaction.targetGoalId)
            }
            TransactionResult.Success
        }
    }

    private suspend fun executeAtomicTransaction(transaction: Transaction): TransactionResult {
        return database.withTransaction {
            transactionDao.insertTransaction(TransactionEntity.fromDomain(transaction))
            if (transaction.goalId != null) {
                val recalculated = recalculateGoalBalanceInternal(transaction.goalId)
                if (recalculated < BigDecimal.ZERO) {
                    throw IllegalStateException("Transaction caused negative balance: $recalculated")
                }
            }
            try {
                FundoryWidgetProvider.updateAllWidgets(context)
            } catch (_: Exception) {}
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
