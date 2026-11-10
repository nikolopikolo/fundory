package com.fundory.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fundory.app.domain.model.Transaction
import com.fundory.app.domain.model.TransactionType
import java.math.BigDecimal

@Entity(
    tableName = "transactions",
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
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val goalId: String,
    val type: TransactionType,
    val amountGel: BigDecimal,
    val targetGoalId: String?,
    val timestampEpochMillis: Long,
    val note: String?
) {
    fun toDomain(goalName: String? = null, targetGoalName: String? = null): Transaction = Transaction(
        id = id,
        goalId = goalId,
        type = type,
        amountGel = amountGel,
        targetGoalId = targetGoalId,
        timestampEpochMillis = timestampEpochMillis,
        note = note,
        goalName = goalName,
        targetGoalName = targetGoalName
    )

    companion object {
        fun fromDomain(transaction: Transaction): TransactionEntity = TransactionEntity(
            id = transaction.id,
            goalId = transaction.goalId,
            type = transaction.type,
            amountGel = transaction.amountGel,
            targetGoalId = transaction.targetGoalId,
            timestampEpochMillis = transaction.timestampEpochMillis,
            note = transaction.note
        )
    }
}
