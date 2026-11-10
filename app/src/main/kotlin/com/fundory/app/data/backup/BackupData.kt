package com.fundory.app.data.backup

import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.domain.model.TransactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val originalTargetAmount: String = targetAmountGel,
    val targetCurrency: String = "GEL",
    val isTargetCalculatedFromProducts: Boolean = false,
    val savedAmountGel: String,
    val monthlyPlanGel: String = "0.00",
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
    val goalId: String? = null,
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

object BackupValidator {
    fun validate(
        rawJson: String,
        json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
    ): BackupValidationResult {
        return try {
            val backup = json.decodeFromString<FundoryBackup>(rawJson)
            if (backup.goals.isEmpty() && backup.products.isEmpty() && backup.transactions.isEmpty()) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "The provided file contains no Fundory records."
                )
            }

            // Verify integrity: every product and transaction must reference a valid goal in the backup
            val goalIds = backup.goals.map { it.id }.toSet()
            val orphanProducts = backup.products.filter { it.goalId !in goalIds }
            val orphanTransactions = backup.transactions.filter { it.goalId != null && it.goalId !in goalIds }

            if (orphanProducts.isNotEmpty() || orphanTransactions.isNotEmpty()) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "Data integrity error: file contains orphaned items or transactions without a parent goal."
                )
            }

            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
            BackupValidationResult(
                isValid = true,
                goalCount = backup.goals.size,
                productCount = backup.products.size,
                transactionCount = backup.transactions.size,
                snapshotCount = backup.snapshots.size,
                exportDateFormatted = dateFormat.format(Date(backup.exportDateEpochMillis)),
                backupData = backup
            )
        } catch (e: Exception) {
            BackupValidationResult(
                isValid = false,
                errorMessage = "Invalid JSON format: ${e.localizedMessage ?: "Unknown parsing error"}"
            )
        }
    }
}
