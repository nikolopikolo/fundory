package com.fundory.app.data.local

import androidx.room.TypeConverter
import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.domain.model.TransactionType
import java.math.BigDecimal

class Converters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }

    @TypeConverter
    fun fromGoalStatus(status: GoalStatus): String = status.name

    @TypeConverter
    fun toGoalStatus(value: String): GoalStatus = try {
        GoalStatus.valueOf(value)
    } catch (_: Exception) {
        GoalStatus.ACTIVE
    }

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (_: Exception) {
        TransactionType.DEPOSIT
    }
}
