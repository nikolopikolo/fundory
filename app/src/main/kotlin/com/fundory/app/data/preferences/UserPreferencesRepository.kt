package com.fundory.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
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
    val hasCompletedOnboarding: Boolean = false,
    val boxetteAirRateUsd: Double = 12.90,
    val boxetteGroundRateUsd: Double = 5.45,
    val inexAirRateUsd: Double = 10.50,
    val inexSeaRateUsd: Double = 4.50,
    val quickAddAmounts: List<Int> = listOf(10, 20, 50, 100),
    val widgetGoalId: String? = null
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
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
        val BOXETTE_AIR_RATE = doublePreferencesKey("boxette_air_rate")
        val BOXETTE_GROUND_RATE = doublePreferencesKey("boxette_ground_rate")
        val INEX_AIR_RATE = doublePreferencesKey("inex_air_rate")
        val INEX_SEA_RATE = doublePreferencesKey("inex_sea_rate")
        val QUICK_ADD_AMOUNTS = stringPreferencesKey("quick_add_amounts")
        val WIDGET_GOAL_ID = stringPreferencesKey("widget_goal_id")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val quickAmountsStr = prefs[Keys.QUICK_ADD_AMOUNTS] ?: "10,20,50,100"
        val parsedQuickAmounts = quickAmountsStr.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .ifEmpty { listOf(10, 20, 50, 100) }

        UserPreferences(
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
            useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: false,
            defaultCurrency = prefs[Keys.DEFAULT_CURRENCY] ?: "GEL",
            selectedActiveGoalId = prefs[Keys.SELECTED_ACTIVE_GOAL_ID],
            defaultMonthlySavings = BigDecimal(prefs[Keys.DEFAULT_MONTHLY_SAVINGS] ?: "100.00"),
            lastExchangeRatesSyncMillis = prefs[Keys.LAST_SYNC_MILLIS] ?: 0L,
            enableMilestoneNotifications = prefs[Keys.ENABLE_MILESTONE_NOTIFICATIONS] ?: true,
            enableMonthlyReminderNotifications = prefs[Keys.ENABLE_MONTHLY_REMINDER_NOTIFICATIONS] ?: true,
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            boxetteAirRateUsd = prefs[Keys.BOXETTE_AIR_RATE] ?: 12.90,
            boxetteGroundRateUsd = prefs[Keys.BOXETTE_GROUND_RATE] ?: 5.45,
            inexAirRateUsd = prefs[Keys.INEX_AIR_RATE] ?: 10.50,
            inexSeaRateUsd = prefs[Keys.INEX_SEA_RATE] ?: 4.50,
            quickAddAmounts = parsedQuickAmounts,
            widgetGoalId = prefs[Keys.WIDGET_GOAL_ID]
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

    suspend fun setCarrierRates(boxetteAir: Double, boxetteGround: Double, inexAir: Double, inexSea: Double) {
        context.dataStore.edit {
            it[Keys.BOXETTE_AIR_RATE] = boxetteAir
            it[Keys.BOXETTE_GROUND_RATE] = boxetteGround
            it[Keys.INEX_AIR_RATE] = inexAir
            it[Keys.INEX_SEA_RATE] = inexSea
        }
    }

    suspend fun setQuickAddAmounts(amounts: List<Int>) {
        context.dataStore.edit {
            it[Keys.QUICK_ADD_AMOUNTS] = amounts.joinToString(",")
        }
    }

    suspend fun setWidgetGoalId(goalId: String?) {
        context.dataStore.edit {
            if (goalId != null) {
                it[Keys.WIDGET_GOAL_ID] = goalId
            } else {
                it.remove(Keys.WIDGET_GOAL_ID)
            }
        }
    }
}
