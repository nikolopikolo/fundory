package com.fundory.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundory.app.data.repository.GoalRepository
import com.fundory.app.data.repository.TransactionRepository
import com.fundory.app.data.repository.TransactionResult
import com.fundory.app.domain.model.Goal
import com.fundory.app.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MonthTransactionGroup(
    val monthTitle: String, // e.g. "November 2026"
    val netSavingsGel: BigDecimal,
    val transactions: List<Transaction>
)

data class TransactionsUiState(
    val goals: List<Goal> = emptyList(),
    val selectedGoalId: String? = null,
    val groupedTransactions: List<MonthTransactionGroup> = emptyList(),
    val totalTransactionsCount: Int = 0,
    val totalNetSavingsGel: BigDecimal = BigDecimal.ZERO,
    val operationMessage: String? = null
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _selectedGoalId = MutableStateFlow<String?>(null)
    private val _operationMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.allTransactionsWithGoalsFlow,
        goalRepository.allGoalsWithProductsFlow,
        _selectedGoalId,
        _operationMessage
    ) { transactions, goals, goalId, message ->
        val filtered = if (goalId != null) {
            transactions.filter { it.goalId == goalId }
        } else {
            transactions
        }

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        val cal = Calendar.getInstance()

        val grouped = filtered.groupBy { tx ->
            cal.timeInMillis = tx.timestampEpochMillis
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
        }

        val sortedMonthKeys = grouped.keys.sortedWith(
            compareByDescending<String> { it.split("-")[0].toInt() }
                .thenByDescending { it.split("-")[1].toInt() }
        )

        val groups = mutableListOf<MonthTransactionGroup>()
        var overallNet = BigDecimal.ZERO

        for (key in sortedMonthKeys) {
            val txs = grouped[key] ?: continue
            val title = monthFormat.format(Date(txs.first().timestampEpochMillis))
            var monthNet = BigDecimal.ZERO

            for (tx in txs) {
                monthNet = monthNet.add(tx.signedAmountGel)
                overallNet = overallNet.add(tx.signedAmountGel)
            }

            groups.add(
                MonthTransactionGroup(
                    monthTitle = title,
                    netSavingsGel = monthNet,
                    transactions = txs.sortedByDescending { it.timestampEpochMillis }
                )
            )
        }

        TransactionsUiState(
            goals = goals,
            selectedGoalId = goalId,
            groupedTransactions = groups,
            totalTransactionsCount = filtered.size,
            totalNetSavingsGel = overallNet,
            operationMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun selectGoal(goalId: String?) {
        _selectedGoalId.value = goalId
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            when (val res = transactionRepository.deleteTransaction(transactionId)) {
                is TransactionResult.Success -> {
                    _operationMessage.value = "Transaction deleted and balance updated."
                }
                is TransactionResult.Failure -> {
                    _operationMessage.value = res.reason
                }
            }
        }
    }

    fun clearMessage() {
        _operationMessage.value = null
    }
}
