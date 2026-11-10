package com.fundory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fundory.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE goalId = :goalId ORDER BY sortOrder ASC, dateAddedEpochMillis ASC")
    fun getProductsByGoalFlow(goalId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE goalId = :goalId ORDER BY sortOrder ASC, dateAddedEpochMillis ASC")
    suspend fun getProductsByGoal(goalId: String): List<ProductEntity>

    @Query("SELECT * FROM products ORDER BY dateAddedEpochMillis ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("DELETE FROM products WHERE goalId = :goalId")
    suspend fun deleteProductsByGoal(goalId: String)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}

