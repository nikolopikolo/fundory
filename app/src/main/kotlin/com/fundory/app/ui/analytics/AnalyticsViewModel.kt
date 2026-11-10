package com.fundory.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundory.app.data.local.dao.GoalSnapshotDao
import com.fundory.app.data.local.entity.GoalSnapshotEntity
import com.fundory.app.data.repository.CurrencyRepository
import com.fundory.app.data.repository.GoalRepository
import com.fundory.app.data.repository.TransactionRepository
import com.fundory.app.domain.engine.SavingsForecastEngine
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.GoalProjectionResult
import com.fundory.app.domain.model.Product
import com.fundory.app.domain.model.ProjectionScenario
import com.fundory.app.domain.model.Transaction
import com.fundory.app.domain.model.TransactionType
import com.fundory.app.ui.components.charts.BalanceHistoryPoint
import com.fundory.app.ui.components.charts.MonthSavingsData
import com.fundory.app.ui.components.charts.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AnalyticsUiState(
    val activeGoals: List<Goal> = emptyList(),
    val selectedGoalId: String? = null, // null = all goals combined
    val selectedTimeRange: TimeRange = TimeRange.ALL_TIME,
    val selectedProjectionScenario: ProjectionScenario = ProjectionScenario.EXPECTED,
    val growthHistoryPoints: List<BalanceHistoryPoint> = emptyList(),
    val monthlySavingsData: List<MonthSavingsData> = emptyList(),
    val projectionResult: GoalProjectionResult? = null,
    val productsBreakdown: List<Product> = emptyList(),
    val ratesToGel: Map<String, BigDecimal> = emptyMap(),
    val snapshots: List<GoalSnapshotEntity> = emptyList()
)

