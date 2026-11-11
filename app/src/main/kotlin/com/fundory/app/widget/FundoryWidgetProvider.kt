package com.fundory.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.fundory.app.MainActivity
import com.fundory.app.R
import com.fundory.app.data.local.FundoryDatabase
import com.fundory.app.domain.model.Currencies
import com.fundory.app.domain.model.GoalStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@AndroidEntryPoint
class FundoryWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var database: FundoryDatabase

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetsInternal(context, appWidgetManager, appWidgetIds, database)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, FundoryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, FundoryWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }

        suspend fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            db: FundoryDatabase
        ) {
            val activeGoals = db.goalDao().getAllGoals().filter { it.status == GoalStatus.ACTIVE }
            val firstGoal = activeGoals.firstOrNull()

            val goalName: String
            val savedGel: BigDecimal
            val targetGel: BigDecimal
            val progressPercent: Int

            if (firstGoal != null) {
                goalName = firstGoal.name
                savedGel = firstGoal.savedAmountGel
                val parcels = db.parcelDao().getParcelsByGoal(firstGoal.id)
                val totalAdditional = parcels.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.toDomain().totalCostGel) }
                targetGel = firstGoal.targetAmountGel.add(totalAdditional)
                progressPercent = if (targetGel > BigDecimal.ZERO) {
                    savedGel.multiply(BigDecimal(100)).divide(targetGel, 0, RoundingMode.HALF_UP).toInt().coerceIn(0, 100)
                } else 0
            } else {
                goalName = "General Savings"
                val generalTx = db.transactionDao().getGeneralSavingsTransactionsAscending()
                val sum = generalTx.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.toDomain().signedAmountGel) }
                savedGel = (if (sum < BigDecimal.ZERO) BigDecimal.ZERO else sum).setScale(2, RoundingMode.HALF_UP)
                targetGel = BigDecimal.ZERO
                progressPercent = 100
            }

            val savedText = Currencies.format(savedGel, "GEL")
            val targetText = if (targetGel > BigDecimal.ZERO) Currencies.format(targetGel, "GEL") else "—"
            val amountsText = if (targetGel > BigDecimal.ZERO) "$savedText / $targetText" else savedText

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_fundory_goal)
                views.setTextViewText(R.id.widget_goal_name, goalName)
                views.setTextViewText(R.id.widget_amounts, amountsText)
                views.setTextViewText(R.id.widget_percentage, "$progressPercent%")
                views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    widgetId,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}

