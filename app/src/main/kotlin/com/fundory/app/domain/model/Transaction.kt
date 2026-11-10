package com.fundory.app.domain.model

import java.math.BigDecimal

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    ADJUSTMENT,
    TRANSFER
}

data class Transaction(
    val id: String,
    val goalId: String? = null,
    val type: TransactionType,
    val amountGel: BigDecimal,
    val targetGoalId: String? = null,
    val timestampEpochMillis: Long = System.currentTimeMillis(),
    val note: String? = null,
    val goalName: String? = null,
    val targetGoalName: String? = null
) {
    val isGeneralSavings: Boolean
        get() = goalId == null

    val isPositive: Boolean
        get() = type == TransactionType.DEPOSIT ||
                (type == TransactionType.ADJUSTMENT && amountGel > BigDecimal.ZERO)

    val signedAmountGel: BigDecimal
        get() = when (type) {
            TransactionType.DEPOSIT -> amountGel
            TransactionType.WITHDRAWAL -> amountGel.negate()
            TransactionType.ADJUSTMENT -> amountGel
            TransactionType.TRANSFER -> amountGel.negate()
        }
}
