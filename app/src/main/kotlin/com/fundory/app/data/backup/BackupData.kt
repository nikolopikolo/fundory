package com.fundory.app.data.backup

import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.domain.model.TransactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FundoryBackup(
    @SerialName("version") val version: Int = 1,
    @SerialName("exportDateEpochMillis") val exportDateEpochMillis: Long = System.currentTimeMillis(),
    @SerialName("appName") val appName: String = "Fundory",
    @SerialName("goals") val goals: List<BackupGoal> = emptyList(),
    @SerialName("products") val products: List<BackupProduct> = emptyList(),
    @SerialName("transactions") val transactions: List<BackupTransaction> = emptyList(),
    @SerialName("snapshots") val snapshots: List<BackupSnapshot> = emptyList()
)

@Serializable
data class BackupGoal(
    val id: String,
    val name: String,
    val description: String = "",
    val iconName: String = "savings",
    val colorHex: String = "#38B273",
    val targetAmountGel: String,
    val isTargetCalculatedFromProducts: Boolean = true,
    val savedAmountGel: String,
    val monthlyPlanGel: String,
    val targetDateEpochMillis: Long? = null,
    val status: String = GoalStatus.ACTIVE.name,
    val createdDateEpochMillis: Long,
    val lastUpdatedEpochMillis: Long
)

@Serializable
data class BackupProduct(
    val id: String,
    val goalId: String,
    val name: String,
    val description: String = "",
    val originalPrice: String,
    val currency: String,
    val quantity: Int = 1,
    val productUrl: String? = null,
    val imageUrl: String? = null,
    val shippingCost: String = "0.00",
    val shippingCurrency: String = "USD",
    val taxesAndFees: String = "0.00",
    val taxesCurrency: String = "USD",
    val manualOverrideGel: String? = null,
    val sortOrder: Int = 0,
    val dateAddedEpochMillis: Long,
    val lastEditedEpochMillis: Long
)

@Serializable
data class BackupTransaction(
    val id: String,
    val goalId: String,
    val type: String = TransactionType.DEPOSIT.name,
    val amountGel: String,
    val targetGoalId: String? = null,
    val timestampEpochMillis: Long,
    val note: String? = null
)

@Serializable
data class BackupSnapshot(
    val goalId: String,
    val timestampEpochMillis: Long,
    val savedAmountGel: String,
    val targetAmountGel: String,
    val completionPercentage: Float
)

data class BackupValidationResult(
    val isValid: Boolean,
    val goalCount: Int = 0,
    val productCount: Int = 0,
    val transactionCount: Int = 0,
    val snapshotCount: Int = 0,
    val exportDateFormatted: String = "",
    val errorMessage: String? = null,
    val backupData: FundoryBackup? = null
)
