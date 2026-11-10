package com.fundory.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey
    val currencyCode: String,
    val rateToGel: BigDecimal, // 1 Foreign = rateToGel GEL (e.g. 1 USD = 2.594 GEL)
    val lastUpdatedEpochMillis: Long
)
