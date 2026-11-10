package com.fundory.app.data.repository

import com.fundory.app.data.local.FundoryDatabase
import com.fundory.app.data.local.dao.GoalDao
import com.fundory.app.data.local.dao.GoalSnapshotDao
import com.fundory.app.data.local.dao.ProductDao
import com.fundory.app.data.local.dao.TransactionDao
import com.fundory.app.data.local.entity.GoalEntity
import com.fundory.app.data.local.entity.GoalSnapshotEntity
import com.fundory.app.data.local.entity.ProductEntity
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val database: FundoryDatabase,
    private val goalDao: GoalDao,
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao,
    private val goalSnapshotDao: GoalSnapshotDao,
    private val currencyRepository: CurrencyRepository
) {

    val allGoalsWithProductsFlow: Flow<List<Goal>> = combine(
        goalDao.getAllGoalsFlow(),
        productDao.getAllProductsFlow()
    ) { goals, products ->
        val productMap = products.groupBy { it.goalId }
        goals.map { goalEntity ->
            val goalProducts = productMap[goalEntity.id]?.map { it.toDomain() } ?: emptyList()
            goalEntity.toDomain().copy(products = goalProducts)
        }
    }

    val activeGoalsWithProductsFlow: Flow<List<Goal>> = combine(
        goalDao.getActiveGoalsFlow(),
        productDao.getAllProductsFlow()
    ) { goals, products ->
        val productMap = products.groupBy { it.goalId }
        goals.map { goalEntity ->
            val goalProducts = productMap[goalEntity.id]?.map { it.toDomain() } ?: emptyList()
            goalEntity.toDomain().copy(products = goalProducts)
        }
    }

    fun getGoalByIdFlow(id: String): Flow<Goal?> = combine(
        goalDao.getGoalByIdFlow(id),
        productDao.getProductsByGoalFlow(id)
    ) { goalEntity, products ->
        goalEntity?.toDomain()?.copy(products = products.map { it.toDomain() })
    }

    suspend fun getGoalById(id: String): Goal? {
        val entity = goalDao.getGoalById(id) ?: return null
        val products = productDao.getProductsByGoal(id).map { it.toDomain() }
        return entity.toDomain().copy(products = products)
    }

    suspend fun saveGoal(goal: Goal) {
        val existing = goalDao.getGoalById(goal.id)
        val finalGoal = if (existing != null) {
            goal.copy(
                savedAmountGel = existing.savedAmountGel,
                lastUpdatedEpochMillis = System.currentTimeMillis()
            )
        } else {
            goal.copy(lastUpdatedEpochMillis = System.currentTimeMillis())
        }
        val entity = GoalEntity.fromDomain(finalGoal)
        goalDao.insertGoal(entity)
        recordSnapshot(finalGoal)
    }

    suspend fun updateGoalStatus(goalId: String, status: GoalStatus) {
        val existing = goalDao.getGoalById(goalId) ?: return
        val updated = existing.copy(
            status = status,
            lastUpdatedEpochMillis = System.currentTimeMillis()
        )
        goalDao.updateGoal(updated)
    }

    suspend fun duplicateGoal(goalId: String): String? {
        val existingGoal = getGoalById(goalId) ?: return null
        val newGoalId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val duplicatedGoal = existingGoal.copy(
            id = newGoalId,
            name = "${existingGoal.name} (Copy)",
            savedAmountGel = BigDecimal.ZERO, // Reset saved balance for a new goal
            status = GoalStatus.ACTIVE,
            createdDateEpochMillis = now,
            lastUpdatedEpochMillis = now
        )

        goalDao.insertGoal(GoalEntity.fromDomain(duplicatedGoal))

        // Duplicate products
        val duplicatedProducts = existingGoal.products.map { product ->
            ProductEntity.fromDomain(
                product.copy(
                    id = UUID.randomUUID().toString(),
                    goalId = newGoalId,
                    dateAddedEpochMillis = now,
                    lastEditedEpochMillis = now
                )
            )
        }
        if (duplicatedProducts.isNotEmpty()) {
            productDao.insertProducts(duplicatedProducts)
        }

        recordSnapshot(duplicatedGoal)
        return newGoalId
    }

    suspend fun deleteGoal(goalId: String) {
        // Cascade delete handled by SQLite foreign keys and explicit queries
        productDao.deleteProductsByGoal(goalId)
        transactionDao.deleteTransactionsByGoal(goalId)
        goalSnapshotDao.deleteSnapshotsForGoal(goalId)
        goalDao.deleteGoalById(goalId)
    }

    suspend fun recalculateGoalTarget(goalId: String) {
        val goal = goalDao.getGoalById(goalId) ?: return
        if (!goal.isTargetCalculatedFromProducts) return

        val products = productDao.getProductsByGoal(goalId).map { it.toDomain() }
        val rates = currencyRepository.getCurrentRatesToGel()

        var totalGel = BigDecimal.ZERO
        for (prod in products) {
            if (prod.isSelectedAlternative) {
                totalGel = totalGel.add(prod.calculateTotalGel(rates))
            }
        }

        val updated = goal.copy(
            targetAmountGel = totalGel,
            lastUpdatedEpochMillis = System.currentTimeMillis()
        )
        goalDao.updateGoal(updated)
        recordSnapshot(updated.toDomain())
    }

    private suspend fun recordSnapshot(goal: Goal) {
        val snapshot = GoalSnapshotEntity(
            goalId = goal.id,
            timestampEpochMillis = System.currentTimeMillis(),
            savedAmountGel = goal.savedAmountGel,
            targetAmountGel = goal.targetAmountGel,
            completionPercentage = goal.progressPercentage
        )
        goalSnapshotDao.insertSnapshot(snapshot)
    }
}

