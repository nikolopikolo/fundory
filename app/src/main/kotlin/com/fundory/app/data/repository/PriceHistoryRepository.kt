package com.fundory.app.data.repository

import com.fundory.app.data.local.dao.PriceHistoryDao
import com.fundory.app.data.local.entity.PriceHistoryEntity
import com.fundory.app.domain.model.PriceHistoryRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceHistoryRepository @Inject constructor(
    private val priceHistoryDao: PriceHistoryDao
) {

    fun getPriceHistoryForProductFlow(productId: String): Flow<List<PriceHistoryRecord>> {
        return priceHistoryDao.getPriceHistoryForProductFlow(productId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getPriceHistoryForProduct(productId: String): List<PriceHistoryRecord> {
        return priceHistoryDao.getPriceHistoryForProduct(productId).map { it.toDomain() }
    }

    suspend fun recordPriceChange(productId: String, price: BigDecimal, currency: String, note: String? = null) {
        val entity = PriceHistoryEntity(
            id = java.util.UUID.randomUUID().toString(),
            productId = productId,
            price = price,
            currency = currency,
            recordedAtEpochMillis = System.currentTimeMillis(),
            note = note
        )
        priceHistoryDao.insertPriceRecord(entity)
    }

    suspend fun deletePriceRecord(id: String) {
        priceHistoryDao.deletePriceRecord(id)
    }
}

