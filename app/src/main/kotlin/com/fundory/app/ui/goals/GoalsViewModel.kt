package com.fundory.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundory.app.data.repository.CurrencyRepository
import com.fundory.app.data.repository.GoalRepository
import com.fundory.app.data.repository.ProductRepository
import com.fundory.app.data.repository.TransactionRepository
import com.fundory.app.domain.engine.ImportCostCalculator
import com.fundory.app.domain.engine.SavingsForecastEngine
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalProjectionResult
import com.fundory.app.domain.model.GoalStatus
import com.fundory.app.domain.model.ImportCalculationConfig
import com.fundory.app.domain.model.ImportCalculationResult
import com.fundory.app.domain.model.Product
import com.fundory.app.domain.model.ProjectionScenario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    val filterStatus: GoalStatus = GoalStatus.ACTIVE,
    val ratesToGel: Map<String, BigDecimal> = emptyMap(),
    val isDeletingGoal: Boolean = false
)

data class GoalDetailUiState(
    val goal: Goal? = null,
    val ratesToGel: Map<String, BigDecimal> = emptyMap(),
    val projection: GoalProjectionResult? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val currencyRepository: CurrencyRepository,
    private val forecastEngine: SavingsForecastEngine,
    private val importCostCalculator: ImportCostCalculator
) : ViewModel() {

    private val _filterStatus = MutableStateFlow(GoalStatus.ACTIVE)
    private val goalDetailFlowCache = ConcurrentHashMap<String, StateFlow<GoalDetailUiState>>()

    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.allGoalsWithProductsFlow,
        _filterStatus,
        currencyRepository.ratesToGelFlow
    ) { goals, filter, rates ->
        val filtered = goals.filter { it.status == filter }
        GoalsUiState(
            goals = filtered,
            filterStatus = filter,
            ratesToGel = rates
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsUiState()
    )

    fun setFilterStatus(status: GoalStatus) {
        _filterStatus.value = status
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            goalDetailFlowCache.remove(goalId)
            goalRepository.deleteGoal(goalId)
        }
    }

    fun archiveGoal(goalId: String) {
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goalId, GoalStatus.ARCHIVED)
        }
    }

    fun restoreGoal(goalId: String) {
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goalId, GoalStatus.ACTIVE)
        }
    }

    fun markGoalCompleted(goalId: String) {
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goalId, GoalStatus.COMPLETED)
        }
    }

    fun duplicateGoal(goalId: String, onDuplicated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val newId = goalRepository.duplicateGoal(goalId)
            if (newId != null) {
                onDuplicated(newId)
            }
        }
    }

    fun getGoalDetailFlow(goalId: String): StateFlow<GoalDetailUiState> {
        return goalDetailFlowCache.getOrPut(goalId) {
            combine(
                goalRepository.getGoalByIdFlow(goalId),
                currencyRepository.ratesToGelFlow
            ) { goal, rates ->
                val proj = goal?.let {
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
                GoalDetailUiState(
                    goal = goal,
                    ratesToGel = rates,
                    projection = proj,
                    isLoading = false
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = GoalDetailUiState(isLoading = true)
            )
        }
    }

    fun getTransactionsForGoal(goalId: String): kotlinx.coroutines.flow.Flow<List<com.fundory.app.domain.model.Transaction>> {
        return transactionRepository.getTransactionsByGoalFlow(goalId)
    }

    suspend fun getGoalById(id: String): Goal? {
        return goalRepository.getGoalById(id)
    }

    suspend fun getProductById(id: String): Product? {
        return productRepository.getProductById(id)
    }

    fun saveGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.saveGoal(goal)
        }
    }

    fun createGoalWithInitialDeposit(goal: Goal, initialDepositGel: BigDecimal) {
        viewModelScope.launch {
            goalRepository.saveGoal(goal)
            if (initialDepositGel > BigDecimal.ZERO) {
                transactionRepository.recordDeposit(
                    goalId = goal.id,
                    amountGel = initialDepositGel,
                    note = "Initial goal balance"
                )
            }
        }
    }

    fun recordDeposit(goalId: String?, amount: BigDecimal, note: String?) {
        viewModelScope.launch {
            transactionRepository.recordDeposit(goalId, amount, note)
        }
    }

    fun recordWithdrawal(goalId: String?, amount: BigDecimal, note: String?) {
        viewModelScope.launch {
            transactionRepository.recordWithdrawal(goalId, amount, note)
        }
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            productRepository.saveProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
        }
    }

    fun reorderProducts(goalId: String, orderedIds: List<String>) {
        viewModelScope.launch {
            productRepository.updateProductOrder(goalId, orderedIds)
        }
    }

    val generalSavingsBalanceFlow = transactionRepository.generalSavingsBalanceFlow

    fun transferFunds(fromGoalId: String?, toGoalId: String?, amount: BigDecimal, note: String?) {
        viewModelScope.launch {
            transactionRepository.transferFunds(fromGoalId, toGoalId, amount, note)
        }
    }

    fun selectAlternative(groupId: String, selectedProductId: String) {
        viewModelScope.launch {
            productRepository.selectAlternative(groupId, selectedProductId)
        }
    }

    fun calculateImportCost(
        subtotalGel: BigDecimal,
        config: ImportCalculationConfig,
        ratesToGel: Map<String, BigDecimal>
    ): ImportCalculationResult {
        return importCostCalculator.calculate(subtotalGel, config, ratesToGel)
    }
}
