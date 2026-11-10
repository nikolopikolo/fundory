package com.fundory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fundory.app.data.local.entity.PriceHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {

    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY recordedAtEpochMillis ASC")
    fun getPriceHistoryForProductFlow(productId: String): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY recordedAtEpochMillis ASC")
    suspend fun getPriceHistoryForProduct(productId: String): List<PriceHistoryEntity>

    @Query("SELECT * FROM price_history ORDER BY recordedAtEpochMillis ASC")
    fun getAllPriceHistoryFlow(): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history")
    suspend fun getAllPriceHistory(): List<PriceHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceRecord(record: PriceHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceRecords(records: List<PriceHistoryEntity>)

    @Query("DELETE FROM price_history WHERE id = :id")
    suspend fun deletePriceRecord(id: String)

    @Query("DELETE FROM price_history WHERE productId = :productId")
    suspend fun deletePriceHistoryForProduct(productId: String)
}

