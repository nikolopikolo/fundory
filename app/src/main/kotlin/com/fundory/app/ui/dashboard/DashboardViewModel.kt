package com.fundory.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundory.app.data.preferences.UserPreferencesRepository
import com.fundory.app.data.repository.CurrencyRepository
import com.fundory.app.data.repository.CurrencySyncResult
import com.fundory.app.data.repository.GoalRepository
import com.fundory.app.data.repository.TransactionRepository
import com.fundory.app.domain.engine.SavingsForecastEngine
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalProjectionResult
import com.fundory.app.domain.model.ProjectionScenario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class DashboardUiState(
    val activeGoals: List<Goal> = emptyList(),
    val selectedActiveGoal: Goal? = null,
    val generalSavingsBalanceGel: BigDecimal = BigDecimal.ZERO,
    val totalSavedAcrossAllGoalsGel: BigDecimal = BigDecimal.ZERO,
    val totalCombinedCostGel: BigDecimal = BigDecimal.ZERO,
    val totalRemainingGel: BigDecimal = BigDecimal.ZERO,
    val overallProgressPercentage: Float = 0f,
    val activeGoalsCount: Int = 0,
    val combinedEstimatedCompletionDate: String = "Not enough information to estimate",
    val activeGoalProjection: GoalProjectionResult? = null,
    val ratesToGel: Map<String, BigDecimal> = emptyMap(),
    val isRatesStale: Boolean = false,
    val isSyncingRates: Boolean = false,
    val syncMessage: String? = null
)

private data class DashboardSyncState(
    val isSyncing: Boolean,
    val message: String?
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val currencyRepository: CurrencyRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val forecastEngine: SavingsForecastEngine
) : ViewModel() {

    private val _syncState = MutableStateFlow(DashboardSyncState(isSyncing = false, message = null))

    val uiState: StateFlow<DashboardUiState> = combine(
        goalRepository.activeGoalsWithProductsFlow,
        transactionRepository.generalSavingsBalanceFlow,
        userPreferencesRepository.userPreferencesFlow,
        currencyRepository.ratesToGelFlow,
        _syncState
    ) { activeGoals, generalSavings, prefs, rates, syncState ->
        val goalsSaved = activeGoals.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.savedAmountGel) }
        val totalSaved = generalSavings.add(goalsSaved)
        val totalCost = activeGoals.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.totalRequiredFundingGel) }
        val totalRemaining = if (totalCost > totalSaved) totalCost.subtract(totalSaved) else BigDecimal.ZERO

        val overallPercentage = if (totalCost > BigDecimal.ZERO) {
            totalSaved.divide(totalCost, 4, RoundingMode.HALF_UP).toFloat() * 100f
        } else 0f

        val selectedGoal = activeGoals.find { it.id == prefs.selectedActiveGoalId }
            ?: activeGoals.firstOrNull()

        // Combined monthly plan calculation (only if user explicitly configured monthly contributions)
        val totalMonthlyPlan = activeGoals.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.monthlyPlanGel) }
        val combinedEstimatedDate = if (totalMonthlyPlan > BigDecimal.ZERO) {
            val combinedProjection = forecastEngine.calculateProjection(
                goalId = "all_goals_combined",
                targetAmountGel = totalCost,
                currentSavedGel = totalSaved,
                monthlyPlanGel = totalMonthlyPlan,
                scenario = ProjectionScenario.EXPECTED
            )
            combinedProjection.completionDateFormatted
        } else {
            "Not enough information to estimate"
        }

        val activeGoalProjection = selectedGoal?.let {
            if (it.monthlyPlanGel > BigDecimal.ZERO) {
                forecastEngine.calculateProjection(
                    goalId = it.id,
                    targetAmountGel = it.targetAmountGel,
                    currentSavedGel = it.savedAmountGel,
                    monthlyPlanGel = it.monthlyPlanGel,
                    scenario = ProjectionScenario.EXPECTED
                )
            } else null
        }

        val now = System.currentTimeMillis()
        val isStale = (now - prefs.lastExchangeRatesSyncMillis) > (24 * 60 * 60 * 1000L)

        DashboardUiState(
            activeGoals = activeGoals,
            selectedActiveGoal = selectedGoal,
            generalSavingsBalanceGel = generalSavings,
            totalSavedAcrossAllGoalsGel = totalSaved,
            totalCombinedCostGel = totalCost,
            totalRemainingGel = totalRemaining,
            overallProgressPercentage = overallPercentage.coerceIn(0f, 100f),
            activeGoalsCount = activeGoals.size,
            combinedEstimatedCompletionDate = combinedEstimatedDate,
            activeGoalProjection = activeGoalProjection,
            ratesToGel = rates,
            isRatesStale = isStale && prefs.lastExchangeRatesSyncMillis > 0L,
            isSyncingRates = syncState.isSyncing,
            syncMessage = syncState.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        // Auto-refresh stale rates on launch
        viewModelScope.launch {
            currencyRepository.syncRates(forceRefresh = false)
        }
    }

    val preferencesFlow = userPreferencesRepository.userPreferencesFlow

    fun transferFunds(fromGoalId: String?, toGoalId: String?, amount: BigDecimal, note: String?) {
        viewModelScope.launch {
            transactionRepository.transferFunds(fromGoalId, toGoalId, amount, note)
        }
    }

    fun selectActiveGoal(goalId: String) {
        viewModelScope.launch {
            userPreferencesRepository.setSelectedActiveGoalId(goalId)
        }
    }

    fun addMoney(goalId: String?, amount: BigDecimal, note: String?) {
        viewModelScope.launch {
            transactionRepository.recordDeposit(goalId, amount, note)
        }
    }

    fun withdrawMoney(goalId: String?, amount: BigDecimal, note: String?) {
        viewModelScope.launch {
            transactionRepository.recordWithdrawal(goalId, amount, note)
        }
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            _syncState.value = DashboardSyncState(isSyncing = true, message = null)
            val msg = when (val result = currencyRepository.syncRates(forceRefresh = true)) {
                is CurrencySyncResult.Success -> "Exchange rates updated successfully"
                is CurrencySyncResult.Failure -> result.message
            }
            _syncState.value = DashboardSyncState(isSyncing = false, message = msg)
        }
    }

    fun clearSyncMessage() {
        _syncState.value = _syncState.value.copy(message = null)
    }
}