data class FilterParams(
    val goalId: String?,
    val timeRange: TimeRange,
    val scenario: ProjectionScenario
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val currencyRepository: CurrencyRepository,
    private val goalSnapshotDao: GoalSnapshotDao,
    private val forecastEngine: SavingsForecastEngine
) : ViewModel() {

    private val _selectedGoalId = MutableStateFlow<String?>(null)
    private val _selectedTimeRange = MutableStateFlow(TimeRange.ALL_TIME)
    private val _selectedProjectionScenario = MutableStateFlow(ProjectionScenario.EXPECTED)

    private val _filterParams = combine(
        _selectedGoalId,
        _selectedTimeRange,
        _selectedProjectionScenario
    ) { goalId, timeRange, scenario ->
        FilterParams(goalId, timeRange, scenario)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        goalRepository.activeGoalsWithProductsFlow,
        transactionRepository.allTransactionsWithGoalsFlow,
        currencyRepository.ratesToGelFlow,
        _filterParams
    ) { goals, transactions, rates, filters ->
        val goalId = filters.goalId
        val timeRange = filters.timeRange
        val scenario = filters.scenario

        // 1. Filter transactions by goal if selected
        val relevantTransactions = if (goalId != null) {
            transactions.filter { it.goalId == goalId }
        } else {
            transactions
        }

        // 2. Compute Growth History Points (Balance over time)
        val growthPoints = buildGrowthHistory(relevantTransactions, timeRange)

        // 3. Compute Monthly Savings (Deposits, Withdrawals, Net)
        val monthlyData = buildMonthlySavings(relevantTransactions)

        // 4. Compute Projection
        val projection = buildProjection(goals, goalId, scenario)

        // 5. Products for Spending Breakdown
        val products = if (goalId != null) {
            goals.find { it.id == goalId }?.products ?: emptyList()
        } else {
            goals.flatMap { it.products }
        }

        AnalyticsUiState(
            activeGoals = goals,
            selectedGoalId = goalId,
            selectedTimeRange = timeRange,
            selectedProjectionScenario = scenario,
            growthHistoryPoints = growthPoints,
            monthlySavingsData = monthlyData,
            projectionResult = projection,
            productsBreakdown = products,
            ratesToGel = rates
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun selectGoal(goalId: String?) {
        _selectedGoalId.value = goalId
    }

    fun selectTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
    }

    fun selectProjectionScenario(scenario: ProjectionScenario) {
        _selectedProjectionScenario.value = scenario
    }

    private fun buildGrowthHistory(
        transactions: List<Transaction>,
        range: TimeRange
    ): List<BalanceHistoryPoint> {
        if (transactions.isEmpty()) return emptyList()

        val sorted = transactions.sortedBy { it.timestampEpochMillis }
        val now = System.currentTimeMillis()
        val cutoff = if (range.days > 0) now - (range.days.toLong() * 86400000L) else 0L

        val filtered = sorted.filter { it.timestampEpochMillis >= cutoff }

        var runningBalance = BigDecimal.ZERO
        // If range cut off some earlier transactions, compute starting balance
        val priorTransactions = sorted.filter { it.timestampEpochMillis < cutoff }
        for (tx in priorTransactions) {
            runningBalance = runningBalance.add(tx.signedAmountGel)
        }

        val points = mutableListOf<BalanceHistoryPoint>()
        if (cutoff > 0 && priorTransactions.isNotEmpty()) {
            points.add(BalanceHistoryPoint(cutoff, runningBalance.coerceAtLeast(BigDecimal.ZERO)))
        }

        for (tx in filtered) {
            runningBalance = runningBalance.add(tx.signedAmountGel)
            points.add(BalanceHistoryPoint(tx.timestampEpochMillis, runningBalance.coerceAtLeast(BigDecimal.ZERO)))
        }

        // Add current moment point
        if (points.isNotEmpty()) {
            points.add(BalanceHistoryPoint(now, runningBalance.coerceAtLeast(BigDecimal.ZERO)))
        }

        return points
    }

    private fun buildMonthlySavings(transactions: List<Transaction>): List<MonthSavingsData> {
        if (transactions.isEmpty()) return emptyList()

        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.US)
        val cal = Calendar.getInstance()

        // Group transactions by Year-Month
        val grouped = transactions.groupBy { tx ->
            cal.timeInMillis = tx.timestampEpochMillis
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
        }

        // Sort chronologically
        val sortedKeys = grouped.keys.sortedWith(compareBy({ it.split("-")[0].toInt() }, { it.split("-")[1].toInt() }))

        val result = mutableListOf<MonthSavingsData>()
        for (key in sortedKeys) {
            val txs = grouped[key] ?: continue
            val firstTx = txs.first()
            val label = monthFormat.format(Date(firstTx.timestampEpochMillis))

            var deposits = BigDecimal.ZERO
            var withdrawals = BigDecimal.ZERO

            for (tx in txs) {
                when (tx.type) {
                    TransactionType.DEPOSIT -> deposits = deposits.add(tx.amountGel)
                    TransactionType.WITHDRAWAL -> withdrawals = withdrawals.add(tx.amountGel)
                    TransactionType.ADJUSTMENT -> {
                        if (tx.amountGel >= BigDecimal.ZERO) deposits = deposits.add(tx.amountGel)
                        else withdrawals = withdrawals.add(tx.amountGel.abs())
                    }
                    TransactionType.TRANSFER -> withdrawals = withdrawals.add(tx.amountGel)
                }
            }

            val net = deposits.subtract(withdrawals)
            result.add(
                MonthSavingsData(
                    monthLabel = label,
                    depositsGel = deposits,
                    withdrawalsGel = withdrawals,
                    netSavingsGel = net
                )
            )
        }
        return result
    }

    private fun buildProjection(
        goals: List<Goal>,
        selectedGoalId: String?,
        scenario: ProjectionScenario
    ): GoalProjectionResult {
        return if (selectedGoalId != null) {
            val goal = goals.find { it.id == selectedGoalId }
            if (goal != null) {
                forecastEngine.calculateProjection(
                    goalId = goal.id,
                    targetAmountGel = goal.targetAmountGel,
                    currentSavedGel = goal.savedAmountGel,
                    monthlyPlanGel = goal.monthlyPlanGel,
                    scenario = scenario
                )
            } else {
                emptyProjection(scenario)
            }
        } else {
            // All goals combined
            val totalTarget = goals.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.targetAmountGel) }
            val totalSaved = goals.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.savedAmountGel) }
            val totalMonthly = goals.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.monthlyPlanGel) }

            forecastEngine.calculateProjection(
                goalId = "all_goals_combined",
                targetAmountGel = totalTarget,
                currentSavedGel = totalSaved,
                monthlyPlanGel = totalMonthly,
                scenario = scenario
            )
        }
    }

    private fun emptyProjection(scenario: ProjectionScenario) = GoalProjectionResult(
        goalId = "empty",
        scenario = scenario,
        monthlyContributionGel = BigDecimal.ZERO,
        targetAmountGel = BigDecimal.ZERO,
        currentSavedGel = BigDecimal.ZERO,
        remainingAmountGel = BigDecimal.ZERO,
        remainingMonths = 0,
        estimatedCompletionDateEpochMillis = null,
        completionDateFormatted = "N/A",
        timeline = emptyList(),
        explanationText = "No data"
    )
}
