package com.fundory.app.domain.model

import java.math.BigDecimal
import java.util.UUID

data class PriceHistoryRecord(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val price: BigDecimal,
    val currency: String,
    val recordedAtEpochMillis: Long = System.currentTimeMillis(),
    val note: String? = null
)

