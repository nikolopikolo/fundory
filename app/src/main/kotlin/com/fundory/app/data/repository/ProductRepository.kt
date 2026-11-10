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
    private val goalRepository: GoalRepository,
    private val priceHistoryRepository: PriceHistoryRepository
) {

    fun getProductsByGoalFlow(goalId: String): Flow<List<Product>> {
        return productDao.getProductsByGoalFlow(goalId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getWishlistProductsFlow(): Flow<List<Product>> {
        return productDao.getWishlistProductsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getTrackingProductsFlow(): Flow<List<Product>> {
        return productDao.getTrackingProductsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAlternativesFlow(groupId: String): Flow<List<Product>> {
        return productDao.getAlternativesFlow(groupId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)?.toDomain()
    }

    suspend fun saveProduct(product: Product) {
        val existing = productDao.getProductById(product.id)
        val entity = ProductEntity.fromDomain(
            product.copy(lastEditedEpochMillis = System.currentTimeMillis())
        )
        productDao.insertProduct(entity)

        // Log price history if new product or price changed
        if (existing == null) {
            priceHistoryRepository.recordPriceChange(
                productId = product.id,
                price = product.originalPrice,
                currency = product.currency,
                note = "Initial price"
            )
        } else if (existing.originalPrice.compareTo(product.originalPrice) != 0 || existing.currency != product.currency) {
            priceHistoryRepository.recordPriceChange(
                productId = product.id,
                price = product.originalPrice,
                currency = product.currency,
                note = "Price updated"
            )
        }

        product.goalId?.let { goalRepository.recalculateGoalTarget(it) }
    }

    suspend fun selectAlternative(groupId: String, selectedProductId: String) {
        val products = productDao.getAllProducts().filter { it.alternativeGroupId == groupId }
        val updated = products.map {
            it.copy(isSelectedAlternative = (it.id == selectedProductId))
        }
        productDao.insertProducts(updated)
        updated.firstOrNull()?.goalId?.let { goalRepository.recalculateGoalTarget(it) }
    }

    suspend fun moveProductToGoal(productId: String, targetGoalId: String) {
        val existing = productDao.getProductById(productId) ?: return
        val oldGoalId = existing.goalId
        val updated = existing.copy(goalId = targetGoalId, lastEditedEpochMillis = System.currentTimeMillis())
        productDao.insertProduct(updated)

        oldGoalId?.let { goalRepository.recalculateGoalTarget(it) }
        goalRepository.recalculateGoalTarget(targetGoalId)
    }

    suspend fun moveProductToWishlist(productId: String) {
        val existing = productDao.getProductById(productId) ?: return
        val oldGoalId = existing.goalId
        val updated = existing.copy(goalId = null, lastEditedEpochMillis = System.currentTimeMillis())
        productDao.insertProduct(updated)

        oldGoalId?.let { goalRepository.recalculateGoalTarget(it) }
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProductById(product.id)
        product.goalId?.let { goalRepository.recalculateGoalTarget(it) }
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
