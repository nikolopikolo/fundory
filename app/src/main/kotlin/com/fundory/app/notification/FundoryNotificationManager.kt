package com.fundory.app.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.fundory.app.FundoryApplication
import com.fundory.app.MainActivity
import com.fundory.app.R
import com.fundory.app.domain.model.Goal
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FundoryNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun showMilestoneNotification(goal: Goal, milestonePercentage: Int) {
        if (!hasNotificationPermission()) return

        val title = if (milestonePercentage >= 100) {
            "🎉 Goal Reached: ${goal.name}!"
        } else {
            "🚀 Milestone: $milestonePercentage% Reached!"
        }

        val message = if (milestonePercentage >= 100) {
            "Congratulations! You've saved ${goal.savedAmountGel} GEL and fully funded ${goal.name}."
        } else {
            "You've saved ${goal.savedAmountGel} GEL of your ${goal.targetAmountGel} GEL target for ${goal.name}."
        }

        sendNotification(
            notificationId = goal.id.hashCode() + milestonePercentage,
            title = title,
            message = message
        )
    }

    fun showMonthlySavingsReminder(plannedAmountGel: String) {
        if (!hasNotificationPermission()) return

        sendNotification(
            notificationId = 1001,
            title = "💰 Time for your monthly savings!",
            message = "Remember to set aside your planned $plannedAmountGel GEL for this month's goals."
        )
    }

    private fun sendNotification(notificationId: Int, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FundoryApplication.CHANNEL_SAVINGS_MILESTONES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

