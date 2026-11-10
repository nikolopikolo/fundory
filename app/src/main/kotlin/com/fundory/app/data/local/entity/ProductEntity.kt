package com.fundory.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fundory.app.domain.model.Product
import com.fundory.app.domain.model.PurchaseStatus
import java.math.BigDecimal

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["alternativeGroupId"]),
        Index(value = ["parcelId"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val goalId: String?,
    val name: String,
    val description: String,
    val originalPrice: BigDecimal,
    val currency: String,
    val quantity: Int,
    val productUrl: String?,
    val imageUrl: String?,
    val shippingCost: BigDecimal,
    val shippingCurrency: String,
    val taxesAndFees: BigDecimal,
    val taxesCurrency: String,
    val manualOverrideGel: BigDecimal?,
    val sortOrder: Int,
    val dateAddedEpochMillis: Long,
    val lastEditedEpochMillis: Long,
    val purchaseStatus: String = PurchaseStatus.NOT_PURCHASED.name,
    val actualPrice: BigDecimal? = null,
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
) {
    fun toDomain(): Product = Product(
        id = id,
        goalId = goalId,
        name = name,
        description = description,
        originalPrice = originalPrice,
        currency = currency,
        quantity = quantity,
        productUrl = productUrl,
        imageUrl = imageUrl,
        shippingCost = shippingCost,
        shippingCurrency = shippingCurrency,
        taxesAndFees = taxesAndFees,
        taxesCurrency = taxesCurrency,
        manualOverrideGel = manualOverrideGel,
        sortOrder = sortOrder,
        dateAddedEpochMillis = dateAddedEpochMillis,
        lastEditedEpochMillis = lastEditedEpochMillis,
        purchaseStatus = try {
            PurchaseStatus.valueOf(purchaseStatus)
        } catch (_: Exception) {
            PurchaseStatus.NOT_PURCHASED
        },
        actualPrice = actualPrice,
        actualPriceCurrency = actualPriceCurrency,
        shippingCarrier = shippingCarrier,
        trackingNumber = trackingNumber,
        expectedDeliveryDate = expectedDeliveryDate,
        actualDeliveryDate = actualDeliveryDate,
        trackingNotes = trackingNotes,
        alternativeGroupId = alternativeGroupId,
        isSelectedAlternative = isSelectedAlternative,
        parcelId = parcelId,
        marketplace = marketplace
    )

    companion object {
        fun fromDomain(product: Product): ProductEntity = ProductEntity(
            id = product.id,
            goalId = product.goalId,
            name = product.name,
            description = product.description,
            originalPrice = product.originalPrice,
            currency = product.currency,
            quantity = product.quantity,
            productUrl = product.productUrl,
            imageUrl = product.imageUrl,
            shippingCost = product.shippingCost,
            shippingCurrency = product.shippingCurrency,
            taxesAndFees = product.taxesAndFees,
            taxesCurrency = product.taxesCurrency,
            manualOverrideGel = product.manualOverrideGel,
            sortOrder = product.sortOrder,
            dateAddedEpochMillis = product.dateAddedEpochMillis,
            lastEditedEpochMillis = product.lastEditedEpochMillis,
            purchaseStatus = product.purchaseStatus.name,
            actualPrice = product.actualPrice,
            actualPriceCurrency = product.actualPriceCurrency,
            shippingCarrier = product.shippingCarrier,
            trackingNumber = product.trackingNumber,
            expectedDeliveryDate = product.expectedDeliveryDate,
            actualDeliveryDate = product.actualDeliveryDate,
            trackingNotes = product.trackingNotes,
            alternativeGroupId = product.alternativeGroupId,
            isSelectedAlternative = product.isSelectedAlternative,
            parcelId = product.parcelId,
            marketplace = product.marketplace
        )
    }
}
