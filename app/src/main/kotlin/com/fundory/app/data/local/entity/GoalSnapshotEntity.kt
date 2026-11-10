package com.fundory.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "goal_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"]), Index(value = ["timestampEpochMillis"])]
)
data class GoalSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: String,
    val timestampEpochMillis: Long,
    val savedAmountGel: BigDecimal,
    val targetAmountGel: BigDecimal,
    val completionPercentage: Float
)
