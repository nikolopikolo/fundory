package com.fundory.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundory.app.data.backup.BackupManager
import com.fundory.app.data.backup.BackupValidationResult
import com.fundory.app.data.backup.FundoryBackup
import com.fundory.app.data.local.dao.GoalDao
import com.fundory.app.data.local.dao.ProductDao
import com.fundory.app.data.local.dao.TransactionDao
import com.fundory.app.data.local.entity.GoalEntity
import com.fundory.app.data.local.entity.ProductEntity
import com.fundory.app.data.local.entity.TransactionEntity
import com.fundory.app.data.preferences.UserPreferences
import com.fundory.app.data.preferences.UserPreferencesRepository
import com.fundory.app.data.repository.CurrencyRepository
import com.fundory.app.data.repository.CurrencySyncResult
import com.fundory.app.sample.SampleDataProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val lastSyncFormatted: String = "Never",
    val isSyncing: Boolean = false,
    val feedbackMessage: String? = null,
    val backupPreview: BackupValidationResult? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val currencyRepository: CurrencyRepository,
    private val backupManager: BackupManager,
    private val goalDao: GoalDao,
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _backupPreview = MutableStateFlow<BackupValidationResult?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.userPreferencesFlow,
        _isSyncing,
        _feedbackMessage,
        _backupPreview
    ) { prefs, isSyncing, msg, preview ->
        val dateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
        val lastSyncStr = if (prefs.lastExchangeRatesSyncMillis > 0) {
            dateFormat.format(Date(prefs.lastExchangeRatesSyncMillis))
        } else {
            "Not synced yet"
        }

        SettingsUiState(
            preferences = prefs,
            lastSyncFormatted = lastSyncStr,
            isSyncing = isSyncing,
            feedbackMessage = msg,
            backupPreview = preview
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateCarrierRates(boxetteAir: Double, boxetteGround: Double, inexAir: Double, inexSea: Double) {
        viewModelScope.launch {
            preferencesRepository.setCarrierRates(boxetteAir, boxetteGround, inexAir, inexSea)
            _feedbackMessage.value = "Carrier rates saved successfully."
        }
    }

    fun updateQuickAddAmounts(amounts: List<Int>) {
        viewModelScope.launch {
            preferencesRepository.setQuickAddAmounts(amounts)
            _feedbackMessage.value = "Quick Add presets updated."
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDarkMode(enabled) }
    }

    fun toggleDynamicColors(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setUseDynamicColors(enabled) }
    }

    fun toggleMilestoneNotifications(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setEnableMilestoneNotifications(enabled) }
    }

    fun toggleMonthlyReminderNotifications(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setEnableMonthlyReminderNotifications(enabled) }
    }

    fun setDefaultMonthlySavings(amount: BigDecimal) {
        viewModelScope.launch { preferencesRepository.setDefaultMonthlySavings(amount) }
    }

    fun syncExchangeRatesNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            when (val res = currencyRepository.syncRates(forceRefresh = true)) {
                is CurrencySyncResult.Success -> {
                    _feedbackMessage.value = "Exchange rates updated successfully."
                }
                is CurrencySyncResult.Failure -> {
                    _feedbackMessage.value = res.message
                }
            }
            _isSyncing.value = false
        }
    }

    suspend fun getExportJson(): String {
        return backupManager.generateBackupJson()
    }

    fun inspectImportJson(rawJson: String) {
        val result = backupManager.validateBackupJson(rawJson)
        _backupPreview.value = result
        if (!result.isValid) {
            _feedbackMessage.value = result.errorMessage ?: "Failed to read backup file."
        }
    }

    fun dismissBackupPreview() {
        _backupPreview.value = null
    }

    fun executeRestore(backup: FundoryBackup, isReplaceMode: Boolean) {
        viewModelScope.launch {
            try {
                backupManager.restoreData(backup, isReplaceMode)
                _backupPreview.value = null
                _feedbackMessage.value = "Successfully restored ${backup.goals.size} goals and ${backup.transactions.size} transactions."
            } catch (e: Exception) {
                _feedbackMessage.value = "Restore failed: ${e.localizedMessage}"
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            backupManager.clearAllData()
            _feedbackMessage.value = "All Fundory savings data has been cleared."
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            val rates = currencyRepository.ratesToGelFlow.first()
            val (sampleGoals, sampleTransactions) = SampleDataProvider.generateSampleGoals(rates)

            val goalEntities = sampleGoals.map { GoalEntity.fromDomain(it) }
            val productEntities = sampleGoals.flatMap { it.products }.map { ProductEntity.fromDomain(it) }
            val transactionEntities = sampleTransactions.map { TransactionEntity.fromDomain(it) }

            goalDao.insertGoals(goalEntities)
            productDao.insertProducts(productEntities)
            transactionDao.insertTransactions(transactionEntities)

            _feedbackMessage.value = "Sample data loaded: PC Upgrade, Camera Kit, Guitar, and Kazbegi Trip."
        }
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }
}

