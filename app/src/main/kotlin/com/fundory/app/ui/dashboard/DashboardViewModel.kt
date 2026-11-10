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
    val totalSavedAcrossAllGoalsGel: BigDecimal = BigDecimal.ZERO,
    val totalCombinedCostGel: BigDecimal = BigDecimal.ZERO,
    val totalRemainingGel: BigDecimal = BigDecimal.ZERO,
    val overallProgressPercentage: Float = 0f,
    val activeGoalsCount: Int = 0,
    val combinedEstimatedCompletionDate: String = "Calculating...",
    val activeGoalProjection: GoalProjectionResult? = null,
    val ratesToGel: Map<String, BigDecimal> = emptyMap(),
    val isRatesStale: Boolean = false,
    val isSyncingRates: Boolean = false,
    val syncMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val currencyRepository: CurrencyRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val forecastEngine: SavingsForecastEngine
) : ViewModel() {

    private val _isSyncingRates = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        goalRepository.activeGoalsWithProductsFlow,
        userPreferencesRepository.userPreferencesFlow,
        currencyRepository.ratesToGelFlow,
        _isSyncingRates,
        _syncMessage
    ) { activeGoals, prefs, rates, isSyncing, syncMsg ->
        val totalSaved = activeGoals.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.savedAmountGel) }
        val totalCost = activeGoals.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.targetAmountGel) }
        val totalRemaining = if (totalCost > totalSaved) totalCost.subtract(totalSaved) else BigDecimal.ZERO

        val overallPercentage = if (totalCost > BigDecimal.ZERO) {
            totalSaved.divide(totalCost, 4, RoundingMode.HALF_UP).toFloat() * 100f
        } else 0f

        val selectedGoal = activeGoals.find { it.id == prefs.selectedActiveGoalId }
            ?: activeGoals.firstOrNull()

        // Combined monthly plan
        val totalMonthlyPlan = activeGoals.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.monthlyPlanGel) }
        val combinedProjection = forecastEngine.calculateProjection(
            goalId = "all_goals_combined",
            targetAmountGel = totalCost,
            currentSavedGel = totalSaved,
            monthlyPlanGel = if (totalMonthlyPlan > BigDecimal.ZERO) totalMonthlyPlan else BigDecimal("100.00"),
            scenario = ProjectionScenario.EXPECTED
        )

        val activeGoalProjection = selectedGoal?.let {
            forecastEngine.calculateProjection(
                goalId = it.id,
                targetAmountGel = it.targetAmountGel,
                currentSavedGel = it.savedAmountGel,
                monthlyPlanGel = it.monthlyPlanGel,
                scenario = ProjectionScenario.EXPECTED
            )
        }

        val now = System.currentTimeMillis()
        val isStale = (now - prefs.lastExchangeRatesSyncMillis) > (24 * 60 * 60 * 1000L)

        DashboardUiState(
            activeGoals = activeGoals,
            selectedActiveGoal = selectedGoal,
            totalSavedAcrossAllGoalsGel = totalSaved,
            totalCombinedCostGel = totalCost,
            totalRemainingGel = totalRemaining,
            overallProgressPercentage = overallPercentage.coerceIn(0f, 100f),
            activeGoalsCount = activeGoals.size,
            combinedEstimatedCompletionDate = combinedProjection.completionDateFormatted,
            activeGoalProjection = activeGoalProjection,
            ratesToGel = rates,
            isRatesStale = isStale && prefs.lastExchangeRatesSyncMillis > 0L,
            isSyncingRates = isSyncing,
            syncMessage = syncMsg
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

    fun selectActiveGoal(goalId: String) {
        viewModelScope.launch {
            userPreferencesRepository.setSelectedActiveGoalId(goalId)
        }
    }

    fun addMoney(goalId: String, amount: BigDecimal, note: String?) {
        viewModelScope.launch {
            transactionRepository.recordDeposit(goalId, amount, note)
        }
    }

    fun withdrawMoney(goalId: String, amount: BigDecimal, note: String?) {
        viewModelScope.launch {
            transactionRepository.recordWithdrawal(goalId, amount, note)
        }
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            _isSyncingRates.value = true
            when (val result = currencyRepository.syncRates(forceRefresh = true)) {
                is CurrencySyncResult.Success -> {
                    _syncMessage.value = "Exchange rates updated successfully"
                }
                is CurrencySyncResult.Failure -> {
                    _syncMessage.value = result.message
                }
            }
            _isSyncingRates.value = false
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
