package com.fundory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fundory.app.data.local.entity.GoalSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalSnapshotDao {

    @Query("SELECT * FROM goal_snapshots WHERE goalId = :goalId ORDER BY timestampEpochMillis ASC")
    fun getSnapshotsForGoalFlow(goalId: String): Flow<List<GoalSnapshotEntity>>

    @Query("SELECT * FROM goal_snapshots WHERE goalId = :goalId ORDER BY timestampEpochMillis ASC")
    suspend fun getSnapshotsForGoal(goalId: String): List<GoalSnapshotEntity>

    @Query("SELECT * FROM goal_snapshots ORDER BY timestampEpochMillis ASC")
    fun getAllSnapshotsFlow(): Flow<List<GoalSnapshotEntity>>

    @Query("SELECT * FROM goal_snapshots ORDER BY timestampEpochMillis ASC")
    suspend fun getAllSnapshots(): List<GoalSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: GoalSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<GoalSnapshotEntity>)

    @Query("DELETE FROM goal_snapshots WHERE goalId = :goalId")
    suspend fun deleteSnapshotsForGoal(goalId: String)

    @Query("DELETE FROM goal_snapshots")
    suspend fun deleteAllSnapshots()
}

