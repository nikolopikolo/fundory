package com.fundory.app.data.backup

import androidx.room.withTransaction
import com.fundory.app.data.local.FundoryDatabase
import com.fundory.app.data.local.dao.GoalDao
import com.fundory.app.data.local.dao.GoalSnapshotDao
import com.fundory.app.data.local.dao.ParcelDao
import com.fundory.app.data.local.dao.PriceHistoryDao
import com.fundory.app.data.local.dao.ProductDao
import com.fundory.app.data.local.dao.TransactionDao
import com.fundory.app.data.local.entity.GoalEntity
import com.fundory.app.data.local.entity.GoalSnapshotEntity
import com.fundory.app.data.local.entity.ParcelEntity
import com.fundory.app.data.local.entity.PriceHistoryEntity
import com.fundory.app.data.local.entity.ProductEntity
import com.fundory.app.data.local.entity.TransactionEntity
import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.domain.model.TransactionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val database: FundoryDatabase,
    private val goalDao: GoalDao,
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao,
    private val goalSnapshotDao: GoalSnapshotDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val parcelDao: ParcelDao,
    private val json: Json
) {

    suspend fun generateBackupJson(): String {
        val goals = goalDao.getAllGoals()
        val products = productDao.getAllProducts()
        val transactions = transactionDao.getAllTransactions()
        val snapshots = goalSnapshotDao.getAllSnapshots()
        val priceHistory = priceHistoryDao.getAllPriceHistory()
        val parcels = parcelDao.getAllParcels()

        val backup = FundoryBackup(
            version = 2,
            exportDateEpochMillis = System.currentTimeMillis(),
            appName = "Fundory",
            goals = goals.map { g ->
                BackupGoal(
                    id = g.id,
                    name = g.name,
                    description = g.description,
                    iconName = g.iconName,
                    colorHex = g.colorHex,
                    targetAmountGel = g.targetAmountGel.toPlainString(),
                    originalTargetAmount = g.originalTargetAmount.toPlainString(),
                    targetCurrency = g.targetCurrency,
                    isTargetCalculatedFromProducts = g.isTargetCalculatedFromProducts,
                    savedAmountGel = g.savedAmountGel.toPlainString(),
                    monthlyPlanGel = g.monthlyPlanGel.toPlainString(),
                    targetDateEpochMillis = g.targetDateEpochMillis,
                    status = g.status.name,
                    createdDateEpochMillis = g.createdDateEpochMillis,
                    lastUpdatedEpochMillis = g.lastUpdatedEpochMillis
                )
            },
            products = products.map { p ->
                BackupProduct(
                    id = p.id,
                    goalId = p.goalId,
                    name = p.name,
                    description = p.description,
                    originalPrice = p.originalPrice.toPlainString(),
                    currency = p.currency,
                    quantity = p.quantity,
                    productUrl = p.productUrl,
                    imageUrl = p.imageUrl,
                    shippingCost = p.shippingCost.toPlainString(),
                    shippingCurrency = p.shippingCurrency,
                    taxesAndFees = p.taxesAndFees.toPlainString(),
                    taxesCurrency = p.taxesCurrency,
                    manualOverrideGel = p.manualOverrideGel?.toPlainString(),
                    sortOrder = p.sortOrder,
                    dateAddedEpochMillis = p.dateAddedEpochMillis,
                    lastEditedEpochMillis = p.lastEditedEpochMillis,
                    purchaseStatus = p.purchaseStatus,
                    actualPrice = p.actualPrice?.toPlainString(),
                    actualPriceCurrency = p.actualPriceCurrency,
                    shippingCarrier = p.shippingCarrier,
                    trackingNumber = p.trackingNumber,
                    expectedDeliveryDate = p.expectedDeliveryDate,
                    actualDeliveryDate = p.actualDeliveryDate,
                    trackingNotes = p.trackingNotes,
                    alternativeGroupId = p.alternativeGroupId,
                    isSelectedAlternative = p.isSelectedAlternative,
                    parcelId = p.parcelId,
                    marketplace = p.marketplace
                )
            },
            transactions = transactions.map { t ->
                BackupTransaction(
                    id = t.id,
                    goalId = t.goalId,
                    type = t.type.name,
                    amountGel = t.amountGel.toPlainString(),
                    targetGoalId = t.targetGoalId,
                    timestampEpochMillis = t.timestampEpochMillis,
                    note = t.note
                )
            },
            snapshots = snapshots.map { s ->
                BackupSnapshot(
                    goalId = s.goalId,
                    timestampEpochMillis = s.timestampEpochMillis,
                    savedAmountGel = s.savedAmountGel.toPlainString(),
                    targetAmountGel = s.targetAmountGel.toPlainString(),
                    completionPercentage = s.completionPercentage
                )
            },
            priceHistory = priceHistory.map { ph ->
                BackupPriceHistory(
                    id = ph.id,
                    productId = ph.productId,
                    price = ph.price.toPlainString(),
                    currency = ph.currency,
                    recordedAtEpochMillis = ph.recordedAtEpochMillis,
                    note = ph.note
                )
            },
            parcels = parcels.map { par ->
                BackupParcel(
                    id = par.id,
                    goalId = par.goalId,
                    name = par.name,
                    carrier = par.carrier,
                    method = par.method,
                    weightKg = par.weightKg,
                    lengthCm = par.lengthCm,
                    widthCm = par.widthCm,
                    heightCm = par.heightCm,
                    shippingCostGel = par.shippingCostGel.toPlainString(),
                    isConfirmed = par.isConfirmed,
                    trackingNumber = par.trackingNumber,
                    createdAtEpochMillis = par.createdAtEpochMillis
                )
            }
        )

        return json.encodeToString(backup)
    }

    fun validateBackupJson(rawJson: String): BackupValidationResult {
        return BackupValidator.validate(rawJson, json)
    }

    suspend fun restoreData(backup: FundoryBackup, isReplaceMode: Boolean) {
        database.withTransaction {
            if (isReplaceMode) {
                parcelDao.getAllParcels().forEach { parcelDao.deleteParcel(it.id) }
                priceHistoryDao.getAllPriceHistory().forEach { priceHistoryDao.deletePriceRecord(it.id) }
                goalSnapshotDao.deleteAllSnapshots()
                transactionDao.deleteAllTransactions()
                productDao.deleteAllProducts()
                goalDao.deleteAllGoals()
            }

            // Insert Goals
            val goalEntities = backup.goals.map { g ->
                GoalEntity(
                    id = g.id,
                    name = g.name,
                    description = g.description,
                    iconName = g.iconName,
                    colorHex = g.colorHex,
                    targetAmountGel = BigDecimal(g.targetAmountGel),
                    originalTargetAmount = try { BigDecimal(g.originalTargetAmount) } catch (_: Exception) { BigDecimal(g.targetAmountGel) },
                    targetCurrency = g.targetCurrency,
                    isTargetCalculatedFromProducts = g.isTargetCalculatedFromProducts,
                    savedAmountGel = BigDecimal(g.savedAmountGel),
                    monthlyPlanGel = BigDecimal(g.monthlyPlanGel),
                    targetDateEpochMillis = g.targetDateEpochMillis,
                    status = try { GoalStatus.valueOf(g.status) } catch (_: Exception) { GoalStatus.ACTIVE },
                    createdDateEpochMillis = g.createdDateEpochMillis,
                    lastUpdatedEpochMillis = g.lastUpdatedEpochMillis
                )
            }
            goalDao.insertGoals(goalEntities)

            // Insert Products
            val productEntities = backup.products.map { p ->
                ProductEntity(
                    id = p.id,
                    goalId = p.goalId,
                    name = p.name,
                    description = p.description,
                    originalPrice = BigDecimal(p.originalPrice),
                    currency = p.currency,
                    quantity = p.quantity,
                    productUrl = p.productUrl,
                    imageUrl = p.imageUrl,
                    shippingCost = BigDecimal(p.shippingCost),
                    shippingCurrency = p.shippingCurrency,
                    taxesAndFees = BigDecimal(p.taxesAndFees),
                    taxesCurrency = p.taxesCurrency,
                    manualOverrideGel = p.manualOverrideGel?.let { BigDecimal(it) },
                    sortOrder = p.sortOrder,
                    dateAddedEpochMillis = p.dateAddedEpochMillis,
                    lastEditedEpochMillis = p.lastEditedEpochMillis,
                    purchaseStatus = p.purchaseStatus,
                    actualPrice = p.actualPrice?.let { BigDecimal(it) },
                    actualPriceCurrency = p.actualPriceCurrency,
                    shippingCarrier = p.shippingCarrier,
                    trackingNumber = p.trackingNumber,
                    expectedDeliveryDate = p.expectedDeliveryDate,
                    actualDeliveryDate = p.actualDeliveryDate,
                    trackingNotes = p.trackingNotes,
                    alternativeGroupId = p.alternativeGroupId,
                    isSelectedAlternative = p.isSelectedAlternative,
                    parcelId = p.parcelId,
                    marketplace = p.marketplace
                )
            }
            productDao.insertProducts(productEntities)

            // Insert Transactions
            val transactionEntities = backup.transactions.map { t ->
                TransactionEntity(
                    id = t.id,
                    goalId = t.goalId,
                    type = try { TransactionType.valueOf(t.type) } catch (_: Exception) { TransactionType.DEPOSIT },
                    amountGel = BigDecimal(t.amountGel),
                    targetGoalId = t.targetGoalId,
                    timestampEpochMillis = t.timestampEpochMillis,
                    note = t.note
                )
            }
            transactionDao.insertTransactions(transactionEntities)

            // Insert Snapshots
            val snapshotEntities = backup.snapshots.map { s ->
                GoalSnapshotEntity(
                    goalId = s.goalId,
                    timestampEpochMillis = s.timestampEpochMillis,
                    savedAmountGel = BigDecimal(s.savedAmountGel),
                    targetAmountGel = BigDecimal(s.targetAmountGel),
                    completionPercentage = s.completionPercentage
                )
            }
            goalSnapshotDao.insertSnapshots(snapshotEntities)

            // Insert Price History
            val priceHistoryEntities = backup.priceHistory.map { ph ->
                PriceHistoryEntity(
                    id = ph.id,
                    productId = ph.productId,
                    price = BigDecimal(ph.price),
                    currency = ph.currency,
                    recordedAtEpochMillis = ph.recordedAtEpochMillis,
                    note = ph.note
                )
            }
            priceHistoryDao.insertPriceRecords(priceHistoryEntities)

            // Insert Parcels
            val parcelEntities = backup.parcels.map { par ->
                ParcelEntity(
                    id = par.id,
                    goalId = par.goalId,
                    name = par.name,
                    carrier = par.carrier,
                    method = par.method,
                    weightKg = par.weightKg,
                    lengthCm = par.lengthCm,
                    widthCm = par.widthCm,
                    heightCm = par.heightCm,
                    shippingCostGel = BigDecimal(par.shippingCostGel),
                    isConfirmed = par.isConfirmed,
                    trackingNumber = par.trackingNumber,
                    createdAtEpochMillis = par.createdAtEpochMillis
                )
            }
            parcelDao.insertParcels(parcelEntities)
        }
    }

    suspend fun clearAllData() {
        database.withTransaction {
            parcelDao.getAllParcels().forEach { parcelDao.deleteParcel(it.id) }
            priceHistoryDao.getAllPriceHistory().forEach { priceHistoryDao.deletePriceRecord(it.id) }
            goalSnapshotDao.deleteAllSnapshots()
            transactionDao.deleteAllTransactions()
            productDao.deleteAllProducts()
            goalDao.deleteAllGoals()
        }
    }
}
