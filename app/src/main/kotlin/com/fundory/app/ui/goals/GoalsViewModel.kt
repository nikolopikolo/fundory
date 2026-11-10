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
import java.util.UUID
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
        val stateFlow = MutableStateFlow(GoalDetailUiState(isLoading = true))
        viewModelScope.launch {
            combine(
                goalRepository.getGoalByIdFlow(goalId),
                currencyRepository.ratesToGelFlow
            ) { goal, rates ->
                val proj = goal?.let {
                    forecastEngine.calculateProjection(
                        goalId = it.id,
                        targetAmountGel = it.targetAmountGel,
                        currentSavedGel = it.savedAmountGel,
                        monthlyPlanGel = it.monthlyPlanGel,
                        scenario = ProjectionScenario.EXPECTED
                    )
                }
                GoalDetailUiState(
                    goal = goal,
                    ratesToGel = rates,
                    projection = proj,
                    isLoading = false
                )
            }.collect { stateFlow.value = it }
        }
        return stateFlow
    }

    suspend fun getProductById(id: String): Product? {
        return productRepository.getProductById(id)
    }

    fun saveGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.saveGoal(goal)
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

    fun calculateImportCost(
        subtotalGel: BigDecimal,
        config: ImportCalculationConfig,
        ratesToGel: Map<String, BigDecimal>
    ): ImportCalculationResult {
        return importCostCalculator.calculate(subtotalGel, config, ratesToGel)
    }
}
