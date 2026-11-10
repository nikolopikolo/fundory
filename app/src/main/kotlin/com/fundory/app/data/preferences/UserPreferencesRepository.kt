package com.fundory.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fundory_user_preferences")

data class UserPreferences(
    val isDarkMode: Boolean = true,
    val useDynamicColors: Boolean = false,
    val defaultCurrency: String = "GEL",
    val selectedActiveGoalId: String? = null,
    val defaultMonthlySavings: BigDecimal = BigDecimal("100.00"),
    val lastExchangeRatesSyncMillis: Long = 0L,
    val enableMilestoneNotifications: Boolean = true,
    val enableMonthlyReminderNotifications: Boolean = true,
    val hasCompletedOnboarding: Boolean = false
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val SELECTED_ACTIVE_GOAL_ID = stringPreferencesKey("selected_active_goal_id")
        val DEFAULT_MONTHLY_SAVINGS = stringPreferencesKey("default_monthly_savings")
        val LAST_SYNC_MILLIS = longPreferencesKey("last_sync_millis")
        val ENABLE_MILESTONE_NOTIFICATIONS = booleanPreferencesKey("enable_milestone_notifications")
        val ENABLE_MONTHLY_REMINDER_NOTIFICATIONS = booleanPreferencesKey("enable_monthly_reminder_notifications")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
            useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: false,
            defaultCurrency = prefs[Keys.DEFAULT_CURRENCY] ?: "GEL",
            selectedActiveGoalId = prefs[Keys.SELECTED_ACTIVE_GOAL_ID],
            defaultMonthlySavings = BigDecimal(prefs[Keys.DEFAULT_MONTHLY_SAVINGS] ?: "100.00"),
            lastExchangeRatesSyncMillis = prefs[Keys.LAST_SYNC_MILLIS] ?: 0L,
            enableMilestoneNotifications = prefs[Keys.ENABLE_MILESTONE_NOTIFICATIONS] ?: true,
            enableMonthlyReminderNotifications = prefs[Keys.ENABLE_MONTHLY_REMINDER_NOTIFICATIONS] ?: true,
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
    }

    suspend fun setUseDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_DYNAMIC_COLORS] = enabled }
    }

    suspend fun setDefaultCurrency(currencyCode: String) {
        context.dataStore.edit { it[Keys.DEFAULT_CURRENCY] = currencyCode }
    }

    suspend fun setSelectedActiveGoalId(goalId: String?) {
        context.dataStore.edit {
            if (goalId != null) {
                it[Keys.SELECTED_ACTIVE_GOAL_ID] = goalId
            } else {
                it.remove(Keys.SELECTED_ACTIVE_GOAL_ID)
            }
        }
    }

    suspend fun setDefaultMonthlySavings(amount: BigDecimal) {
        context.dataStore.edit { it[Keys.DEFAULT_MONTHLY_SAVINGS] = amount.toPlainString() }
    }

    suspend fun setLastExchangeRatesSyncMillis(millis: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_MILLIS] = millis }
    }

    suspend fun setEnableMilestoneNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_MILESTONE_NOTIFICATIONS] = enabled }
    }

    suspend fun setEnableMonthlyReminderNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_MONTHLY_REMINDER_NOTIFICATIONS] = enabled }
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = completed }
    }
}
