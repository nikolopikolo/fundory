package com.fundory.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

data class Product(
    val id: String,
    val goalId: String? = null,
    val name: String,
    val description: String = "",
    val originalPrice: BigDecimal,
    val currency: String, // e.g. "USD", "CNY", "GEL"
    val quantity: Int = 1,
    val productUrl: String? = null,
    val imageUrl: String? = null,
    val shippingCost: BigDecimal = BigDecimal.ZERO,
    val shippingCurrency: String = currency,
    val taxesAndFees: BigDecimal = BigDecimal.ZERO,
    val taxesCurrency: String = currency,
    val manualOverrideGel: BigDecimal? = null,
    val sortOrder: Int = 0,
    val dateAddedEpochMillis: Long = System.currentTimeMillis(),
    val lastEditedEpochMillis: Long = System.currentTimeMillis(),
    val purchaseStatus: PurchaseStatus = PurchaseStatus.NOT_PURCHASED,
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
    val isManuallyOverridden: Boolean
        get() = manualOverrideGel != null

    /**
     * Calculates converted GEL price given a map of currency -> rateToGel
     * e.g. 1 USD = 2.594 GEL
     */
    fun calculateTotalGel(ratesToGel: Map<String, BigDecimal>): BigDecimal {
        if (manualOverrideGel != null) {
            return manualOverrideGel.setScale(2, RoundingMode.HALF_UP)
        }

        val itemRate = ratesToGel[currency.uppercase()] ?: BigDecimal.ONE
        val shippingRate = ratesToGel[shippingCurrency.uppercase()] ?: itemRate
        val taxesRate = ratesToGel[taxesCurrency.uppercase()] ?: itemRate

        val itemsGel = originalPrice.multiply(BigDecimal(quantity)).multiply(itemRate)
        val shippingGel = shippingCost.multiply(shippingRate)
        val taxesGel = taxesAndFees.multiply(taxesRate)

        return itemsGel.add(shippingGel).add(taxesGel).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateItemOnlyGel(ratesToGel: Map<String, BigDecimal>): BigDecimal {
        if (manualOverrideGel != null) {
            return manualOverrideGel.setScale(2, RoundingMode.HALF_UP)
        }
        val rate = ratesToGel[currency.uppercase()] ?: BigDecimal.ONE
        return originalPrice.multiply(BigDecimal(quantity)).multiply(rate).setScale(2, RoundingMode.HALF_UP)
    }
}

