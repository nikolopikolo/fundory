package com.fundory.app.data.repository

import com.fundory.app.data.local.dao.ProductDao
import com.fundory.app.data.local.entity.ProductEntity
import com.fundory.app.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val goalRepository: GoalRepository
) {

    fun getProductsByGoalFlow(goalId: String): Flow<List<Product>> {
        return productDao.getProductsByGoalFlow(goalId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)?.toDomain()
    }

    suspend fun saveProduct(product: Product) {
        val entity = ProductEntity.fromDomain(
            product.copy(lastEditedEpochMillis = System.currentTimeMillis())
        )
        productDao.insertProduct(entity)
        goalRepository.recalculateGoalTarget(product.goalId)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProductById(product.id)
        goalRepository.recalculateGoalTarget(product.goalId)
    }

    suspend fun updateProductOrder(goalId: String, orderedProductIds: List<String>) {
        val products = productDao.getProductsByGoal(goalId)
        val productMap = products.associateBy { it.id }
        val updatedList = mutableListOf<ProductEntity>()

        orderedProductIds.forEachIndexed { index, id ->
            productMap[id]?.let { existing ->
                updatedList.add(existing.copy(sortOrder = index))
            }
        }
        if (updatedList.isNotEmpty()) {
            productDao.insertProducts(updatedList)
        }
    }
}

