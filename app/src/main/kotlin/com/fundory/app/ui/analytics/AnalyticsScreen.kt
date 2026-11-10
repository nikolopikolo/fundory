package com.fundory.app.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundory.app.ui.components.charts.GoalProgressHistoryChart
import com.fundory.app.ui.components.charts.MonthlySavingsBarChart
import com.fundory.app.ui.components.charts.SavingsGrowthLineChart
import com.fundory.app.ui.components.charts.SavingsProjectionChart
import com.fundory.app.ui.components.charts.SpendingBreakdownDonutChart
import com.fundory.app.ui.theme.FundoryGreen
import com.fundory.app.ui.theme.FundoryGreenLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Savings Analytics & Trends",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Goal Filter Header (All Goals Combined or Specific Goal)
            item {
                Column {
                    Text(
                        text = "Scope",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            val isAllSelected = state.selectedGoalId == null
                            FilterChip(
                                selected = isAllSelected,
                                onClick = { viewModel.selectGoal(null) },
                                label = { Text("All Goals Combined") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FundoryGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = FundoryGreenLight
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isAllSelected,
                                    borderColor = if (isAllSelected) FundoryGreen else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        items(state.activeGoals) { goal ->
                            val isSelected = state.selectedGoalId == goal.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectGoal(goal.id) },
                                label = { Text(goal.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FundoryGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = FundoryGreenLight
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) FundoryGreen else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }
                }
            }

            // GRAPH A: Savings Growth (Interactive Line Chart)
            item {
                SavingsGrowthLineChart(
                    points = state.growthHistoryPoints,
                    selectedTimeRange = state.selectedTimeRange,
                    onTimeRangeSelected = { viewModel.selectTimeRange(it) }
                )
            }

            // GRAPH B: Monthly Savings (Bar Chart, Net vs Deposits)
            item {
                MonthlySavingsBarChart(
                    data = state.monthlySavingsData
                )
            }

            // GRAPH C: Savings Projection (Solid historical + dashed future line, target line, scenarios)
            state.projectionResult?.let { proj ->
                item {
                    SavingsProjectionChart(
                        projection = proj,
                        historicalPoints = state.growthHistoryPoints,
                        selectedScenario = state.selectedProjectionScenario,
                        onScenarioSelected = { viewModel.selectProjectionScenario(it) }
                    )
                }
            }

            // GRAPH D: Spending Breakdown (Donut Chart)
            if (state.productsBreakdown.isNotEmpty()) {
                item {
                    SpendingBreakdownDonutChart(
                        products = state.productsBreakdown,
                        ratesToGel = state.ratesToGel
                    )
                }
            }

            // Bottom space
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
