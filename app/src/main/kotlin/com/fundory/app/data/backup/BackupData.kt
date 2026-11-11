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
    @SerialName("version") val version: Int = 2,
    @SerialName("exportDateEpochMillis") val exportDateEpochMillis: Long = System.currentTimeMillis(),
    @SerialName("appName") val appName: String = "Fundory",
    @SerialName("goals") val goals: List<BackupGoal> = emptyList(),
    @SerialName("products") val products: List<BackupProduct> = emptyList(),
    @SerialName("transactions") val transactions: List<BackupTransaction> = emptyList(),
    @SerialName("snapshots") val snapshots: List<BackupSnapshot> = emptyList(),
    @SerialName("priceHistory") val priceHistory: List<BackupPriceHistory> = emptyList(),
    @SerialName("parcels") val parcels: List<BackupParcel> = emptyList()
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
    val goalId: String? = null,
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
    val lastEditedEpochMillis: Long,
    val purchaseStatus: String = "NOT_PURCHASED",
    val actualPrice: String? = null,
    val actualPriceCurrency: String? = null,
    val shippingCarrier: String? = null,
    val trackingNumber: String? = null,
    val expectedDeliveryDate: String? = null,
    val actualDeliveryDate: String? = null,
    val trackingNotes: String? = null,
    val alternativeGroupId: String? = null,
    val isSelectedAlternative: Boolean = true,
    val parcelId: String? = null,
    val marketplace: String? = null
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

@Serializable
data class BackupPriceHistory(
    val id: String,
    val productId: String,
    val price: String,
    val currency: String,
    val recordedAtEpochMillis: Long,
    val note: String? = null
)

@Serializable
data class BackupParcel(
    val id: String,
    val goalId: String? = null,
    val name: String,
    val carrier: String,
    val method: String,
    val weightKg: Double,
    val lengthCm: Double? = null,
    val widthCm: Double? = null,
    val heightCm: Double? = null,
    val shippingCostGel: String,
    val vatGel: String = "0.00",
    val customsFeeGel: String = "0.00",
    val otherFeesGel: String = "0.00",
    val productSubtotalGel: String = "0.00",
    val isConfirmed: Boolean = false,
    val trackingNumber: String? = null,
    val createdAtEpochMillis: Long
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

            // Verify integrity: non-null goalId must reference a valid goal in the backup
            val goalIds = backup.goals.map { it.id }.toSet()
            val orphanProducts = backup.products.filter { it.goalId != null && it.goalId !in goalIds }
            val orphanTransactions = backup.transactions.filter { it.goalId != null && it.goalId !in goalIds }

            if (orphanProducts.isNotEmpty() || orphanTransactions.isNotEmpty()) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "Integrity check failed: file contains orphaned items (${orphanProducts.size} product(s), ${orphanTransactions.size} transaction(s) reference non-existent goals)."
                )
            }

            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val formattedDate = formatter.format(Date(backup.exportDateEpochMillis))

            BackupValidationResult(
                isValid = true,
                goalCount = backup.goals.size,
                productCount = backup.products.size,
                transactionCount = backup.transactions.size,
                snapshotCount = backup.snapshots.size,
                exportDateFormatted = formattedDate,
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
