package com.fundory.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fundory.app.domain.model.Parcel
import java.math.BigDecimal

@Entity(
    tableName = "parcels",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["goalId"])]
)
data class ParcelEntity(
    @PrimaryKey
    val id: String,
    val goalId: String?,
    val name: String,
    val carrier: String,
    val method: String,
    val weightKg: Double,
    val lengthCm: Double?,
    val widthCm: Double?,
    val heightCm: Double?,
    val shippingCostGel: BigDecimal,
    val isConfirmed: Boolean,
    val trackingNumber: String?,
    val createdAtEpochMillis: Long
) {
    fun toDomain(): Parcel = Parcel(
        id = id,
        goalId = goalId,
        name = name,
        carrier = carrier,
        method = method,
        weightKg = weightKg,
        lengthCm = lengthCm,
        widthCm = widthCm,
        heightCm = heightCm,
        shippingCostGel = shippingCostGel,
        isConfirmed = isConfirmed,
        trackingNumber = trackingNumber,
        createdAtEpochMillis = createdAtEpochMillis
    )

    companion object {
        fun fromDomain(parcel: Parcel): ParcelEntity = ParcelEntity(
            id = parcel.id,
            goalId = parcel.goalId,
            name = parcel.name,
            carrier = parcel.carrier,
            method = parcel.method,
            weightKg = parcel.weightKg,
            lengthCm = parcel.lengthCm,
            widthCm = parcel.widthCm,
            heightCm = parcel.heightCm,
            shippingCostGel = parcel.shippingCostGel,
            isConfirmed = parcel.isConfirmed,
            trackingNumber = parcel.trackingNumber,
            createdAtEpochMillis = parcel.createdAtEpochMillis
        )
    }
}

