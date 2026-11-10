package com.fundory.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fundory.app.domain.model.PriceHistoryRecord
import java.math.BigDecimal

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["recordedAtEpochMillis"])
    ]
)
data class PriceHistoryEntity(
    @PrimaryKey
    val id: String,
    val productId: String,
    val price: BigDecimal,
    val currency: String,
    val recordedAtEpochMillis: Long,
    val note: String? = null
) {
    fun toDomain(): PriceHistoryRecord = PriceHistoryRecord(
        id = id,
        productId = productId,
        price = price,
        currency = currency,
        recordedAtEpochMillis = recordedAtEpochMillis,
        note = note
    )

    companion object {
        fun fromDomain(record: PriceHistoryRecord): PriceHistoryEntity = PriceHistoryEntity(
            id = record.id,
            productId = record.productId,
            price = record.price,
            currency = record.currency,
            recordedAtEpochMillis = record.recordedAtEpochMillis,
            note = record.note
        )
    }
}

