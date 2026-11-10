package com.fundory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fundory.app.data.local.entity.ParcelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParcelDao {

    @Query("SELECT * FROM parcels WHERE goalId = :goalId ORDER BY createdAtEpochMillis DESC")
    fun getParcelsByGoalFlow(goalId: String): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE goalId = :goalId ORDER BY createdAtEpochMillis DESC")
    suspend fun getParcelsByGoal(goalId: String): List<ParcelEntity>

    @Query("SELECT * FROM parcels ORDER BY createdAtEpochMillis DESC")
    fun getAllParcelsFlow(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels")
    suspend fun getAllParcels(): List<ParcelEntity>

    @Query("SELECT * FROM parcels WHERE id = :id")
    suspend fun getParcelById(id: String): ParcelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParcel(parcel: ParcelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParcels(parcels: List<ParcelEntity>)

    @Update
    suspend fun updateParcel(parcel: ParcelEntity)

    @Query("DELETE FROM parcels WHERE id = :id")
    suspend fun deleteParcel(id: String)

    @Query("DELETE FROM parcels WHERE goalId = :goalId")
    suspend fun deleteParcelsByGoal(goalId: String)
}

