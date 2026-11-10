package com.fundory.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundory.app.data.local.dao.GoalDao
import com.fundory.app.data.local.dao.ProductDao
import com.fundory.app.data.local.dao.TransactionDao
import com.fundory.app.data.local.entity.GoalEntity
import com.fundory.app.data.local.entity.ProductEntity
import com.fundory.app.data.local.entity.TransactionEntity
import com.fundory.app.data.preferences.UserPreferencesRepository
import com.fundory.app.data.repository.CurrencyRepository
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.domain.model.Transaction
import com.fundory.app.domain.model.TransactionType
import com.fundory.app.sample.SampleDataProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val goalDao: GoalDao,
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    fun completeOnboardingWithGoal(
        goalName: String,
        targetAmountGel: BigDecimal,
        currentSavedGel: BigDecimal,
        monthlyPlanGel: BigDecimal,
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
                    isTargetCalculatedFromProducts = false,
                    savedAmountGel = currentSavedGel,
                    monthlyPlanGel = monthlyPlanGel,
                    status = GoalStatus.ACTIVE,
                    createdDateEpochMillis = now,
                    lastUpdatedEpochMillis = now
                )
                goalDao.insertGoal(GoalEntity.fromDomain(goal))

                if (currentSavedGel > BigDecimal.ZERO) {
                    val tx = Transaction(
                        id = UUID.randomUUID().toString(),
                        goalId = goalId,
                        type = TransactionType.DEPOSIT,
                        amountGel = currentSavedGel,
                        timestampEpochMillis = now,
                        note = "Initial savings deposit"
                    )
                    transactionDao.insertTransaction(TransactionEntity.fromDomain(tx))
                }
            }

            preferencesRepository.setHasCompletedOnboarding(true)
            onComplete()
        }
    }

    fun completeOnboardingWithSampleData(onComplete: () -> Unit) {
        viewModelScope.launch {
            val rates = currencyRepository.ratesToGelFlow.first()
            val (sampleGoals, sampleTransactions) = SampleDataProvider.generateSampleGoals(rates)

            goalDao.insertGoals(sampleGoals.map { GoalEntity.fromDomain(it) })
            productDao.insertProducts(sampleGoals.flatMap { it.products }.map { ProductEntity.fromDomain(it) })
            transactionDao.insertTransactions(sampleTransactions.map { TransactionEntity.fromDomain(it) })

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
