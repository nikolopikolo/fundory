package com.fundory.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundory.app.data.local.dao.GoalDao
import com.fundory.app.data.local.dao.TransactionDao
import com.fundory.app.data.local.entity.GoalEntity
import com.fundory.app.data.local.entity.TransactionEntity
import com.fundory.app.data.preferences.UserPreferencesRepository
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.domain.model.Transaction
import com.fundory.app.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val goalDao: GoalDao,
    private val transactionDao: TransactionDao
) : ViewModel() {

    fun completeOnboardingWithGoal(
        goalName: String,
        targetAmountGel: BigDecimal,
        initialDepositGel: BigDecimal,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            if (goalName.isNotBlank() && targetAmountGel > BigDecimal.ZERO) {
                val goalId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()

                val goal = Goal(
                    id = goalId,
                    name = goalName.trim(),
                    targetAmountGel = targetAmountGel,
                    originalTargetAmount = targetAmountGel,
                    targetCurrency = "GEL",
                    isTargetCalculatedFromProducts = false,
                    savedAmountGel = initialDepositGel,
                    monthlyPlanGel = BigDecimal.ZERO,
                    status = GoalStatus.ACTIVE,
                    createdDateEpochMillis = now,
                    lastUpdatedEpochMillis = now
                )
                goalDao.insertGoal(GoalEntity.fromDomain(goal))

                if (initialDepositGel > BigDecimal.ZERO) {
                    val tx = Transaction(
                        id = UUID.randomUUID().toString(),
                        goalId = goalId,
                        type = TransactionType.DEPOSIT,
                        amountGel = initialDepositGel,
                        timestampEpochMillis = now,
                        note = "Initial goal savings"
                    )
                    transactionDao.insertTransaction(TransactionEntity.fromDomain(tx))
                }
            }

            preferencesRepository.setHasCompletedOnboarding(true)
            onComplete()
        }
    }

    fun completeOnboardingWithGeneralSavings(
        initialDepositGel: BigDecimal,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            if (initialDepositGel > BigDecimal.ZERO) {
                val now = System.currentTimeMillis()
                val tx = Transaction(
                    id = UUID.randomUUID().toString(),
                    goalId = null, // General unallocated savings
                    type = TransactionType.DEPOSIT,
                    amountGel = initialDepositGel,
                    timestampEpochMillis = now,
                    note = "Initial general savings"
                )
                transactionDao.insertTransaction(TransactionEntity.fromDomain(tx))
            }

            preferencesRepository.setHasCompletedOnboarding(true)
            onComplete()
        }
    }

    fun skipOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setHasCompletedOnboarding(true)
            onComplete()
        }
    }
}
